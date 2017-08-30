package com.rustyrazorblade.cdm

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import java.util.ArrayList

/**
 * Created by jhaddad on 10/7/16.
 */

@Parameters(commandDescription = "command")
class BaseCommand {

    @Parameter(names = arrayOf("--help"), help = true)
    private val help: Boolean = false


}

@Parameters(commandDescription = "Install")
class InstallCommand {

    @Parameter(names = arrayOf("--host", "-h"), description = "Hostname of node in cluster")
    var host = "localhost"

    @Parameter
    var datasets: List<String> = ArrayList()

    @Parameter(names = arrayOf("--rf"), description = "Replication Factor")
    var rf: Int? = 1

    @Parameter(names = arrayOf("--no-data", "--nodata"), description = "Only set up schema")
    var noData: Boolean? = false // setting this is schema only

    @Parameter(names = arrayOf("--noddl"), description = "Do not run DDL statements")
    var noDDL: Boolean = false // data only
}

@Parameters(commandDescription = "help")
class HelpCommand

@Parameters(commandDescription = "List Datasets")
class ListCommand

@Parameters(commandDescription = "Dump current dataset")
class DumpCommand

@Parameters(commandDescription = "New Dataset")
class NewCommand {
    @Parameter
    var dataset: List<String> = ArrayList()
}

@Parameters(commandDescription = "Update datasets")
class UpdateCommand

class CommandParser internal constructor(args: Array<String>) {
    internal var argParser: BaseCommand
    internal var installCommand: InstallCommand
    internal var newCommand: NewCommand
    internal var listCommand: ListCommand
    internal var updateCommand: UpdateCommand
    internal var dumpCommand: DumpCommand
    internal var helpCommand: HelpCommand
    internal var jc: JCommander

    init {
        argParser = BaseCommand()

        jc = JCommander(argParser)
        jc.setProgramName("Cassandra Dataset Manager")

        installCommand = InstallCommand()
        jc.addCommand("install", installCommand)

        newCommand = NewCommand()
        jc.addCommand("new", newCommand)

        listCommand = ListCommand()
        jc.addCommand("list", listCommand)

        updateCommand = UpdateCommand()
        jc.addCommand("update", updateCommand)

        dumpCommand = DumpCommand()
        jc.addCommand("dump", dumpCommand)

        helpCommand = HelpCommand()
        jc.addCommand("help", helpCommand)

        jc.parse(*args)
    }
}
