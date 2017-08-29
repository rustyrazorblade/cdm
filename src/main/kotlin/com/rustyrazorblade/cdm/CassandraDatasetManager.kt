package com.rustyrazorblade.cdm

import com.beust.jcommander.JCommander
import com.datastax.driver.core.*
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.util.SystemReader

import java.lang.StringBuilder

import java.io.*
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * Created by jhaddad on 6/29/16.
 */


class CassandraDatasetManager {

    inner class InvalidArgsException : Exception()

    private val datasets: Map<String, Dataset>? = null
    private val session: Session? = null
    private val cassandraContactPoint: String = ""
    private var host: String? = null
    private var args: BaseCommand? = null
    var relative_schema_path = "schema.cql"


    internal constructor() {
        val args = arrayOf<String>()
        val parsedArgs = BaseCommand()
        JCommander(parsedArgs, *args)

        this.host = "localhost"
        this.args = parsedArgs
        this.datasets = Map<String, Dataset>;
    }

    internal constructor(args: BaseCommand, datasets: Map<String, Dataset>) {
        this.datasets = datasets
        //        this.host = args.host;
        this.args = args
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun dump() {
        val mapper = ObjectMapper(YAMLFactory())
        val config = mapper.readValue(File("cdm.yaml"), Config::class.java)

        for (table in config.tables!!) {
            val command = StringBuilder()
            command.append("cqlsh -k ")
                    .append(config.keyspace)
                    .append(" -e \"")
                    .append("COPY ")
                    .append(table)
                    .append(" TO 'data/")
                    .append(table)
                    .append(".csv'\"")
            println(command)
            Runtime.getRuntime().exec(arrayOf("bash", "-c", command.toString())).waitFor()
        }

    }

    @Throws(FileNotFoundException::class, UnsupportedEncodingException::class)
    private fun new_dataset(newCommand: NewCommand) {
        val name = newCommand.dataset[0]
        println("Creating new dataset " + name)
        val f = File(name)
        f.mkdir()
        val conf = name + "/" + "cdm.yaml"
        val config = PrintWriter(conf, "UTF-8")
        val sample_conf = "keyspace: " + name + "\n" +
                "tables:\n" +
                "    - tablename\n" +
                "version: 2.1"
        config.println(sample_conf)
        config.close()
        val data_dir = File(name + "/data")
        data_dir.mkdir()
    }

    @Throws(IOException::class, InterruptedException::class, GitAPIException::class)
    internal fun install(command: InstallCommand) {
        // for now i'm using local
        println("Installing " + command.datasets)

        val cdmDir = System.getProperty("user.home") + "/.cdm/"

        val schema: String
        val dataPath: String
        val configLocation: String
        val path: String
        val name = command.datasets[0]
        // temp variables that need to change depending on the dataset
        path = getPath(cdmDir, name)


        println("CDM is using dataset path: " + path)

        // all the paths
        dataPath = path + "/data/"
        configLocation = path + "/cdm.yaml"

        val configFile = File(configLocation)

        // load the yaml
        val mapper = ObjectMapper(YAMLFactory())

        val config = mapper.readValue(configFile, Config::class.java)


        val address = command.host
        val cluster = Cluster.builder().addContactPoint(address).build()
        val session = cluster.connect()
        println("Connecting to " + address)

        if (!command.noDDL) {
            if (config.schema != null) {
                schema = path + "/" + config.schema

            } else {
                schema = path + "/schema.cql"
            }
            println("Loading schema from " + schema)
            val rf = command.rf
            val createKeyspace = StringBuilder()
            createKeyspace.append(" CREATE KEYSPACE ")
                    .append(config.keyspace)
                    .append(" WITH replication = {'class': 'SimpleStrategy', 'replication_factor': ")
                    .append(rf)
                    .append("}")

            println("Dropping keyspace")
            session.execute("DROP KEYSPACE IF EXISTS " + config.keyspace!!)

            Thread.sleep(2000)

            println(createKeyspace)
            session.execute(createKeyspace.toString())

            session.execute("USE " + config.keyspace!!)

            println("Schema: " + schema)
            //        String loadSchema = "cqlsh -k " + config.keyspace + " -f " + schema;

            createTables(schema, session)
        } else {
            println("Skipping DDL statements")
            session.execute("USE " + config.keyspace!!)
        }

        // skip the data load
        try {
            if (command.noData!!) {
                println("Not loading up data, skipping")
            } else {
                println("Loading data")
                loadAllTAbles(dataPath, config, cluster, session)
            }
        } finally {
            cluster.close()
        }
    }

    @Throws(IOException::class)
    private fun loadAllTAbles(dataPath: String, config: Config, cluster: Cluster, session: Session) {
        for (table in config.tables!!) {
            val dataFile = dataPath + table + ".csv"
            val records = openCSV(dataFile)

            println("Importing " + table)
            val keyspaceMetadata = cluster.metadata
                    .getKeyspace(config.keyspace)
            val tableMetadata = keyspaceMetadata.getTable(table)

            val columns = tableMetadata.columns

            val fields = StringJoiner(", ")
            val values = StringJoiner(", ")

            val types = HashMap()

            val fieldlist = ArrayList<Field>()

            for (c in columns) {
                fields.add(c.name)
                val ftype = c.type.name.toString()
                types.put(c.name, ftype)
                fieldlist.add(Field(c.name, ftype))
            }

            val totalComplete = 0
            val futures = ArrayList<ResultSetFuture>()
            insertRecords(session, table, records, fieldlist, totalComplete, futures)
            futures.forEach(Consumer<ResultSetFuture> { it.getUninterruptibly() })
            futures.clear()
            println("Done importing " + table)
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun createTables(schema: String, session: Session) {
        val bytes = Files.readAllBytes(Paths.get(schema))
        val create_tables = String(bytes).split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (c in create_tables) {
            println("Letting schema settle...")
            Thread.sleep(2000)
            val tmp = c.trim { it <= ' ' }
            if (tmp.length > 0) {
                println(tmp)
                session.execute(tmp)
            }
        }
    }

    @Throws(IOException::class, GitAPIException::class)
    fun getPath(cdmDir: String, name: String): String {
        val path: String// we're dealing with a request to install a local dataset
        if (name == ".") {
            path = System.getProperty("user.dir")
            // do nothing here
        } else {
            val dataset = datasets[name]
            // pull the repo down
            val repoLocation = cdmDir + name
            path = repoLocation

            println("Checking for repo at " + repoLocation)
            // if the repo doesn't exist, clone it
            val f = File(repoLocation)

            if (!f.exists()) {
                println("Cloning " + dataset.url!!)
                f.mkdir()

                try {
                    val result = Git.cloneRepository()
                            .setURI(dataset.url)
                            .setDirectory(f)
                            .call()
                    println("Having repository: " + result.repository.directory)
                    // Note: the call() returns an opened repository
                    // already which needs to be closed to avoid file handle leaks!
                } catch (e: InvalidRemoteException) {
                    e.printStackTrace()
                } catch (e: TransportException) {
                    e.printStackTrace()
                } catch (e: GitAPIException) {
                    e.printStackTrace()
                }

            } else {
                // pull the latest
                println("Pulling latest")
                val repo = Git.open(f)
                repo.pull().call()
            }
        }
        return path
    }

    fun insertRecords(session: Session, table: String, records: Iterable<CSVRecord>, fieldlist: ArrayList<Field>, totalComplete: Int, futures: MutableList<ResultSetFuture>) {
        var totalComplete = totalComplete
        for (record in records) {
            // generate a CQL statement
            var cql: String? = null
            try {
                cql = generateCQL(table, record, fieldlist)

                val future = session.executeAsync(cql)
                futures.add(future)
                totalComplete++
                if (totalComplete % 100 == 0) {
                    futures.forEach(Consumer<ResultSetFuture> { it.getUninterruptibly() })
                    futures.clear()
                }
                print("Complete: " + totalComplete + "\r")

            } catch (e: InvalidArgsException) {
                e.printStackTrace()
                println(record)
            }

        }
    }

    @Throws(IOException::class)
    internal fun openCSV(path: String): CSVParser {
        val f = File(path)
        return CSVParser.parse(f, Charset.forName("UTF-8"), CSVFormat.RFC4180.withEscape('\\'))
    }

    @Throws(CassandraDatasetManager.InvalidArgsException::class)
    internal fun generateCQL(table: String,
                             record: CSVRecord,
                             fields: ArrayList<Field>): String {

        val needs_quotes = HashSet()

        needs_quotes.add("text")
        needs_quotes.add("datetime")
        needs_quotes.add("timestamp")


        val query = StringBuilder("INSERT INTO ")
        query.append(table)
        query.append("(")

        val sjfields = StringJoiner(", ")
        val values = StringJoiner(", ")

        fields.forEach { f -> sjfields.add(f.name) }
        query.append(sjfields.toString())

        query.append(") VALUES (")
        if (record.size() != fields.size)
            throw InvalidArgsException()

        for (i in 0..record.size() - 1) {
            var v = record.get(i)
            val f = fields[i]
            if (needs_quotes.contains(f.type)) {
                v = "'" + v.replace("'", "''") + "'"
            }
            if (v.trim { it <= ' ' } == "") {
                v = "null"
            }
            values.add(v)
        }

        query.append(values.toString())

        query.append(")")

        return query.toString()
    }

    @Throws(IOException::class)
    internal fun update() {
        println("Updating datasets...")
        val home_dir = System.getProperty("user.home")
        val cdm_path = home_dir + "/.cdm"

        val yaml = File(cdm_path + "/datasets.yaml")
        val y = URL(YAML_URI)
        FileUtils.copyURLToFile(y, yaml)
    }

    internal fun list() {
        println("Datasets: ")
        var maxLen = 0
        for (s in datasets.keys) {
            if (s.length > maxLen) {
                maxLen = s.length
            }
        }

        val format = "%-" + Integer.toString(maxLen + 2) + "s %s\n"

        for ((key, value) in datasets) {
            System.out.printf(format, key, value.description)
        }
    }

    companion object {


        private val YAML_URI = "https://raw.githubusercontent.com/riptano/cdm-java/master/datasets.yaml"


        @Throws(IOException::class, InterruptedException::class, GitAPIException::class)
        @JvmStatic
        fun main(args: Array<String>) {

            println("Starting CDM")

            val parser = CommandParser(args)

            // check for the .cdm directory
            val home_dir = System.getProperty("user.home")
            val cdm_path = home_dir + "/.cdm"

            val f = File(cdm_path)

            f.mkdir()

            // check for the YAML file
            val yaml = File(cdm_path + "/datasets.yaml")
            if (!yaml.exists()) {
                val y = URL(YAML_URI)
                FileUtils.copyURLToFile(y, yaml)
            }
            // read in the YAML dataset list
            val mapper = ObjectMapper(YAMLFactory())

            // why extra work? Java Type Erasure will prevent type detection otherwise
            val data = mapper.readValue<Map<String, Dataset>>(yaml, object : TypeReference<Map<String, Dataset>>() {

            })

            val cdm = CassandraDatasetManager(parser.argParser, data)

            // check for the case where no arguments are passed in resulting in a null return
            val parse_command = parser.jc.parsedCommand
            val command = parse_command ?: "help"

            // connect to the cluster via the driver
            when (command) {
                "install" -> cdm.install(parser.installCommand)
                "list" -> cdm.list()
                "new" -> cdm.new_dataset(parser.newCommand)
                "dump" -> cdm.dump()
                "update" -> cdm.update()
                else -> parser.jc.usage()
            }
            println("Finished.")
        }
    }
}
