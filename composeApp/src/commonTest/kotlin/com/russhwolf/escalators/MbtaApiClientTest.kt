package com.russhwolf.escalators

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MbtaApiClientTest {

    private fun apiTest(engine: HttpClientEngine, expectedResponse: EscalatorResponse) = runTest {
        val client = MbtaApiClient(engine)
        val response = client.getPorterEscalatorStatus()
        assertEquals(expectedResponse, response)
    }

    @Test
    fun escalatorsPopulated_alertsPopulated() = apiTest(
        MockMbtaApiEngine.create(FacilitiesResponse_Populated, AlertsResponse_Populated),
        EscalatorResponse.Success(listOf(Escalator509_NotWorking, Escalator510_NotWorking, Escalator511_Working))
    )

    @Test
    fun escalatorsPopulated_alertsEmpty() = apiTest(
        MockMbtaApiEngine.create(FacilitiesResponse_Populated, AlertsResponse_Empty),
        EscalatorResponse.Success(listOf(Escalator509_Working, Escalator510_Working, Escalator511_Working))
    )

    @Test
    fun escalatorsEmpty_alertsPopulated() = apiTest(
        MockMbtaApiEngine.create(FacilitiesResponse_Empty, AlertsResponse_Populated),
        EscalatorResponse.Success(emptyList())
    )

    @Test
    fun escalatorsEmpty_alertsEmpty() = apiTest(
        MockMbtaApiEngine.create(FacilitiesResponse_Empty, AlertsResponse_Empty),
        EscalatorResponse.Success(emptyList())
    )

    @Test
    fun escalatorsError() = apiTest(
        MockMbtaApiEngine.create(
            { respondBadRequest() },
            { respondJson(AlertsResponse_Empty) }
        ),
        EscalatorResponse.Failure
    )

    @Test
    fun alertsError() = apiTest(
        MockMbtaApiEngine.create(
            { respondJson(FacilitiesResponse_Populated) },
            { respondError(HttpStatusCode.InternalServerError) }
        ),
        EscalatorResponse.Failure
    )
}

private object MockMbtaApiEngine {
    fun create(
        excalatorsJson: String,
        alertsJson: String,
    ) = create(
        { respondJson(excalatorsJson) },
        { respondJson(alertsJson) },
    )

    fun create(
        escalatorsResponse: MockRequestHandleScope.() -> HttpResponseData,
        alertsResponse: MockRequestHandleScope.() -> HttpResponseData
    ): HttpClientEngine = MockEngine.create {
        addHandler {
            val requestUrl = it.url.toString()
            when (requestUrl) {
                "https://api-v3.mbta.com/facilities?filter%5Bstop%5D=place-portr&filter%5Btype%5D=ESCALATOR" -> escalatorsResponse()
                "https://api-v3.mbta.com/alerts?filter%5Bactivity%5D=USING_ESCALATOR&filter%5Bstop%5D=place-portr&filter%5Bfacility%5D=509%2C510%2C511&filter%5Bdatetime%5D=NOW" -> alertsResponse()
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
    }
}

private fun MockRequestHandleScope.respondJson(content: String) =
    respond(
        content = content,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )

private val Escalator509_Working = Escalator(
    "509",
    "Porter Escalator 509 (Paid lobby to Ashmont/Braintree platform)",
    Escalator.Direction.Down,
    true,
    "WORKING"
)
private val Escalator509_NotWorking =
    Escalator509_Working.withStatus("Porter Escalator 509 (Paid lobby to Ashmont/Braintree platform) unavailable due to maintenance")

private val Escalator510_Working = Escalator(
    "510",
    "Porter Escalator 510 (Ashmont/Braintree platform to paid lobby)",
    Escalator.Direction.Up,
    true,
    "WORKING"
)
private val Escalator510_NotWorking =
    Escalator510_Working.withStatus("Porter Escalator 510 (Ashmont/Braintree platform to paid lobby) unavailable due to maintenance")

private val Escalator511_Working = Escalator(
    "511",
    "Porter Escalator 511 (Ashmont/Braintree platform to paid lobby)",
    Escalator.Direction.Up,
    true,
    "WORKING"
)

private fun Escalator.withStatus(status: String) = copy(isWorking = false, status = status)
