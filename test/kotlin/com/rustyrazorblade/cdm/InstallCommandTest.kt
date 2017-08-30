package com.rustyrazorblade.cdm

import org.junit.Test

/**
 * Created by jhaddad on 10/7/16.
 */
class InstallCommandTest {
    @Test
    fun testLocalInstall() {
        val args = arrayOf("install", ".")
        val p = CommandParser(args)
    }
}
