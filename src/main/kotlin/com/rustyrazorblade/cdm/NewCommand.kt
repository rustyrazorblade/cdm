package com.rustyrazorblade.cdm

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters

import java.util.ArrayList

/**
 * Created by jhaddad on 10/7/16.
 */
@Parameters(commandDescription = "New Dataset")
class NewCommand {
    @Parameter
    var dataset: List<String> = ArrayList()
}
