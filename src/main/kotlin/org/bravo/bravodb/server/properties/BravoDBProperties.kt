package org.bravo.bravodb.server.properties

import org.apache.logging.log4j.LogManager
import org.bravo.bravodb.server.consts.DefaultDiscoveryConnectInfo
import java.util.*
import kotlin.system.exitProcess

/**
 * Contain discovery service properties
 * [selfClientHost]: "bravodb.discovery.client.self.host"
 * [selfClientPort]: "bravodb.discovery.client.self.port"
 * [selfServerHost]: "bravodb.discovery.server.self.host"
 * [selfServerPort]: "bravodb.discovery.server.self.port"
 * [otherServerHost]: "bravodb.discovery.server.other.host"
 * [otherServerPort]: "bravodb.discovery.server.other.port"
 */
data class BravoDBProperties private constructor(
    val selfServerHost: String,
    val selfServerPort: Int,

    val otherServerHost: String?,
    val otherServerPort: Int?
) {

    companion object {
        /**
         * Build [BravoDBProperties]
         * Reading properties file from classpath.
         * If no find properties, use default values from [DefaultDiscoveryConnectInfo]
         * @return [BravoDBProperties] after reading properties file.
         * Can return null.
         */
        fun fromResourceFile(resourceFilename: String): BravoDBProperties? {
            this::class.java.classLoader.getResourceAsStream(resourceFilename).use {
                val properties = Properties()
                try {
                    properties.load(it)
                } catch (e: Exception) {
                    logger.error("Can not read properties file $resourceFilename: ${e.message}")
                    return null
                }

                val selfServerHost = properties.getProperty(
                    "bravodb.server.self.host",
                    DefaultDiscoveryConnectInfo.HOST
                )

                val selfServerPort = properties.getProperty(
                    "bravodb.server.self.port",
                    DefaultDiscoveryConnectInfo.PORT.toString()
                ).toInt()

                val otherServerHost = properties.getProperty(
                    "bravodb.server.other.host",
                    DefaultDiscoveryConnectInfo.OTHER_SERVER_HOST
                )

                val otherServerPort = properties.getProperty(
                    "bravodb.server.other.port",
                    DefaultDiscoveryConnectInfo.OTHER_SERVER_PORT.toString()
                ).toInt()

                return BravoDBProperties(
                    selfServerHost, selfServerPort,
                    otherServerHost, otherServerPort
                )
            }
        }

        fun fromEnvironments(): BravoDBProperties {
            val selfHost = System.getenv("BRAVODB_SERVER_SELF_HOST")
                ?: errorLog("Self server host must be set")
            val selfPort = System.getenv("BRAVODB_SERVER_SELF_PORT")?.toInt()
                ?: errorLog("Self server port must be set")

            val otherHost = System.getenv("BRAVODB_SERVER_OTHER_HOST")
            otherHost ?: logger.warn("Host other server does not set")
            val otherPort = System.getenv("BRAVODB_SERVER_OTHER_PORT")?.toInt()
            otherPort ?: logger.warn("Port other server does not set")

            return BravoDBProperties(selfHost, selfPort, otherHost, otherPort)
        }

        private fun errorLog(message: String): Nothing {
            logger.error(message)
            exitProcess(1)
        }

        private val logger = LogManager.getLogger(this::class.java.declaringClass)
    }
}
