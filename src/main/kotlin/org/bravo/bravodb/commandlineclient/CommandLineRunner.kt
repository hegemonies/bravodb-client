package org.bravo.bravodb.commandlineclient

import kotlinx.coroutines.runBlocking
import org.bravo.bravodb.client.facade.ClientFacade
import org.bravo.bravodb.data.storage.InstanceStorage
import org.bravo.bravodb.data.storage.model.DataUnit
import java.lang.Exception

object CommandLineRunner {

    private val help = """
        How to use command line runner:
        [put | p] <key> <value>    Put data with <key> <value> in database
        [get | g] <key>            Get and show data from database by key
        [quit | q]                 Quit from program
        [help | h]                 Print help
        [list | l]                 Print list of known instances
    """.trimIndent()

    fun run() {
        @Suppress("ControlFlowWithEmptyBody")
        runBlocking {
            while (next()) {
            }
        }
    }

    private fun printHelp() = println(help)

    private fun printInstancesList() {
        InstanceStorage.findAll().forEach { instance ->
            print(" [${instance.host}:${instance.port}]")
        }
        println()
    }

    private suspend fun next(): Boolean {
        print(":) ")
        return try {
            readLine()?.let {
                val strings = it.split(" ")
                if (strings.isEmpty()) {
                    printHelp()
                    return@let true
                }
                val command = strings[0]
                return when (command) {
                    "get", "g" -> {
                        runCatching {
                            val key = strings[1]
                            ClientFacade.getData(key)?.also { data ->
                                println(":> ${data.key} ${data.value}")
                            } ?: println(":> records with key $key not found")
                        }.getOrElse {
                            println(":> records not found")
                        }
                        true
                    }
                    "put", "p" -> {
                        runCatching {
                            if (strings.count() < 3) {
                                printHelp()
                            } else {
                                val key = strings[1]
                                val value = strings[2]
                                if (ClientFacade.putData(DataUnit(key, value))) {
                                    println("Put [$key; $value] is successfully")
                                } else {
                                    println("Put [$key; $value] is bad")
                                }
                            }
                        }.getOrElse {
                            println("Put data is bad")
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
                    "list", "l" -> {
                        printInstancesList()
                        true
                    }
                    else -> {
                        // printHelp()
                        true
                    }
                }
            } ?: true
        } catch (e: Exception) {
            true
        } catch (t: Throwable) {
            true
        }
    }
}
