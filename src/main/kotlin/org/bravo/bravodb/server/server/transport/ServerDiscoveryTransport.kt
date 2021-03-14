package org.bravo.bravodb.server.server.transport

interface ServerDiscoveryTransport {
    suspend fun start(port: Int)
}
