package org.bravo.bravodb.client.config

import org.bravo.bravodb.client.transport.Client

/**
 * Contain client config data and building
 */
class ClientConfig(
    val host: String,
    val port: Int,
    val transport: Client
)
