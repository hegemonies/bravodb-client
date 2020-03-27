package org.bravo.bravodb.client.facade

import org.apache.logging.log4j.LogManager
import org.bravo.bravodb.data.storage.InstanceStorage
import org.bravo.bravodb.data.storage.model.DataUnit

object ClientFacade {

    private val logger = LogManager.getLogger(this::class.java.declaringClass)

    suspend fun putData(data: DataUnit): Boolean {
        InstanceStorage.findAll().forEach { instance ->
            if (instance.client.putData(data)) {
                logger.info("Put data $data in ${instance.host}:${instance.port}")
                return true
            }
        }
        return false
    }

    suspend fun getData(key: String): DataUnit? {
        InstanceStorage.findAll().forEach { instance ->
            instance.client.getData(key)?.let {
                return it
            }
        }
        return null
    }
}
