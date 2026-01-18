package com.russhwolf.escalators

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.client.engine.okhttp.OkHttp

fun main() {
    val mbtaApiClient = MbtaApiClient(OkHttp.create())

    application {
        Window(onCloseRequest = ::exitApplication) {
            App(mbtaApiClient)
        }
    }
}
