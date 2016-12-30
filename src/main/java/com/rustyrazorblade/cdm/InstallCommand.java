package com.rustyrazorblade.cdm;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.sun.org.apache.xpath.internal.operations.Bool;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jhaddad on 10/7/16.
 */
@Parameters(commandDescription = "Install")
public class InstallCommand {

    @Parameter(names = {"--host", "-h"}, description = "Hostname of node in cluster")
    public String host = "localhost";

    @Parameter
    public List<String> datasets = new ArrayList<>();

    @Parameter(names = "--rf", description = "Replication Factor")
    public Integer rf = 1;

    @Parameter(names = {"--no-data", "--nodata"}, description = "Only set up schema")
    public Boolean noData = false; // setting this is schema only

    @Parameter(names = {"--noddl"}, description = "Do not run DDL statements")
    public Boolean noDDL = false; // data only


}