package com.russhwolf.escalators

sealed interface EscalatorResponse {
    data class Success(val escalators: List<Escalator>) : EscalatorResponse
    data object Failure : EscalatorResponse
}

data class Escalator(
    val id: String,
    val description: String,
    val direction: Direction,
    val isWorking: Boolean,
    val status: String
) {
    enum class Direction { Up, Down, Unknown }
}
