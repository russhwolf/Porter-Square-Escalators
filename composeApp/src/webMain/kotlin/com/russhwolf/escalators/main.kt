package com.russhwolf.escalators

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.ktor.client.engine.HttpClientEngine
import kotlinx.browser.document

expect val httpClientEngine: HttpClientEngine

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val mbtaApiClient = MbtaApiClient(httpClientEngine)

    val favicon = document.getElementById("favicon")

    ComposeViewport {
        App(mbtaApiClient) { isWorking ->
            favicon?.setAttribute("href", if (isWorking) "mbta-logo.ico" else "mbta-logo-fire.png")
        }
    }
}
