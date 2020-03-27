package org.bravo.bravodb.client.transport.rsocket

import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import io.rsocket.frame.decoder.PayloadDecoder
import io.rsocket.transport.netty.client.TcpClientTransport
import io.rsocket.util.DefaultPayload
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.bravo.bravodb.client.transport.Client
import org.bravo.bravodb.data.common.fromJson
import org.bravo.bravodb.data.database.GetDataUnitRequest
import org.bravo.bravodb.data.database.PutDataUnit
import org.bravo.bravodb.data.registration.RegistrationRequest
import org.bravo.bravodb.data.registration.RegistrationResponse
import org.bravo.bravodb.data.storage.InstanceStorage
import org.bravo.bravodb.data.storage.model.DataUnit
import org.bravo.bravodb.data.storage.model.InstanceInfoView
import org.bravo.bravodb.data.transport.AnswerStatus
import org.bravo.bravodb.data.transport.DataType
import org.bravo.bravodb.data.transport.Request
import org.bravo.bravodb.data.transport.Response
import java.time.Duration

class RSocketClient(
    override val host: String,
    override val port: Int
) : Client {

    private var client: RSocket? = null

    init {
        logger.info("Start connect to $host:$port")
        runBlocking {
            if (!connect()) {
                logger.error("Error init RSocketClient during connection to $host:$port")
                // InstanceStorage.findByHostAndPort(host, port)?.also {
                //     if (InstanceStorage.delete(it)) {
                //         logger.info("Deleted instance $host:$port")
                //     } else {
                //         logger.info("Cannot delete instance $host:$port")
                //     }
                // }
            }
        }
        logger.info("Finish connect to $host:$port")
    }

    override suspend fun connect(): Boolean {
        runCatching {
            client?.isDisposed?.let {
                if (!it) {
                    client?.dispose()
                }
            }
            client = RSocketFactory.connect()
                .keepAlive(Duration.ofSeconds(5), Duration.ofSeconds(10), 5)
                .frameDecoder(PayloadDecoder.ZERO_COPY)
                .transport(TcpClientTransport.create(host, port))
                .start()
                .awaitFirstOrNull()
        }.getOrElse {
            logger.error("Cannot connect to $host:$port: ${it.message}")
            return false
        }
        return client != null
    }

    override suspend fun registration(selfHost: String, selfPort: Int): Boolean {
        client ?: also {
            logger.error("Client of $host:$port is null")
            if (!connect()) {
                logger.error("Bad reconnection")
                return false
            } else {
                logger.info("Successfully reconnection")
            }
        }

        logger.info("Start registration in $host:$port")

        val requestBody = RegistrationRequest(
            InstanceInfoView(selfHost, selfPort)
        ).toJson()
        val request = Request(DataType.REGISTRATION_REQUEST, requestBody).toJson()

        runCatching {
            client?.requestResponse(DefaultPayload.create(request))
                ?.awaitFirstOrNull()
                ?.also { payload ->
                    logger.info("Received response: ${payload.dataUtf8}")
                    val response = fromJson<Response>(payload.dataUtf8)

                    if (response.answer.statusCode == AnswerStatus.OK) {
                        response.body ?: let {
                            logger.error("Response body is null")
                            return false
                        }

                        fromJson<RegistrationResponse>(response.body).also { resp ->
                            if (resp.otherInstances.count() > 0) {
                                resp.otherInstances.forEach { instanceInfo ->
                                    GlobalScope.launch {
                                        if (!InstanceStorage.save(instanceInfo.host, instanceInfo.port)) {
                                            logger.info(
                                                "Cannot adding instance info ${instanceInfo.host}:${instanceInfo.port}" +
                                                    " in storage because it already exists"
                                            )
                                        }
                                    }.start()
                                }
                            } else {
                                logger.info("Received 0 other instances")
                            }
                        }
                    } else {
                        logger.error("Receive error status on self registration")
                        return false
                    }

                    logger.info("Response on self registration: ${payload.dataUtf8}")
                }
                ?: also {
                    logger.error("Error registration")
                    return false
                }
        }.getOrElse {
            logger.error("Cannot do request-response: ${it.message}")
            InstanceStorage.delete(InstanceInfoView(host, port))
        }
        return true
    }

    override suspend fun putData(unit: DataUnit): Boolean {
        val requestBody = PutDataUnit(unit.key, unit.value).toJson()
        val request = Request(DataType.PUT_DATA, requestBody).toJson()

        val payload = client?.requestResponse(DefaultPayload.create(request))
            ?.awaitFirstOrNull()
            ?.dataUtf8
            ?: run {
                logger.error("Cannot send data to $host:$port: client not connection")
                return false
            }

        val response = fromJson<Response>(payload)

        if (response.answer.statusCode != AnswerStatus.OK) {
            logger.error("Received not success response: ${response.answer.message}")
            return false
        }
        if (response.type != DataType.PUT_DATA) {
            logger.error("Data type in response is not DataType.SEND_DATA_RESPONSE")
            return false
        }

        return true
    }

    override suspend fun getData(key: String): DataUnit? {
        val requestBody = GetDataUnitRequest(key).toJson()
        val request = Request(DataType.GET_DATA, requestBody).toJson()

        val payload = client?.requestResponse(DefaultPayload.create(request))
            ?.awaitFirstOrNull()
            ?: run {
                logger.error("Cannot get data from $host:$port")
                return null
            }

        val response = fromJson<Response>(payload.dataUtf8)

        if (response.answer.statusCode != AnswerStatus.OK) {
            logger.error(response.answer.message)
            return null
        }
        if (response.type != DataType.GET_DATA) {
            logger.error("Not correct data type received")
            return null
        }

        return response.body?.let {
            fromJson<DataUnit>(it)
        }
    }

    companion object {
        private val logger = LogManager.getLogger(this::class.java.declaringClass)
    }
}
