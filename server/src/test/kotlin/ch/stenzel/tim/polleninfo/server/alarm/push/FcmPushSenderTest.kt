package ch.stenzel.tim.polleninfo.server.alarm.push

import ch.stenzel.tim.polleninfo.server.alarm.domain.PushChannel
import ch.stenzel.tim.polleninfo.server.alarm.domain.PushMessage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FcmPushSenderTest {

    private val message = PushMessage(
        title = "Pollen in Zürich",
        body = "Grasses: High · Birch: Moderate",
        channel = PushChannel.DAILY_REPORT,
        data = mapOf("stationAbbr" to "PZH", "alarmId" to "alarm-1"),
    )

    private val requests = mutableListOf<HttpRequestData>()

    private fun sender(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        FcmPushSender(
            client = HttpClient(MockEngine { request ->
                requests += request
                handler(request)
            }),
            projectId = "pollen-test",
            accessToken = { "access-token-1" },
            baseUrl = "https://fcm.test",
        )

    private fun MockRequestHandleScope.error(status: HttpStatusCode, body: String) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun `posts to the project's send endpoint with the access token as bearer`() = runTest {
        sender { respond("""{"name":"projects/pollen-test/messages/1"}""") }.send("device-token", message)

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("https://fcm.test/v1/projects/pollen-test/messages:send", request.url.toString())
        assertEquals("Bearer access-token-1", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun `the body carries the token and the notification and the channel and the data`() = runTest {
        sender { respond("{}") }.send("device-token", message)

        val content = assertIs<TextContent>(requests.single().body)
        assertEquals("application/json", content.contentType.withoutParameters().toString())
        val sent = Json.parseToJsonElement(content.text).jsonObject.getValue("message").jsonObject
        assertEquals("device-token", sent.string("token"))
        assertEquals("Pollen in Zürich", sent.obj("notification").string("title"))
        assertEquals("Grasses: High · Birch: Moderate", sent.obj("notification").string("body"))
        assertEquals("daily_report", sent.obj("android").obj("notification").string("channel_id"))
        assertEquals("PZH", sent.obj("data").string("stationAbbr"))
        assertEquals("alarm-1", sent.obj("data").string("alarmId"))
    }

    @Test
    fun `200 is Sent`() = runTest {
        assertEquals(PushResult.Sent, sender { respond("{}") }.send("device-token", message))
    }

    @Test
    fun `404 UNREGISTERED is Unregistered`() = runTest {
        val result = sender { error(HttpStatusCode.NotFound, UNREGISTERED_BODY) }.send("device-token", message)

        assertEquals(PushResult.Unregistered, result)
    }

    @Test
    fun `400 INVALID_ARGUMENT is Unregistered`() = runTest {
        val result = sender { error(HttpStatusCode.BadRequest, INVALID_ARGUMENT_BODY) }.send("device-token", message)

        assertEquals(PushResult.Unregistered, result)
    }

    @Test
    fun `404 without an FCM error body is Failed`() = runTest {
        val result = sender { error(HttpStatusCode.NotFound, "Not Found") }.send("device-token", message)

        assertIs<PushResult.Failed>(result)
    }

    @Test
    fun `500 is Failed`() = runTest {
        val result = sender { error(HttpStatusCode.InternalServerError, INTERNAL_BODY) }.send("device-token", message)

        assertIs<PushResult.Failed>(result)
    }

    @Test
    fun `a timeout is Failed`() = runTest {
        // Thrown directly rather than waited for: the test asserts the mapping, not the client's timer.
        val result = sender { request -> throw HttpRequestTimeoutException(request.url.toString(), 15_000) }
            .send("device-token", message)

        assertIs<HttpRequestTimeoutException>(assertIs<PushResult.Failed>(result).cause)
    }

    @Test
    fun `a failing access token is Failed without a request`() = runTest {
        val sender = FcmPushSender(
            client = HttpClient(MockEngine { request -> requests += request; respond("{}") }),
            projectId = "pollen-test",
            accessToken = { throw IllegalStateException("no token") },
        )

        assertIs<PushResult.Failed>(sender.send("device-token", message))
        assertEquals(emptyList(), requests)
    }

    private fun JsonObject.obj(key: String) = getValue(key).jsonObject

    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content

    private companion object {
        // Shapes as documented for the HTTP v1 API's error responses.
        const val UNREGISTERED_BODY = """{"error":{"code":404,"message":"Requested entity was not found.",
            "status":"NOT_FOUND","details":[{"@type":"type.googleapis.com/google.firebase.fcm.v1.FcmError",
            "errorCode":"UNREGISTERED"}]}}"""
        const val INVALID_ARGUMENT_BODY = """{"error":{"code":400,
            "message":"The registration token is not a valid FCM registration token",
            "status":"INVALID_ARGUMENT","details":[{"@type":"type.googleapis.com/google.firebase.fcm.v1.FcmError",
            "errorCode":"INVALID_ARGUMENT"}]}}"""
        const val INTERNAL_BODY = """{"error":{"code":500,"message":"Internal error","status":"INTERNAL",
            "details":[{"@type":"type.googleapis.com/google.firebase.fcm.v1.FcmError","errorCode":"INTERNAL"}]}}"""
    }
}
