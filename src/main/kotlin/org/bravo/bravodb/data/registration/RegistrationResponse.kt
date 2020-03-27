package org.bravo.bravodb.data.registration

import org.bravo.bravodb.data.common.JsonConverter
import org.bravo.bravodb.data.storage.model.InstanceInfo
import org.bravo.bravodb.data.storage.model.InstanceInfoView
import java.util.*

data class RegistrationResponse(
    val otherInstances: List<InstanceInfoView>
) : JsonConverter()
