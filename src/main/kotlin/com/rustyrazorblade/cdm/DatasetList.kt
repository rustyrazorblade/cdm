package com.rustyrazorblade.cdm

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory

import java.io.File
import java.io.IOException

/**
 * Created by jhaddad on 7/8/16.
 */
class DatasetList @Throws(IOException::class)
internal constructor(inputfile: File) {
    private val data: Map<String, Dataset>

    init {
        val mapper = ObjectMapper(YAMLFactory())

        data = mapper.readValue<Map<String, Dataset>>(inputfile, Map<*, *>::class.java)

    }
}
