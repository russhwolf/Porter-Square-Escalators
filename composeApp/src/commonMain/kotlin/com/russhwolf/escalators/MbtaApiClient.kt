package com.russhwolf.escalators

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

suspend fun main() {
    MbtaApiClient(CIO.create()).getPorterEscalatorStatus().forEach {
        println("Escalator ${it.description} (direction=${it.direction}) has status: ${it.status}")
    }
}

private const val PorterSquareId = "place-portr"
private val BigEscalatorIds = arrayOf("509", "510", "511")

class MbtaApiClient(engine: HttpClientEngine) {
    private val httpClient = HttpClient(engine) {
        expectSuccess = true

        install(Logging) {
            level = LogLevel.ALL
        }

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    suspend fun getPorterEscalatorStatus(): List<Escalator> = coroutineScope {
        val escalatorsJsonDeferred = async { getEscalators(PorterSquareId) }
        val alertsJsonDeferred = async { getEscalatorAlerts(PorterSquareId, *BigEscalatorIds) }
        val (escalatorsJson, alertsJson) = awaitAll(escalatorsJsonDeferred, alertsJsonDeferred)

        val selectedEscalators = escalatorsJson["data"].filterObjects { it["id"].content() in BigEscalatorIds }
        val alertedEscalators = alertsJson["data"]?.coerceToArray()
            ?.associate { it["attributes"]["informed_entity"].findObject { it["stop"].content() == PorterSquareId }["facility"].content() to it["attributes"]["header"] }

        selectedEscalators?.map { escalatorJson ->
            val id = escalatorJson["id"].content().orEmpty()
            val directionString =
                escalatorJson["attributes"]["properties"].findObject { it["name"].content() == "direction" }["value"].content()
            val description = escalatorJson["attributes"]["long_name"].content().orEmpty()
            val alert = alertedEscalators?.get(id).content()

            Escalator(
                id = id,
                description = description,
                direction = when (directionString) {
                    "up" -> Escalator.Direction.Up
                    "down" -> Escalator.Direction.Down
                    else -> Escalator.Direction.Unknown
                },
                isWorking = alert == null,
                status = alert ?: "WORKING"
            )
        }.orEmpty()
    }

    private suspend fun getEscalators(placeId: String): JsonElement = httpClient.get {
        url("https://api-v3.mbta.com/facilities?filter%5Bstop%5D=$placeId&filter%5Btype%5D=ESCALATOR")
    }.body()

    private suspend fun getEscalatorAlerts(placeId: String, vararg facilityIds: String): JsonElement = httpClient.get {
        val facilityIdsList = facilityIds.joinToString("%2C")
        url("https://api-v3.mbta.com/alerts?filter%5Bactivity%5D=USING_ESCALATOR&filter%5Bstop%5D=${placeId}&filter%5Bfacility%5D=$facilityIdsList&filter%5Bdatetime%5D=NOW")
    }.body()
}


private fun JsonElement.coerceToArray() = when (this) {
    is JsonArray -> this
    is JsonObject, is JsonPrimitive -> JsonArray(listOf(this))
    JsonNull -> JsonArray(emptyList())
}

private operator fun JsonElement?.get(key: String): JsonElement? = (this as? JsonObject)?.get(key)

private fun JsonElement?.findObject(predicate: (JsonObject) -> Boolean): JsonElement? =
    this?.coerceToArray()?.find { it is JsonObject && predicate(it) }

private fun JsonElement?.filterObjects(predicate: (JsonObject) -> Boolean): JsonArray? =
    this?.coerceToArray()?.filter { it is JsonObject && predicate(it) }?.let { JsonArray(it) }

private fun JsonElement?.content(): String? = (this as? JsonPrimitive)?.content
