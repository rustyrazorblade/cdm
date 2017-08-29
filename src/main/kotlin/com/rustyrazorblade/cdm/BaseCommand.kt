package com.rustyrazorblade.cdm

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters

import java.util.ArrayList

/**
 * Created by jhaddad on 9/14/16.
 */
@Parameters(commandDescription = "command")
class BaseCommand {

    @Parameter(names = arrayOf("--help"), help = true)
    private val help: Boolean = false


}
