package ch.stenzel.tim.polleninfo.server.alarm

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.read.ListAppender
import ch.stenzel.tim.polleninfo.server.alarm.model.RegisterDeviceResponse
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedAlarmStore
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedDeviceStore
import ch.stenzel.tim.polleninfo.server.alarm.store.TestPostgres
import ch.stenzel.tim.polleninfo.server.plugins.configureAlarmRouting
import ch.stenzel.tim.polleninfo.server.plugins.configureLogging
import ch.stenzel.tim.polleninfo.server.plugins.configureSecurity
import ch.stenzel.tim.polleninfo.server.plugins.configureSerialization
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

/**
 * What a whole device flow writes to the log, at the levels the server is configured with: neither
 * the device token nor a push address may appear in full, so log access never grants access to an
 * install.
 */
class DeviceFlowLoggingTest {

    private val database = TestPostgres.cleanDatabase()
    private val root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
    private val appender = ListAppender<ILoggingEvent>()

    @BeforeTest
    fun captureLog() {
        appender.start()
        root.addAppender(appender)
    }

    @AfterTest
    fun releaseLog() {
        root.detachAppender(appender)
        appender.stop()
    }

    /** Shaped like Firebase Installation IDs, so a leak would look like a real one. */
    private val firstAddress = "cQ8vXk2RTbW1eLmN0pYz3a"
    private val secondAddress = "fH7sJd4KqUo9iEw5rTy6uB"

    @Test
    fun `a full device flow logs neither the device token nor a push address`() = testApplication {
        application {
            configureSerialization()
            configureSecurity()
            configureLogging()
            configureAlarmRouting(devices = ExposedDeviceStore(database), alarms = ExposedAlarmStore(database))
        }

        val token = Json.decodeFromString<RegisterDeviceResponse>(
            client.post("/devices") {
                contentType(ContentType.Application.Json)
                setBody("""{"fcmToken":"$firstAddress"}""")
            }.bodyAsText(),
        ).deviceToken
        assertEquals(HttpStatusCode.OK, client.get("/devices/me/alarms") { bearerAuth(token) }.status)
        val created = client.post("/devices/me/alarms") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                """
                { "enabled": true, "stationAbbr": "PBE", "species": ["BIRCH"], "minSeverity": "NONE",
                  "days": ["MONDAY"], "schedule": { "type": "daily", "at": "07:30" } }
                """,
            )
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val alarmId = Json.parseToJsonElement(created.bodyAsText()).jsonObject.getValue("id").jsonPrimitive.content
        val tokenUpdate = client.put("/devices/me/fcm-token") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"fcmToken":"$secondAddress"}""")
        }
        assertEquals(HttpStatusCode.NoContent, tokenUpdate.status)
        assertEquals(HttpStatusCode.NoContent, client.delete("/devices/me/alarms/$alarmId") { bearerAuth(token) }.status)
        assertEquals(HttpStatusCode.NoContent, client.delete("/devices/me") { bearerAuth(token) }.status)

        val logged = appender.list.map { event ->
            event.formattedMessage + event.throwableProxy?.let(ThrowableProxyUtil::asString).orEmpty()
        }
        // Call logging wrote a line per request, so the capture itself works.
        assertTrue(logged.any { "/devices/me/alarms" in it }, "captured: $logged")
        listOf(token, firstAddress, secondAddress).forEach { secret ->
            assertTrue(logged.none { secret in it }, "a log line contains $secret: $logged")
        }
    }
}
