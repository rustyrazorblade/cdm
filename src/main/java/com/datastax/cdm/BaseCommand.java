package com.datastax.cdm;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jhaddad on 9/14/16.
 */
@Parameters(commandDescription = "command")
public class BaseCommand {

    @Parameter(names = "--help", help = true)
    private boolean help;


}
