package com.rustyrazorblade.cdm;

import com.beust.jcommander.JCommander;

/**
 * Created by jhaddad on 10/7/16.
 */
public class CommandParser {
    BaseCommand argParser;
    InstallCommand installCommand;
    NewCommand newCommand;
    ListCommand listCommand;
    UpdateCommand updateCommand;
    DumpCommand dumpCommand;
    HelpCommand helpCommand;
    JCommander jc;

    CommandParser(String[] args) {
        argParser = new BaseCommand();

        jc = new JCommander(argParser);
        jc.setProgramName("Cassandra Dataset Manager");

        installCommand = new InstallCommand();
        jc.addCommand("install", installCommand);

        newCommand = new NewCommand();
        jc.addCommand("new", newCommand);

        listCommand = new ListCommand();
        jc.addCommand("list", listCommand);

        updateCommand = new UpdateCommand();
        jc.addCommand("update", updateCommand);

        dumpCommand = new DumpCommand();
        jc.addCommand("dump", dumpCommand);

        helpCommand = new HelpCommand();
        jc.addCommand("help", helpCommand);

        jc.parse(args);
    }
}
