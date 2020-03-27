package org.bravo.bravodb.data.registration

import org.bravo.bravodb.data.common.JsonConverter
import org.bravo.bravodb.data.storage.model.InstanceInfoView

data class RegistrationRequest(
    val instanceInfo: InstanceInfoView
) : JsonConverter()
