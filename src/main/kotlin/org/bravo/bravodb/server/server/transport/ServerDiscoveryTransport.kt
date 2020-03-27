package org.bravo.bravodb.server.server.transport

interface ServerDiscoveryTransport {
    var port: Int
    var host: String
    suspend fun start()
}
