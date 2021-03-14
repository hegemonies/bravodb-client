package org.bravo.bravodb

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.apache.logging.log4j.LogManager
import org.bravo.bravodb.commandlineclient.CommandLineRunner
import org.bravo.bravodb.server.Discovery
import org.bravo.bravodb.server.properties.BravoDBProperties
import org.bravo.bravodb.server.server.config.ServerDiscoveryConfig
import org.bravo.bravodb.server.server.transport.rsocket.RSocketServerDiscovery
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

private val logger = LogManager.getLogger()

suspend fun main() {
    val isClientMode = runCatching {
        System.getenv("BRAVODB_CLIENT_MODE").toLowerCase().toBoolean()
    }.getOrElse { error ->
        logger.info("BRAVODB_CLIENT_MODE not set, start as only server.")
        false
    }

    if (isClientMode) {
        runClientMode()
    } else {
        runServerMode()
    }
}

fun runClientMode() {
    GlobalScope.launch {
        runServerMode()
    }
    CommandLineRunner.run()
}

suspend fun runServerMode() {
    runCatching {
        initializeDiscovery()
    }.getOrElse { error ->
        logger.fatal("Can not initialize discovery: ${error.message}")
    }
}

suspend fun initializeDiscovery() {
    measureTimeMillis {
        runCatching {
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

            val configOtherServer =
                if (discoveryProperties.otherServerHost != null && discoveryProperties.otherServerPort != null) {
                    ServerDiscoveryConfig.Builder()
                        .setPort(discoveryProperties.otherServerPort)
                        .setHost(discoveryProperties.otherServerHost)
                        .setTransport(RSocketServerDiscovery.javaClass)
                        .build()
                } else {
                    null
                }

            Discovery(selfServerConfig).start(configOtherServer)
        }.getOrElse { error ->
            logger.error("Can not starting server: ${error.message}")
        }
    }.also { elapsedTime ->
        logger.info("Discovery start by $elapsedTime millis")
    }
}
