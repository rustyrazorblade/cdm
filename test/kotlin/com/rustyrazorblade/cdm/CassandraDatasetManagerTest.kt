package com.rustyrazorblade.cdm

import org.junit.Assert.assertEquals
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.assertThat

import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.junit.Test

import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

/**
 * Created by jhaddad on 9/5/16.
 */
class CassandraDatasetManagerTest {

    @Test
    @Throws(IOException::class, CassandraDatasetManager.InvalidArgsException::class)
    fun testCQLStatementGeneration() {
        val c = CassandraDatasetManager()
        val records = c.openCSV("data/alltypes.csv")
        val r = records.iterator().next()

        val types = HashMap()
        val fieldList = ArrayList<Field>()
        fieldList.add(Field("id", "uuid"))
        fieldList.add(Field("avg", "float"))
        fieldList.add(Field("cash", "decimal"))
        fieldList.add(Field("intmap", "map"))
        fieldList.add(Field("num", "int"))
        fieldList.add(Field("ts", "timeuuid"))

        val query = c.generateCQL("whatever",
                r,
                fieldList
        )
        assertThat(query, containsString("INSERT INTO whatever(id, avg, cash, intmap, num, ts) VALUES"))

    }

    @Test
    @Throws(IOException::class)
    fun testCSVWeirdQuotes() {
        val c = CassandraDatasetManager()
        val records = c.openCSV("data/users2.csv")
        for (record in records) {

        }

    }

}
