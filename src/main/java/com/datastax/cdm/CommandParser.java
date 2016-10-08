package com.datastax.cdm;

import com.beust.jcommander.JCommander;

/**
 * Created by jhaddad on 10/7/16.
 */
public class CommandParser {
    BaseCommand argParser;
    InstallCommand installCommand;
    NewCommand newCommand;
    ListCommand listCommand;
    JCommander jc;

    CommandParser(String[] args) {
        argParser = new BaseCommand();

        jc = new JCommander(argParser);

        installCommand = new InstallCommand();
        jc.addCommand("install", installCommand);

        newCommand = new NewCommand();
        jc.addCommand("new", newCommand);

        listCommand = new ListCommand();
        jc.addCommand("list", listCommand);
        jc.parse(args);
    }
}
