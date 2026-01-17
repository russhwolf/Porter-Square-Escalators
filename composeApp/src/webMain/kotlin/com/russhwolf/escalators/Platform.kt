package com.russhwolf.escalators

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform