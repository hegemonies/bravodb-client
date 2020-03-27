package org.bravo.bravodb.server.server.transport.rsocket

import io.rsocket.AbstractRSocket
import io.rsocket.Payload
import io.rsocket.util.DefaultPayload
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.bravo.bravodb.data.common.fromJson
import org.bravo.bravodb.data.database.GetDataUnitRequest
import org.bravo.bravodb.data.database.GetDataUnitResponse
import org.bravo.bravodb.data.database.PutDataUnit
import org.bravo.bravodb.data.registration.RegistrationRequest
import org.bravo.bravodb.data.registration.RegistrationResponse
import org.bravo.bravodb.data.storage.DataStorage
import org.bravo.bravodb.data.storage.InstanceStorage
import org.bravo.bravodb.data.storage.model.DataUnit
import org.bravo.bravodb.data.transport.Answer
import org.bravo.bravodb.data.transport.AnswerStatus
import org.bravo.bravodb.data.transport.DataType
import org.bravo.bravodb.data.transport.Request
import org.bravo.bravodb.data.transport.Response
import reactor.core.publisher.Mono

class RSocketReceiveHandler : AbstractRSocket() {

    /**
     * Registration handler: save instance info in storage and response
     * @param [payload] contain data about host instance (InstanceInfo}
     */
    override fun requestResponse(payload: Payload?): Mono<Payload> {
        return Mono.create { sink ->
            logger.info("Receive data: ${payload?.dataUtf8}")

            payload?.let {
                try {
                    val request = fromJson<Request>(it.dataUtf8)

                    when (request.type) {
                        DataType.REGISTRATION_REQUEST -> {
                            val requestBody = fromJson<RegistrationRequest>(request.body)
                            runBlocking {
                                GlobalScope.launch {
                                    InstanceStorage.save(
                                        requestBody.instanceInfo.host,
                                        requestBody.instanceInfo.port
                                    )
                                }.start()
                                InstanceStorage.findAll().map { instanceInfo ->
                                    instanceInfo.toView()
                                }.let { instancesInfoViewList ->
                                    logger.info("Send list: $instancesInfoViewList")
                                    Response(
                                        Answer(AnswerStatus.OK),
                                        DataType.REGISTRATION_RESPONSE,
                                        RegistrationResponse(instancesInfoViewList).toJson()
                                    ).toJson().let { json ->
                                        sink.success(DefaultPayload.create(json))
                                    }
                                }
                            }
                        }
                        DataType.PUT_DATA -> {
                            val requestBody = fromJson<PutDataUnit>(request.body)
                            runBlocking { DataStorage.save(requestBody.key, requestBody.value) }
                            GlobalScope.launch { replicationData(requestBody) }.start()
                            val response = Response(Answer(AnswerStatus.OK)).toJson()
                            sink.success(DefaultPayload.create(response)).also {
                                logger.info("Success answer $response")
                            }
                        }
                        DataType.GET_DATA -> {
                            val requestBody = fromJson<GetDataUnitRequest>(request.body)
                            val responseBody = GetDataUnitResponse(DataStorage.findByKey(requestBody.key)).toJson()
                            val response = Response(Answer(AnswerStatus.OK), DataType.GET_DATA, responseBody).toJson()
                            sink.success(DefaultPayload.create(response)).also {
                                logger.info("Success answer $response")
                            }
                        }
                        else -> {
                            sink.error(Exception("Data type is not correct"))
                        }
                    }
                } catch (e: Exception) {
                    sink.error(e)
                }
            } ?: "Receive empty payload".also {
                logger.error(it)
                sink.error(Exception(it))
            }
        }
    }

    private suspend fun replicationData(unitBody: PutDataUnit) {
        val data = DataUnit(unitBody.key, unitBody.value)
        InstanceStorage.findAll().asFlow().collect {
            it.client.putData(data)
        }
    }

    companion object {
        private val logger = LogManager.getLogger(this::class.java.declaringClass)
    }
}
