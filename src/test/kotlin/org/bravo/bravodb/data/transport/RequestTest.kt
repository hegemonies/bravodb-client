package org.bravo.bravodb.data.transport

import org.bravo.bravodb.data.common.fromJson
import org.bravo.bravodb.data.registration.RegistrationRequest
import org.bravo.bravodb.data.storage.model.InstanceInfoView
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class RequestTest {

    @Test
    fun `simple test`() {
        val data = RegistrationRequest(
            InstanceInfoView(
                "localhost",
                7777
            )
        ).toJson()

        val requestJson = Request(
            DataType.REGISTRATION_REQUEST,
            data
        ).also {
            println(it)
        }.toJson().also {
            println(it)
        }

        val request = fromJson<Request>(requestJson).also {
            println(it)
        }

        when(request.type) {
            DataType.REGISTRATION_REQUEST -> println(fromJson<RegistrationRequest>(request.body))
            else -> Assertions.fail()
        }
    }
}
