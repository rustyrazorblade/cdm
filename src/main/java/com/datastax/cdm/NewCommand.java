package com.datastax.cdm;

import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jhaddad on 10/7/16.
 */
public class NewCommand {
    @Parameter
    public List<String> dataset = new ArrayList<>();
}
