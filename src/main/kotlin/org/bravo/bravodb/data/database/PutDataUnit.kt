package org.bravo.bravodb.data.database

import org.bravo.bravodb.data.common.JsonConverter

// Request
data class PutDataUnit(
    val key: String,
    val value: String
) : JsonConverter()

// Response is nothing
