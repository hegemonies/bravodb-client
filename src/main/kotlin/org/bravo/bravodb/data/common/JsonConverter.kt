package org.bravo.bravodb.data.common

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

abstract class JsonConverter {
    companion object {
        val jsonMapper = jacksonObjectMapper()
    }
    fun toJson(): String = jsonMapper.writeValueAsString(this)
}

inline fun<reified T> fromJson(json: String): T = JsonConverter.jsonMapper.readValue(json, T::class.java)
