package org.bravo.bravodb.server

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.bravo.bravodb.data.storage.InstanceStorage
import org.bravo.bravodb.server.server.Server
import org.bravo.bravodb.server.server.config.ServerDiscoveryConfig

class Discovery(
    private val serverDiscoveryConfig: ServerDiscoveryConfig
) {
    private val server = Server(serverDiscoveryConfig)

    private suspend fun schedulePrintStorageState() {
        GlobalScope.launch {
            while (true) {
                delay(10 * 1000) // 10 sec
                var acc = ""
                val list = InstanceStorage.findAll()
                list.forEach {
                    acc = "$acc (${it.host}:${it.port})"
                }
                logger.warn("Storage state: $acc")
            }
        }
    }

    fun start(configOtherServerDiscovery: ServerDiscoveryConfig) {
        runBlocking {
            schedulePrintStorageState()
            runCatching {
                InstanceStorage.setSelfInstanceInfo(serverDiscoveryConfig.host, serverDiscoveryConfig.port)
                logger.info("Discovery start")

                if (serverDiscoveryConfig::class.java != configOtherServerDiscovery::class.java) {
                    logger.error(
                        "Type of server config and other known server config not equal:" +
                            " ${serverDiscoveryConfig::class.java} != ${configOtherServerDiscovery::class.java}"
                    )
                    return@runBlocking
                }

                bootstrapServer()

                runCatching {
                    if (configOtherServerDiscovery.host != serverDiscoveryConfig.host) {
                        saveAndFirstRegistration(configOtherServerDiscovery)
                    } else if (configOtherServerDiscovery.port != serverDiscoveryConfig.port) {
                        saveAndFirstRegistration(configOtherServerDiscovery)
                    } else {
                        logger.warn(
                            "Doesn't save other instance ${configOtherServerDiscovery.host}:${configOtherServerDiscovery.port}"
                        )
                    }
                }.getOrElse {
                    logger.error("Cannot first registration because  ${it.message}")
                }

                scheduleReregistration()
            }.getOrElse {
                logger.error("Restart discovery server because ${it.message}")
                start(configOtherServerDiscovery)
            }
        }
    }

    fun start() {
        runCatching {
            runBlocking {
                logger.info("Discovery start")
                schedulePrintStorageState()
                InstanceStorage.setSelfInstanceInfo(serverDiscoveryConfig.host, serverDiscoveryConfig.port)
                bootstrapServer()
                scheduleReregistration()
            }
        }.getOrElse {
            logger.error("Restart discovery server because ${it.message}")
            start()
        }
    }

    private suspend fun saveAndFirstRegistration(configOtherServerDiscovery: ServerDiscoveryConfig) {
        InstanceStorage.save(
            configOtherServerDiscovery.host,
            configOtherServerDiscovery.port
        )
        firstRegistration(configOtherServerDiscovery)
    }

    private suspend fun scheduleReregistration() {
        while (true) {
            delay(15 * 1000) // 15 seconds
            logger.info("Start re-registration")
            for (instance in InstanceStorage.findAll()) {
                try {
                    if (instance.client.registration(serverDiscoveryConfig.host, serverDiscoveryConfig.port)) {
                        logger.info("Reregistration in $instance is successfully")
                    } else {
                        logger.error("Reregistration in $instance is bad")
                        if (InstanceStorage.delete(instance)) {
                            logger.warn("Delete ${instance.host}:${instance.port} is successfully")
                        } else {
                            logger.warn("Delete ${instance.host}:${instance.port} is bad")
                        }
                    }
                } catch(e: Throwable) {
                    logger.error("Error during reregistration in ${instance.host}:${instance.port}")
                }
            }
            logger.info("Finish re-registration")
        }
    }

    /**
     * Start server for to receive registration and to send known hosts
     */
    private suspend fun bootstrapServer() {
        logger.info("Start bootstrap server")
        server.start()
        logger.info("Finish bootstrap server")
    }

    /**
     * Do self registration on other same servers
     */
    private suspend fun firstRegistration(otherServerDiscoveryConfig: ServerDiscoveryConfig) {
        logger.info("First registration start")

        // registration and get info about other instance on first known instance
        val isRegistration = InstanceStorage.findByHostAndPort(
            otherServerDiscoveryConfig.host,
            otherServerDiscoveryConfig.port
        )?.client?.registration(serverDiscoveryConfig.host, serverDiscoveryConfig.port)
            ?: logger.info("Cannot find $otherServerDiscoveryConfig").let {
                return
            }

        // registration in got instances
        if (isRegistration) {
            logger.info("First registration finish successfully")
            logger.info("Start registration in received instances")
            InstanceStorage.findAll().asFlow()
                .filter { instance ->
                    instance.host != otherServerDiscoveryConfig.host && instance.port != otherServerDiscoveryConfig.port
                }
                .collect { instance ->
                    if (!instance.client.registration(serverDiscoveryConfig.host, serverDiscoveryConfig.port)) {
                        logger.error("Can not registration in instance ${instance.host}:${instance.port}")
                    }
                }
            logger.info("Finish registration in received instances")
        } else {
            logger.error("Can not registration in instance ${otherServerDiscoveryConfig.host}:${otherServerDiscoveryConfig.port}")
        }
    }

    companion object {
        private val logger = LogManager.getLogger(this::class.java.declaringClass)
    }
}
