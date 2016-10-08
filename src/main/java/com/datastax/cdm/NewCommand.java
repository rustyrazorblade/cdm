package com.datastax.cdm;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jhaddad on 10/7/16.
 */
@Parameters(commandDescription = "New Dataset")
public class NewCommand {
    @Parameter
    public List<String> dataset = new ArrayList<>();
}
