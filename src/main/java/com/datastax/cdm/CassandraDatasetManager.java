package com.datastax.cdm;

import com.beust.jcommander.JCommander;
import com.datastax.driver.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import java.lang.StringBuilder;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by jhaddad on 6/29/16.
 */


public class CassandraDatasetManager {

    public class InvalidArgsException extends Exception {

    }


    private static final String YAML_URI = "https://raw.githubusercontent.com/riptano/cdm-java/master/datasets.yaml";
    private Map<String, Dataset> datasets;
    private Session session;
    private String cassandraContactPoint;
    private String host;
    private BaseCommand args;
    public String relative_schema_path = "schema.cql";


    CassandraDatasetManager() {
        String[] args = {};
        BaseCommand parsedArgs = new BaseCommand();
        new JCommander(parsedArgs, args);

        this.host = "localhost";
        this.args = parsedArgs;
    }

    CassandraDatasetManager(BaseCommand args, Map<String, Dataset> datasets) {
        this.datasets = datasets;
//        this.host = args.host;
        this.args = args;
    }


    public static void main(String[] args) throws IOException, InterruptedException, GitAPIException {

        System.out.println("Starting CDM");
        BaseCommand argParser = new BaseCommand();

        JCommander jc = new JCommander(argParser);

        InstallCommand installCommand = new InstallCommand();
        jc.addCommand("install", installCommand);

        NewCommand newCommand = new NewCommand();
        jc.addCommand("new", newCommand);

        ListCommand listCommand = new ListCommand();
        jc.addCommand("list", listCommand);
        jc.parse(args);

        // check for the .cdm directory
        String home_dir = System.getProperty("user.home");
        String cdm_path = home_dir + "/.cdm";

        File f = new File(cdm_path);

        f.mkdir();

        // check for the YAML file
        File yaml = new File(cdm_path + "/datasets.yaml");
        if (!yaml.exists()) {
            URL y = new URL(YAML_URI);
            FileUtils.copyURLToFile(y, yaml);
        }
        // read in the YAML dataset list
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        // why extra work? Java Type Erasure will prevent type detection otherwise
        Map<String, Dataset> data = mapper.readValue(yaml, new TypeReference<Map<String, Dataset>>() {} );

        CassandraDatasetManager cdm = new CassandraDatasetManager(argParser, data);

        String command = jc.getParsedCommand();

        // connect to the cluster via the driver
        switch (command) {
            case "install":
                cdm.install(installCommand);
                break;
            case "list":
                cdm.list();
                break;
            case "new":
                cdm.new_dataset(newCommand);
                break;
            case "dump":
                cdm.dump();
                break;
            case "update":
                cdm.update();
                break;
            default:
                jc.usage();
                cdm.printHelp();

        }
        System.out.println("Finished.");
    }

    private void dump() throws IOException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Config config = mapper.readValue(new File("cdm.yaml"), Config.class);

        for(String table: config.tables) {
            StringBuilder command = new StringBuilder();
            command.append("cqlsh -k ")
                   .append(config.keyspace)
                   .append(" -e \"")
                   .append("COPY ")
                   .append(table)
                   .append(" TO 'data/")
                   .append(table)
                   .append(".csv'\"");
            System.out.println(command);
            Runtime.getRuntime().exec(new String[]{"bash", "-c", command.toString()}).waitFor();
        }

    }

    private void new_dataset(NewCommand newCommand) throws FileNotFoundException, UnsupportedEncodingException {
        String name = newCommand.dataset.get(0);
        System.out.println("Creating new dataset " + name);
        File f = new File(name);
        f.mkdir();
        String conf = name + "/" + "cdm.yaml";
        PrintWriter config = new PrintWriter(conf, "UTF-8");
        String sample_conf = "keyspace: " + name + "\n" +
                "tables:\n" +
                "    - tablename\n" +
                "version: 2.1";
        config.println(sample_conf);
        config.close();
        File data_dir = new File(name + "/data");
        data_dir.mkdir();
    }

    void install(InstallCommand command) throws IOException, InterruptedException, GitAPIException {
        // for now i'm using local
        System.out.println("Installing " + command.datasets);


        String cdmDir = System.getProperty("user.home") + "/.cdm/";

        String schema;
        String dataPath;
        String configLocation;
        String path;
        String name = command.datasets.get(0);
        // temp variables that need to change depending on the dataset

        // we're dealing with a request to install a local dataset
        if(name.equals(".")) {
            path = System.getProperty("user.dir");
            // do nothing here
        } else {
            Dataset dataset = datasets.get(name);
            // pull the repo down
            String repoLocation = cdmDir + name;
            path = repoLocation;

            System.out.println("Checking for repo at " + repoLocation);
            // if the repo doesn't exist, clone it
            File f = new File(repoLocation);

            if(!f.exists()) {
                System.out.println("Cloning " + dataset.url);
                f.mkdir();

                try {
                    Git result = Git.cloneRepository()
                            .setURI(dataset.url)
                            .setDirectory(f)
                            .call();
                    System.out.println("Having repository: " + result.getRepository().getDirectory());
                    // Note: the call() returns an opened repository
                    // already which needs to be closed to avoid file handle leaks!
                }
                 catch (InvalidRemoteException e) {
                    e.printStackTrace();
                } catch (TransportException e) {
                    e.printStackTrace();
                } catch (GitAPIException e) {
                    e.printStackTrace();
                }

            } else {
                // pull the latest
                System.out.println("Pulling latest");
                Git repo = Git.open(f);
                repo.pull().call();
            }
        }
        System.out.println("CDM is using dataset path: " + path);

        // all the paths
        dataPath = path + "/data/";
        configLocation = path + "/cdm.yaml";

        File configFile =  new File(configLocation);

        // load the yaml

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        Config config = mapper.readValue(configFile, Config.class);

        if(config.schema != null) {
            schema = path + "/" + config.schema;

        }
        else {
            schema = path + "/schema.cql";
        }
        System.out.println("Loading schema from " + schema);
        String address = command.host;

        Cluster cluster = Cluster.builder().addContactPoint(address).build();
        Session session = cluster.connect();

        Integer rf = command.rf;
        StringBuilder createKeyspace = new StringBuilder();
        createKeyspace.append(" CREATE KEYSPACE ")
                .append(config.keyspace)
                .append(" WITH replication = {'class': 'SimpleStrategy', 'replication_factor': ")
                .append(rf)
                .append("}");

        System.out.println("Dropping keyspace");
        session.execute("DROP KEYSPACE IF EXISTS " + config.keyspace);
        Thread.sleep(2000);

        System.out.println(createKeyspace);
        session.execute(createKeyspace.toString());

        session.execute("USE " + config.keyspace);
        
        System.out.println("Schema: " + schema);
//        String loadSchema = "cqlsh -k " + config.keyspace + " -f " + schema;

        byte[] bytes = Files.readAllBytes(Paths.get(schema));
        String[] create_tables = new String(bytes).split(";");
        for(String c: create_tables) {
            System.out.println("Letting schema settle...");
            Thread.sleep(2000);
            String tmp = c.trim();
            if(tmp.length() > 0) {
                System.out.println(tmp);
                session.execute(tmp);
            }
        }

        // skip the data load
        if(command.noData) {
            System.out.println("Not loading up data, skipping");
            cluster.close();
            return;
        }
        System.out.println("Loading data");

        this.session = session;

        for(String table: config.tables) {
            String dataFile = dataPath + table + ".csv";
            Iterable<CSVRecord> records = openCSV(dataFile);

            System.out.println("Importing " + table);
            KeyspaceMetadata keyspaceMetadata = cluster.getMetadata()
                                                        .getKeyspace(config.keyspace);
            TableMetadata tableMetadata = keyspaceMetadata.getTable(table);

            List<ColumnMetadata> columns = tableMetadata.getColumns();

            StringJoiner fields = new StringJoiner(", ");
            StringJoiner values = new StringJoiner(", ");

            HashMap types = new HashMap();

            ArrayList<Field> fieldlist = new ArrayList<>();

            for(ColumnMetadata c: columns) {
                fields.add(c.getName());
                String ftype = c.getType().getName().toString();
                types.put(c.getName(), ftype);
                fieldlist.add(new Field(c.getName(), ftype));
            }

            int totalComplete = 0;
            List<ResultSetFuture> futures = new ArrayList<>();
            for(CSVRecord record: records) {
                // generate a CQL statement
                String cql = null;
                try {
                    cql = generateCQL(table, record, fieldlist);

                    ResultSetFuture future = session.executeAsync(cql);
                    futures.add(future);
                    totalComplete++;
                    if(totalComplete % 100 == 0) {
                        futures.forEach(ResultSetFuture::getUninterruptibly);
                        futures.clear();
                    }
                    System.out.print("Complete: " + totalComplete + "\r");

                } catch (InvalidArgsException e) {
                    e.printStackTrace();
                    System.out.println(record);
                }

            }
            futures.forEach(ResultSetFuture::getUninterruptibly);
            futures.clear();
            System.out.println("Done importing " + table);
        }

        cluster.close();
        System.out.println("Loading data");
    }

    CSVParser openCSV(String path) throws IOException {
        File f = new File(path);
        return CSVParser.parse(f, Charset.forName("UTF-8"), CSVFormat.RFC4180.withEscape('\\'));
    }

    String generateCQL(String table,
                       CSVRecord record,
                       ArrayList<Field> fields) throws InvalidArgsException {

        HashSet needs_quotes = new HashSet();

        needs_quotes.add("text");
        needs_quotes.add("datetime");
        needs_quotes.add("timestamp");


        StringBuilder query = new StringBuilder("INSERT INTO ");
        query.append(table);
        query.append("(");

        StringJoiner sjfields = new StringJoiner(", ");
        StringJoiner values = new StringJoiner(", ");

        fields.forEach(f -> sjfields.add(f.name));
        query.append(sjfields.toString());

        query.append(") VALUES (");
        if (record.size() != fields.size())
            throw new InvalidArgsException();

        for(int i = 0; i < record.size(); i++) {
            String v = record.get(i);
            Field f = fields.get(i);
            if(needs_quotes.contains(f.type)) {
                v = "'" + v.replace("'", "''") + "'";
            }
            if(v.trim().equals("")) {
                v = "null";
            }
            values.add(v);
        }

        query.append(values.toString());

        query.append(")");

        return query.toString();
    }

    void update() throws IOException {
        System.out.println("Updating datasets...");
        String home_dir = System.getProperty("user.home");
        String cdm_path = home_dir + "/.cdm";

        File yaml = new File(cdm_path + "/datasets.yaml");
        URL y = new URL(YAML_URI);
        FileUtils.copyURLToFile(y, yaml);
    }

    void list() {
        System.out.println("Datasets: ");

        for(Map.Entry<String, Dataset> dataset : datasets.entrySet()) {
            System.out.println(dataset.getKey());
        }
    }
    void printHelp() {
        System.out.println("Put help here.");
    }
}
