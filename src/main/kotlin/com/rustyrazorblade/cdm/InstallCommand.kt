package com.rustyrazorblade.cdm

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.sun.org.apache.xpath.internal.operations.Bool

import java.util.ArrayList

/**
 * Created by jhaddad on 10/7/16.
 */
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
