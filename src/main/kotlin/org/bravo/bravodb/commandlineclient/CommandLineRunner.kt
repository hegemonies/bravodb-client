package org.bravo.bravodb.commandlineclient

import kotlinx.coroutines.runBlocking
import org.bravo.bravodb.client.facade.ClientFacade
import org.bravo.bravodb.data.storage.model.DataUnit

object CommandLineRunner {

    private val help = """
        How to use command line runner:
        [put | p] <key> <value>    Put data with <key> <value> in database
        [get | g] <key>            Get and show data from database by key
        [quit | q]                 Quit from program
        [help | h]                 Print help
    """.trimIndent()

    fun run() {
        @Suppress("ControlFlowWithEmptyBody")
        runBlocking { while (next()) {} }
    }

    private fun printHelp() = println(help)

    private suspend fun next(): Boolean {
        print(":) ")
        return readLine()?.let {
            val strings = it.split(" ")
            if (strings.isEmpty()) {
                printHelp()
               return@let true
            }
            val command = strings[0]
            return when (command) {
                "get", "g" -> {
                    val key = strings[1]
                    ClientFacade.getData(key)?.also { data ->
                        println(":> ${data.key} ${data.value}")
                    } ?: println(":> records with key $key not found")
                    true
                }
                "put", "p" -> {
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
                }
                "quit", "q" -> {
                    println("Bye")
                    false
                }
                "help", "h" -> {
                    printHelp()
                    true
                }
                else -> {
                    printHelp()
                    true
                }
            }
        } ?: true // .also { printHelp() }
    }
}
