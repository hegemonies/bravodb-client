package org.bravo.bravodb.data.storage

import org.apache.logging.log4j.LogManager
import org.bravo.bravodb.data.storage.model.DataUnit
import java.util.concurrent.ConcurrentHashMap

/**
 * Contain create and delete function on [DataUnit] storage
 */
object DataStorage {
    private val pool = ConcurrentHashMap<String, String>()
    private val logger = LogManager.getLogger(this::class.java.declaringClass)

    fun save(key: String, value: String) = save(DataUnit(key, value))

    fun save(unit: DataUnit) {
        if (pool[unit.key] == null) {
            logger.info("Save $unit to database")
        } else {
            logger.info("Update $unit in database")
        }
        pool[unit.key] = unit.value
    }

    fun findAll() = pool

    fun delete(unit: DataUnit): Boolean = pool.remove(unit.key, unit.value)

    fun findByKey(key: String) =
        pool[key]?.let { value ->
            DataUnit(key, value)
        }

    fun deleteAll() =
        pool.forEach {
            delete(DataUnit(it.key, it.value))
        }
}
