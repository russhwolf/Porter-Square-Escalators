package com.russhwolf.escalators

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.ktor.client.engine.HttpClientEngine

expect val httpClientEngine: HttpClientEngine

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val mbtaApiClient = MbtaApiClient(httpClientEngine)

    ComposeViewport {
        App(mbtaApiClient)
    }
}
