package org.bravo.bravodb.data.database

import org.bravo.bravodb.data.common.JsonConverter
import org.bravo.bravodb.data.storage.model.DataUnit

data class GetDataUnitRequest(
    val key: String
) : JsonConverter()

data class GetDataUnitResponse(
    val data: DataUnit?
) : JsonConverter()
