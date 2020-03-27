package org.bravo.bravodb.data.transport

import org.bravo.bravodb.data.common.JsonConverter

data class Request(
    val type: DataType,
    val body: String
) : JsonConverter()
