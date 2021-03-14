package org.bravo.bravodb.server

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.bravo.bravodb.data.storage.InstanceStorage
import org.bravo.bravodb.data.storage.model.InstanceInfo
import org.bravo.bravodb.server.server.Server
import org.bravo.bravodb.server.server.config.ServerDiscoveryConfig

class Discovery(
    private val serverDiscoveryConfig: ServerDiscoveryConfig
) {
    private val server = Server(serverDiscoveryConfig)

    private suspend fun schedulePrintStorageState() {
        GlobalScope.launch {
            while (true) {
                delay(10_000) // 10 sec
                var acc = ""
                val list = InstanceStorage.findAll()
                list.forEach {
                    acc = "$acc (${it.host}:${it.port})"
                }
                logger.warn("Storage state: $acc")
            }
        }
    }

    suspend fun start(configOtherServerDiscovery: ServerDiscoveryConfig?) {
        runCatching {
            logger.info("Discovery starts")

            schedulePrintStorageState()

            InstanceStorage.setSelfInstanceInfo(serverDiscoveryConfig.host, serverDiscoveryConfig.port)

            bootstrapServer(serverDiscoveryConfig.port)

            firstRegistrationIfNeed(configOtherServerDiscovery)

            scheduleReregistration()
        }.getOrElse {
            logger.error("Restart discovery server because ${it.message}")
            start(configOtherServerDiscovery)
        }
    }

    suspend fun firstRegistrationIfNeed(configOtherServerDiscovery: ServerDiscoveryConfig?) {
        if (configOtherServerDiscovery != null) {
            if (serverDiscoveryConfig::class.java != configOtherServerDiscovery::class.java) {
                logger.error(
                    "Type of server config and other known server config not equal:" +
                        " ${serverDiscoveryConfig::class.java} != ${configOtherServerDiscovery::class.java}"
                )
                return
            }

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
            }.getOrElse { error ->
                logger.error("Cannot first registration because ${error.message}")
            }
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
            delay(15_000) // 15 seconds

            logger.info("Start re-registration")

            val asyncJobs = mutableListOf<Job>()

            for (instance in InstanceStorage.findAll()) {
                val job = GlobalScope.launch {
                    var counter = 0

                    while (counter < 10) {
                        if (registration(instance)) {
                            break
                        }

                        counter++
                    }

                    if (counter == 10) {
                        logger.info("Deleting instance ${instance.host}:${instance.port}")

                        if (InstanceStorage.delete(instance)) {
                            logger.info("Delete ${instance.host}:${instance.port} is successfully")
                        } else {
                            logger.info("Delete ${instance.host}:${instance.port} is bad")
                        }
                    }

                    delay(2000)
                }

                asyncJobs.add(job)
            }

            asyncJobs.joinAll()

            logger.info("Finish re-registration")
        }
    }

    private suspend fun registration(instance: InstanceInfo): Boolean {
        runCatching {

            if (instance.client.registration(serverDiscoveryConfig.host, serverDiscoveryConfig.port)) {
                logger.info("Reregistration in $instance is successfully")

                return true
            } else {
                logger.error("Reregistration in $instance is bad")

                return false
            }
        }.getOrElse { error ->
            logger.error(
                "Error during reregistration in ${instance.host}:${instance.port} : ${error.message}"
            )

            return false
        }
    }

    /**
     * Start server for to receive registration and to send known hosts
     */
    private suspend fun bootstrapServer(port: Int) {
        logger.info("Starting bootstrap server on $port port")
        server.start(port)
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
