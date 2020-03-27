package org.bravo.bravodb.data.storage

import org.apache.logging.log4j.LogManager
import org.bravo.bravodb.data.storage.model.InstanceInfo
import org.bravo.bravodb.data.storage.model.InstanceInfoView
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

private val logger = LogManager.getLogger()

/**
 * Storage all information about database instances in network
 */
object InstanceStorage {

    // private val instances = ConcurrentLinkedQueue<InstanceInfo>()
    private val instances = CopyOnWriteArrayList<InstanceInfo>()

    private var selfInstanceInfo: InstanceInfoView? = null

    fun setSelfInstanceInfo(host: String, port: Int) {
        if (selfInstanceInfo == null) {
            selfInstanceInfo = InstanceInfoView(host, port)
        }
    }

    private fun isSelfInstance(instance: InstanceInfo) =
        if (selfInstanceInfo != null) {
            selfInstanceInfo!!.host == instance.host && selfInstanceInfo!!.port == instance.port
        } else {
            false
        }

    private fun isSelfInstance(host: String, port: Int) =
        if (selfInstanceInfo != null) {
            selfInstanceInfo!!.host == host && selfInstanceInfo!!.port == port
        } else {
            false
        }

    suspend fun save(instance: InstanceInfo) =
        if (!instanceExists(instance) && !isSelfInstance(instance)) {
            instances.add(instance)
        } else {
            true
        }

    suspend fun save(host: String, port: Int) =
        if (!instanceExists(host, port) && !isSelfInstance(host, port)) {
            save(InstanceInfo(host, port)).also {
                logger.info("Save instance: $host:$port")
            }
        } else {
            false
        }

    private fun instanceExists(host: String, port: Int): Boolean {
        instances.find {
            it.host == host && it.port == port
        }.let {
            return it != null
        }
    }

    private fun instanceExists(instance: InstanceInfo): Boolean {
        instances.find {
            it.host == instance.host && it.port == instance.port
        }.let {
            return when (it) {
                null -> false
                else -> true
            }
        }
    }

    fun findAll() = instances

    fun findByHost(host: String) =
        instances.filter {
            it.host == host
        }

    fun findByPort(port: Int) =
        instances.filter {
            it.port == port
        }

    fun findByHostAndPort(host: String, port: Int) =
        instances.find {
            it.host == host && it.port == port
        }

    fun delete(instance: InstanceInfo) = instances.remove(instance)

    fun delete(instance: InstanceInfoView) =
        instances.find {
            instance.host == it.host && instance.port == it.port
        }?.let {
            delete(it)
        } ?: false
}
