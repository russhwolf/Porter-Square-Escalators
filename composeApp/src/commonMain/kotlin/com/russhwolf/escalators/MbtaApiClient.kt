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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

suspend fun main() {
    val client = MbtaApiClient(CIO.create())

    operator fun JsonElement?.get(key: String): JsonElement? = (this as? JsonObject)?.get(key)
    operator fun JsonElement?.get(key: Int): JsonElement? = (this as? JsonArray)?.get(key)
    fun JsonElement?.findObject(predicate: (JsonObject) -> Boolean): JsonElement? =
        (this as? JsonArray)?.find { it is JsonObject && predicate(it) }

    fun JsonElement?.filterObjects(predicate: (JsonObject) -> Boolean): JsonArray? =
        (this as? JsonArray)?.filter { it is JsonObject && predicate(it) }?.let { JsonArray(it) }

    fun JsonElement?.content(): String? = (this as? JsonPrimitive)?.content

    val facilities = client.escalators(PorterSquareId)
    val upDownFacilities = facilities["data"].filterObjects { it["id"].content() in BigEscalatorIds }
        ?.associate { it["id"] to it["attributes"]["properties"].findObject { it["name"].content() == "direction" }["value"] }
//    println(upDownFacilities)

    val alerts = client.escalatorAlerts(PorterSquareId, *BigEscalatorIds)
    val alertedFacilities = alerts["data"]?.filterObjects { true }
        ?.associate { it["attributes"]["informed_entity"].findObject { it["stop"].content() == PorterSquareId }["facility"] to it["attributes"]["header"] }
//    println(alertedFacilities)

    val facilityStatus =
        upDownFacilities?.mapValues { (key, value) -> value to (alertedFacilities?.get(key).content() ?: "WORKING") }

    facilityStatus?.forEach { (key, value) ->
        val (direction, alert) = value
        println("Escalator $key (direction=$direction) has status: $alert")
    }
}

class MbtaApiClient(engine: HttpClientEngine) {
    private val httpClient = HttpClient(engine) {
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

    suspend fun escalators(placeId: String): JsonElement = httpClient.get {
        url("https://api-v3.mbta.com/facilities?filter%5Bstop%5D=$placeId&filter%5Btype%5D=ESCALATOR")
    }.body()

    suspend fun escalatorAlerts(placeId: String, vararg facilityIds: String): JsonElement = httpClient.get {
        val facilityIdsList = facilityIds.joinToString("%2C")
        url("https://api-v3.mbta.com/alerts?filter%5Bactivity%5D=USING_ESCALATOR&filter%5Bstop%5D=${placeId}&filter%5Bfacility%5D=$facilityIdsList&filter%5Bdatetime%5D=NOW")
    }.body()
}

private const val PorterSquareId = "place-portr"
private val BigEscalatorIds = arrayOf("509", "510", "511")
