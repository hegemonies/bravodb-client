package org.bravo.bravodb.server.server.transport.rsocket

import io.rsocket.ConnectionSetupPayload
import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import io.rsocket.frame.decoder.PayloadDecoder
import io.rsocket.transport.netty.server.TcpServerTransport
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.apache.logging.log4j.LogManager
import org.bravo.bravodb.server.consts.DefaultDiscoveryConnectInfo
import org.bravo.bravodb.server.server.transport.ServerDiscoveryTransport
import reactor.core.publisher.Mono

class RSocketServerDiscovery : ServerDiscoveryTransport {

    override var port: Int = DefaultDiscoveryConnectInfo.PORT
    override var host: String = DefaultDiscoveryConnectInfo.HOST

    override suspend fun start() {
        runCatching {
            RSocketFactory.receive()
                .frameDecoder(PayloadDecoder.ZERO_COPY)
                .acceptor(this::receiveHandler)
                .transport(TcpServerTransport.create(port))
                .start()
                .awaitFirstOrNull()
                // ?.onClose()
                // ?.awaitFirstOrNull()
                ?: logger.error("Error starting RSocket discovery server")
        }.getOrElse {
            logger.error("Error during run RSocket server because ${it.message}")
        }
    }

    private fun receiveHandler(setup: ConnectionSetupPayload, sendingSocket: RSocket): Mono<RSocket> {
        return Mono.just(RSocketReceiveHandler())
    }

    companion object {
        private val logger = LogManager.getLogger(this::class.java.declaringClass)
    }
}
