package org.bravo.bravodb.commandlineclient

import kotlinx.coroutines.runBlocking
import org.bravo.bravodb.client.facade.ClientFacade
import org.bravo.bravodb.data.storage.model.DataUnit

object CommandLineRunner {

    fun run() {
        @Suppress("ControlFlowWithEmptyBody")
        runBlocking { while (next()) {} }
    }

    private fun printHelp() = """
        How to use command line runner:
        :) put <key> <value>
        :) get <key>
    """.trimIndent()

    private suspend fun next(): Boolean {
        print(":) ")
        return readLine()?.let {
            val strings = it.split(" ")
            if (strings.isEmpty()) {
                printHelp()
               return@let false
            }
            val command = strings[0]
            return if (command == "get") {
                println("> ${ClientFacade.getData(strings[1])}")
                true
            } else if (command == "put") {
                if (strings.count() < 3) {
                    printHelp()
                    return@let false
                }
                val key = strings[1]
                val value = strings[2]
                if (ClientFacade.putData(DataUnit(key, value))) {
                    println("Put $key $value is successfully")
                } else {
                    println("Put $key $value is bad")
                }
                true
            } else {
                printHelp()
                false
            }
        } ?: false.also { printHelp() }
    }
}
