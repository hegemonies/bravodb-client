package org.bravo.bravodb

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.bravo.bravodb.server.Discovery
import org.bravo.bravodb.server.properties.BravoDBProperties
import org.bravo.bravodb.server.server.config.ServerDiscoveryConfig
import org.bravo.bravodb.server.server.transport.rsocket.RSocketServerDiscovery
import kotlin.system.measureTimeMillis

private val logger = LogManager.getLogger()

fun main() {
    try {
        initializeDiscovery()
    } catch (e: Exception) {
        logger.error("Error from main: ${e.message}")
    } catch (thr: Throwable) {
        logger.error("Error from main: ${thr.message}")
    }
}

fun initializeDiscovery() {
    measureTimeMillis {
        // val discoveryProperties = DiscoveryProperties.fromResourceFile("application.properties")
        //     ?: run {
        //         println("Can not read properties")
        //         return
        //     }
        val discoveryProperties = BravoDBProperties.fromEnvironments()

        logger.info("Setup discovery properties: $discoveryProperties")

        val selfServerConfig = ServerDiscoveryConfig.Builder()
            .setPort(discoveryProperties.selfServerPort)
            .setHost(discoveryProperties.selfServerHost)
            .setTransport(RSocketServerDiscovery.javaClass)
            .build()

        val configOtherServer = if (discoveryProperties.otherServerHost != null
            && discoveryProperties.otherServerPort != null
        ) {
            ServerDiscoveryConfig.Builder()
                .setPort(discoveryProperties.otherServerPort)
                .setHost(discoveryProperties.otherServerHost)
                .setTransport(RSocketServerDiscovery.javaClass)
                .build()
        } else {
            null
        }

        configOtherServer?.let {
            Discovery(selfServerConfig).start(it)
        } ?: Discovery(selfServerConfig).start()
    }.also {
        logger.info("Discovery start by $it millis")
    }
}
