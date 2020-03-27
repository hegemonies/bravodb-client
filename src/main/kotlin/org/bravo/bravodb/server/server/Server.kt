package org.bravo.bravodb.server.server

import org.apache.logging.log4j.LogManager
import org.bravo.bravodb.server.server.config.ServerDiscoveryConfig

/**
 * Discovery server
 */
class Server(
    private val discoveryConfig: ServerDiscoveryConfig
) {

    private val transport = discoveryConfig.discoveryTransport

    /**
     * Async start server
     */
    suspend fun start() {
        logger.info("Bootstrap discovery server start on port ${discoveryConfig.port}")
        // GlobalScope.launch {
        bootstrap()
        // }.start()
    }

    private suspend fun bootstrap() {
        transport.start()
    }

    companion object {
        private val logger = LogManager.getLogger(this::class.java.declaringClass)
    }
}
