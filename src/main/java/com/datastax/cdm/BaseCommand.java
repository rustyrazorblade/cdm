package com.datastax.cdm;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jhaddad on 9/14/16.
 */
public class BaseCommand {

    @Parameter(names = "--help", help = true)
    private boolean help;


}
