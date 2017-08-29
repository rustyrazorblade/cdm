package com.rustyrazorblade.cdm

import com.beust.jcommander.JCommander

/**
 * Created by jhaddad on 10/7/16.
 */
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
