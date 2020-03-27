package org.bravo.bravodb.data.transport

data class Answer(
    val statusCode: AnswerStatus,
    val message: String? = null
)
