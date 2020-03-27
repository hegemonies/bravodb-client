package org.bravo.bravodb.data.storage

import kotlinx.coroutines.runBlocking
import org.bravo.bravodb.data.storage.model.DataUnit
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DataStorageTest {

    @BeforeEach
    fun cleanStorage() {
        runBlocking {
            DataStorage.deleteAll()
        }
    }

    @Test
    fun `pass when test is correct`() = runBlocking {
        val data1 = DataUnit("1", "1")
        val data2 = DataUnit("2", "2")
        val data3 = DataUnit("1", "3")

        DataStorage.save(data1)
        DataStorage.save(data2)
        DataStorage.save(data3)

        assertEquals(
            hashMapOf(Pair(data2.key, data2.value), Pair(data3.key, data3.value)),
            DataStorage.findAll()
        )
        assertEquals(data3, DataStorage.findByKey(data3.key))
    }
}
