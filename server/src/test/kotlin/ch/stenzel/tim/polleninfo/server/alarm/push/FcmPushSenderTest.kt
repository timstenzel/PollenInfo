package ch.stenzel.tim.polleninfo.server.alarm.push

import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmId
import ch.stenzel.tim.polleninfo.server.alarm.domain.PushChannel
import ch.stenzel.tim.polleninfo.server.alarm.domain.PushKind
import ch.stenzel.tim.polleninfo.server.alarm.domain.PushMessage
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
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
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FcmPushSenderTest {

    private val message = PushMessage(
        kind = PushKind.REPORT,
        channel = PushChannel.DAILY_REPORT,
        station = PollenStation.ZUERICH,
        alarmId = AlarmId("alarm-1"),
        levels = listOf(
            PollenSpecies.GRASSES to PollenSeverity.HIGH,
            PollenSpecies.BIRCH to PollenSeverity.MODERATE,
        ),
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
    fun `the body is a data-only message with the token and high Android priority`() = runTest {
        sender { respond("{}") }.send("device-token", message)

        val sent = sentMessage()
        assertEquals(setOf("token", "android", "data"), sent.keys)
        assertEquals("device-token", sent.string("token"))
        assertEquals(JsonObject(mapOf("priority" to JsonPrimitive("HIGH"))), sent.obj("android"))
    }

    @Test
    fun `a Firebase Installation ID is sent as fid and not as token`() = runTest {
        sender { respond("{}") }.send(FID, message)

        val sent = sentMessage()
        assertEquals(setOf("fid", "android", "data"), sent.keys)
        assertEquals(FID, sent.string("fid"))
    }

    @Test
    fun `a registration token from an app that has not updated is still sent as token`() = runTest {
        sender { respond("{}") }.send(REGISTRATION_TOKEN, message)

        val sent = sentMessage()
        assertEquals(setOf("token", "android", "data"), sent.keys)
        assertEquals(REGISTRATION_TOKEN, sent.string("token"))
    }

    @Test
    fun `an installation id is exactly 22 base64url characters`() {
        assertTrue(isInstallationId(FID))
        assertTrue(isInstallationId("A".repeat(22)))
        assertFalse(isInstallationId("A".repeat(21)))
        assertFalse(isInstallationId("A".repeat(23)))
        assertFalse(isInstallationId("A".repeat(21) + ":"))
        assertFalse(isInstallationId(REGISTRATION_TOKEN))
    }

    @Test
    fun `the data carries the kind and channel and station and alarm and levels as strings`() = runTest {
        sender { respond("{}") }.send("device-token", message)

        val data = sentMessage().obj("data")
        assertTrue(data.values.all { it is JsonPrimitive && it.isString })
        assertEquals(
            mapOf(
                "kind" to "report",
                "channel" to "daily_report",
                "stationAbbr" to "PZH",
                "stationName" to "Zürich",
                "alarmId" to "alarm-1",
                "levels" to "GRASSES:HIGH,BIRCH:MODERATE",
            ),
            data.mapValues { it.value.jsonPrimitive.content },
        )
    }

    @Test
    fun `a no current reading message carries measuredAt as an ISO instant and no levels`() = runTest {
        val noCurrent = message.copy(
            kind = PushKind.NO_CURRENT_READING,
            levels = emptyList(),
            measuredAt = Instant.parse("2026-08-02T20:00:00Z"),
        )

        sender { respond("{}") }.send("device-token", noCurrent)

        val data = sentMessage().obj("data")
        assertEquals("no_current_reading", data.string("kind"))
        assertEquals("2026-08-02T20:00:00Z", data.string("measuredAt"))
        assertFalse("levels" in data)
    }

    @Test
    fun `a threshold alert names its channel and has no measuredAt`() = runTest {
        val alert = message.copy(kind = PushKind.ALERT, channel = PushChannel.THRESHOLD_ALERT)

        sender { respond("{}") }.send("device-token", alert)

        val data = sentMessage().obj("data")
        assertEquals("alert", data.string("kind"))
        assertEquals("threshold_alert", data.string("channel"))
        assertFalse("measuredAt" in data)
    }

    private fun sentMessage(): JsonObject {
        val content = assertIs<TextContent>(requests.single().body)
        assertEquals("application/json", content.contentType.withoutParameters().toString())
        return Json.parseToJsonElement(content.text).jsonObject.getValue("message").jsonObject
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
        // The shapes FCM issues: a 22 character FID, and a registration token as issued before
        // firebase-messaging 26 (sender id, a colon, then the long body).
        const val FID = "cK3xmP9qRAWv-_7tLz2bUq"
        const val REGISTRATION_TOKEN =
            "fXk2Q9e0RtS3abcdEfGh12:APA91bHj4kLmNoPqRsTuVwXyZ0123456789abcdefGhIjKlMnOpQrStUvWxYz-_0123456789"

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
