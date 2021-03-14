package org.bravo.bravodb.server.server

import org.bravo.bravodb.server.server.config.ServerDiscoveryConfig

/**
 * Discovery server
 */
class Server(
    private val discoveryConfig: ServerDiscoveryConfig
) {

    /**
     * Async start server
     */
    suspend fun start(port: Int) {
        discoveryConfig.discoveryTransport.start(port)
    }
}
