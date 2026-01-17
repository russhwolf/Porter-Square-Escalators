package com.russhwolf.escalators

data class Escalator(
    val id: String,
    val description: String,
    val direction: Direction,
    val isWorking: Boolean,
    val status: String
) {
    enum class Direction { Up, Down, Unknown }
}
