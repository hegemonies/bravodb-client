package org.bravo.bravodb.data.storage.model

import com.fasterxml.jackson.annotation.JsonIgnore
import org.bravo.bravodb.client.transport.rsocket.RSocketClient

data class InstanceInfo(
    val host: String,
    val port: Int
) {
    @JsonIgnore
    val client: RSocketClient = RSocketClient(host, port)

    fun toView() = InstanceInfoView(host, port)
}

data class InstanceInfoView(
    val host: String,
    val port: Int
)
