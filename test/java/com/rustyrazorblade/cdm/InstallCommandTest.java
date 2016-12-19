package com.rustyrazorblade.cdm;

import org.junit.Test;

/**
 * Created by jhaddad on 10/7/16.
 */
public class InstallCommandTest {
    @Test
    public void testLocalInstall() {
        String[] args = new String[]{"install", "."};
        CommandParser p = new CommandParser(args);
    }
}
