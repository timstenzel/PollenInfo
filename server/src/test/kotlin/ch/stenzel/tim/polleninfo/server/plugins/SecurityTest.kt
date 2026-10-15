package ch.stenzel.tim.polleninfo.server.plugins

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import ch.stenzel.tim.polleninfo.server.account.domain.AccountSpec
import ch.stenzel.tim.polleninfo.server.account.domain.DiaryEntry
import ch.stenzel.tim.polleninfo.server.account.domain.DiaryFeeling
import ch.stenzel.tim.polleninfo.server.account.domain.PasswordHasher
import ch.stenzel.tim.polleninfo.server.account.domain.SessionToken
import ch.stenzel.tim.polleninfo.server.account.domain.UserId
import ch.stenzel.tim.polleninfo.server.account.domain.cheapHasher
import ch.stenzel.tim.polleninfo.server.account.domain.newSessionToken
import ch.stenzel.tim.polleninfo.server.account.store.DiaryStore
import ch.stenzel.tim.polleninfo.server.account.store.RecordResult
import ch.stenzel.tim.polleninfo.server.account.store.RegisterResult
import ch.stenzel.tim.polleninfo.server.account.store.SessionStore
import ch.stenzel.tim.polleninfo.server.account.store.StoredCredentials
import ch.stenzel.tim.polleninfo.server.account.store.UserStore
import ch.stenzel.tim.polleninfo.server.alarm.domain.Alarm
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmId
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSpec
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceToken
import ch.stenzel.tim.polleninfo.server.alarm.domain.newDeviceToken
import ch.stenzel.tim.polleninfo.server.alarm.store.AlarmStore
import ch.stenzel.tim.polleninfo.server.alarm.store.AlarmWithToken
import ch.stenzel.tim.polleninfo.server.alarm.store.CreateResult
import ch.stenzel.tim.polleninfo.server.alarm.store.DeviceStore
import ch.stenzel.tim.polleninfo.server.config.RateLimits
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.history.HistoryService
import ch.stenzel.tim.polleninfo.server.pollen.measurement.MeasurementService
import ch.stenzel.tim.polleninfo.server.pollen.upstream.FakePollenService
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

/**
 * Rate limiting, the forwarded client address, the body size cap and the error answers — over fake
 * stores, so none of it needs a database.
 */
class SecurityTest {

    private val devices = RecordingDeviceStore()
    private val alarms = RecordingAlarmStore()
    private val users = RecordingUserStore()
    private val sessions = InMemorySessionStore()
    private val hasher = cheapHasher()
    private val pollenService = FakePollenService()

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

    private fun ApplicationTestBuilder.installApp(
        rateLimits: RateLimits = RateLimits(),
        trustedProxy: Boolean = false,
    ) {
        application {
            configureSerialization()
            configureLogging()
            configureSecurity(rateLimits, trustedProxy)
            val thresholds = PollenThresholds()
            configureRouting(
                thresholds = thresholds,
                measurementService = MeasurementService(pollenService, thresholds),
                historyService = HistoryService(pollenService, thresholds),
            )
            configureAlarmRouting(devices = devices, alarms = alarms)
            configureAccountRouting(
                users = users,
                sessions = sessions,
                diary = NoDiaryStore,
                hasher = hasher,
                clock = Clock.systemUTC(),
            )
        }
    }

    /** A valid registration under a fresh email, so only the limiter can refuse it. */
    private suspend fun ApplicationTestBuilder.registerUser(from: String? = null) = client.post("/users") {
        from?.let { header(HttpHeaders.XForwardedFor, it) }
        contentType(ContentType.Application.Json)
        setBody("""{"email":"user${users.registered.size}-${UUID.randomUUID()}@example.com","password":"correct horse","consent":true}""")
    }

    /** A login with the right password for an account that exists, so only the limiter can refuse it. */
    private suspend fun ApplicationTestBuilder.login(from: String? = null) = client.post("/users/session") {
        from?.let { header(HttpHeaders.XForwardedFor, it) }
        contentType(ContentType.Application.Json)
        setBody("""{"email":"${users.existing(hasher)}","password":"correct horse"}""")
    }

    private suspend fun ApplicationTestBuilder.register(from: String? = null) = client.post("/devices") {
        from?.let { header(HttpHeaders.XForwardedFor, it) }
        contentType(ContentType.Application.Json)
        setBody("""{"fcmToken":"token-1"}""")
    }

    private suspend fun ApplicationTestBuilder.species(from: String? = null) = client.get("/pollen/species") {
        from?.let { header(HttpHeaders.XForwardedFor, it) }
    }

    private suspend fun assertTooManyRequests(response: HttpResponse) {
        assertEquals(HttpStatusCode.TooManyRequests, response.status)
        val retryAfter = assertNotNull(response.headers[HttpHeaders.RetryAfter], "Retry-After")
        assertTrue(retryAfter.toLong() >= 0, retryAfter)
        assertEquals("Too many requests", response.errorText())
    }

    private suspend fun HttpResponse.errorText(): String =
        Json.parseToJsonElement(bodyAsText()).jsonObject.getValue("error").jsonPrimitive.content

    // --- Registration limiter ---

    @Test
    fun `the register limiter lets request N through and answers N+1 with 429`() = testApplication {
        installApp(RateLimits(registerPerHour = 3))

        repeat(3) { assertEquals(HttpStatusCode.Created, register().status, "registration ${it + 1}") }
        assertTooManyRequests(register())
        assertEquals(3, devices.registered.size)
    }

    @Test
    fun `the register limiter counts two addresses independently`() = testApplication {
        installApp(RateLimits(registerPerHour = 2), trustedProxy = true)

        repeat(2) { assertEquals(HttpStatusCode.Created, register(from = "203.0.113.1").status) }
        assertTooManyRequests(register(from = "203.0.113.1"))

        assertEquals(HttpStatusCode.Created, register(from = "203.0.113.2").status)
    }

    @Test
    fun `the default register limit is five per hour`() = testApplication {
        installApp()

        repeat(5) { assertEquals(HttpStatusCode.Created, register().status) }
        assertTooManyRequests(register())
    }

    // --- Account limiter ---

    @Test
    fun `the default account limit lets ten registrations through and answers the eleventh with 429`() =
        testApplication {
            installApp()

            repeat(10) { assertEquals(HttpStatusCode.Created, registerUser().status, "registration ${it + 1}") }
            assertTooManyRequests(registerUser())
            assertEquals(10, users.registered.size)
        }

    @Test
    fun `the account limiter counts two addresses independently`() = testApplication {
        installApp(RateLimits(accountPerHour = 2), trustedProxy = true)

        repeat(2) { assertEquals(HttpStatusCode.Created, registerUser(from = "203.0.113.1").status) }
        assertTooManyRequests(registerUser(from = "203.0.113.1"))

        assertEquals(HttpStatusCode.Created, registerUser(from = "203.0.113.2").status)
    }

    @Test
    fun `the account and device registration limiters do not share a bucket`() = testApplication {
        installApp(RateLimits(registerPerHour = 1, accountPerHour = 1))

        assertEquals(HttpStatusCode.Created, register().status)
        assertEquals(HttpStatusCode.Created, registerUser().status)
        assertTooManyRequests(register())
        assertTooManyRequests(registerUser())
    }

    @Test
    fun `logins and registrations share the account bucket`() = testApplication {
        installApp(RateLimits(accountPerHour = 3))

        assertEquals(HttpStatusCode.Created, registerUser().status)
        assertEquals(HttpStatusCode.Created, login().status)
        assertEquals(HttpStatusCode.Created, login().status)
        assertTooManyRequests(login())
        assertTooManyRequests(registerUser())
    }

    @Test
    fun `failed logins draw on the account bucket too`() = testApplication {
        installApp(RateLimits(accountPerHour = 2))

        repeat(2) {
            val response = client.post("/users/session") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"nobody@example.com","password":"guess-$it"}""")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
        assertTooManyRequests(login())
    }

    // --- User limiter ---

    @Test
    fun `the default user limit lets 60 calls a minute through per account and answers the 61st with 429`() =
        testApplication {
            installApp()
            val first = sessions.known()
            val second = sessions.known()

            repeat(60) {
                val response = client.delete("/users/me/session") { bearerAuth(first) }
                assertEquals(HttpStatusCode.NoContent, response.status, "call ${it + 1}")
            }
            assertTooManyRequests(client.delete("/users/me/session") { bearerAuth(first) })

            assertEquals(HttpStatusCode.NoContent, client.delete("/users/me/session") { bearerAuth(second) }.status)
        }

    @Test
    fun `unknown session tokens are refused with 401 and use no user bucket`() = testApplication {
        installApp(RateLimits(userPerMinute = 1))

        repeat(3) {
            val response = client.delete("/users/me/session") { bearerAuth(newSessionToken().value) }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    // --- Device limiter ---

    @Test
    fun `the device limiter lets call N through and answers N+1 with 429 per device`() = testApplication {
        installApp(RateLimits(devicePerMinute = 3))
        val first = devices.known()
        val second = devices.known()

        repeat(3) {
            assertEquals(HttpStatusCode.OK, client.get("/devices/me/alarms") { bearerAuth(first) }.status)
        }
        assertTooManyRequests(client.get("/devices/me/alarms") { bearerAuth(first) })

        assertEquals(HttpStatusCode.OK, client.get("/devices/me/alarms") { bearerAuth(second) }.status)
    }

    @Test
    fun `the device limiter counts per device even from one address`() = testApplication {
        // Both devices call from the same test client address; the second is not held back by
        // the first one's exhausted bucket.
        installApp(RateLimits(devicePerMinute = 1))
        val first = devices.known()
        val second = devices.known()

        assertEquals(HttpStatusCode.OK, client.get("/devices/me/alarms") { bearerAuth(first) }.status)
        assertTooManyRequests(client.get("/devices/me/alarms") { bearerAuth(first) })
        assertEquals(HttpStatusCode.OK, client.get("/devices/me/alarms") { bearerAuth(second) }.status)
    }

    @Test
    fun `unknown tokens are refused with 401 and use no device bucket`() = testApplication {
        installApp(RateLimits(devicePerMinute = 1))

        repeat(3) {
            val response = client.get("/devices/me/alarms") { bearerAuth(newDeviceToken().value) }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    // --- Pollen limiter ---

    @Test
    fun `the pollen limiter lets request N through and answers N+1 with 429`() = testApplication {
        installApp(RateLimits(pollenPerMinute = 3), trustedProxy = true)

        repeat(3) { assertEquals(HttpStatusCode.OK, species(from = "203.0.113.1").status) }
        assertTooManyRequests(species(from = "203.0.113.1"))

        assertEquals(HttpStatusCode.OK, species(from = "203.0.113.2").status)
    }

    @Test
    fun `at the default pollen limit eight full All stations refreshes pass and the next request is refused`() =
        testApplication {
            installApp()

            repeat(8) { round ->
                PollenStation.entries.forEach { station ->
                    val response = client.get("/pollen/stations/${station.abbr}/measurements")
                    assertEquals(HttpStatusCode.OK, response.status, "round ${round + 1}, ${station.abbr}")
                }
            }
            assertTooManyRequests(client.get("/pollen/stations/PZH/measurements"))
        }

    @Test
    fun `health is not rate-limited`() = testApplication {
        installApp(RateLimits(pollenPerMinute = 1))

        assertEquals(HttpStatusCode.OK, species().status)
        assertTooManyRequests(species())
        repeat(5) { assertEquals(HttpStatusCode.OK, client.get("/health").status) }
    }

    // --- Forwarded client address ---

    @Test
    fun `with a trusted proxy the bucket follows X-Forwarded-For`() = testApplication {
        installApp(RateLimits(pollenPerMinute = 1), trustedProxy = true)

        assertEquals(HttpStatusCode.OK, species(from = "203.0.113.1").status)
        assertTooManyRequests(species(from = "203.0.113.1"))
        assertEquals(HttpStatusCode.OK, species(from = "203.0.113.2").status)
    }

    @Test
    fun `with a trusted proxy the address the proxy appended counts and not one the caller claimed`() =
        testApplication {
            installApp(RateLimits(pollenPerMinute = 1), trustedProxy = true)

            assertEquals(HttpStatusCode.OK, species(from = "198.51.100.7, 203.0.113.1").status)
            assertTooManyRequests(species(from = "198.51.100.8, 203.0.113.1"))
        }

    @Test
    fun `without a trusted proxy a spoofed X-Forwarded-For does not change the bucket`() = testApplication {
        installApp(RateLimits(pollenPerMinute = 1), trustedProxy = false)

        assertEquals(HttpStatusCode.OK, species(from = "203.0.113.1").status)
        assertTooManyRequests(species(from = "203.0.113.2"))
    }

    // --- Body size ---

    private val oversizedRegistration = """{"fcmToken":"${"x".repeat(17 * 1024)}"}""".toByteArray()

    @Test
    fun `a 17 KB body with Content-Length is refused with 413 before the handler stores anything`() =
        testApplication {
            installApp()

            val response = client.post("/devices") {
                contentType(ContentType.Application.Json)
                setBody(oversizedRegistration)
            }

            assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
            assertTrue(devices.registered.isEmpty())
        }

    @Test
    fun `a 17 KB chunked body is refused with 413 while it is read`() = testApplication {
        installApp()

        val response = client.post("/devices") { chunked(oversizedRegistration) }

        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
        assertTrue(devices.registered.isEmpty())
    }

    @Test
    fun `a 17 KB chunked alarm body from a known device is refused with 413`() = testApplication {
        installApp()
        val token = devices.known()

        val response = client.post("/devices/me/alarms") {
            bearerAuth(token)
            chunked(oversizedRegistration)
        }

        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
        assertTrue(alarms.created.isEmpty())
    }

    @Test
    fun `a body just under the limit is read`() = testApplication {
        installApp()

        val response = client.post("/devices") {
            contentType(ContentType.Application.Json)
            setBody("""{"fcmToken":"${"x".repeat(15 * 1024)}"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    private fun HttpRequestBuilder.chunked(bytes: ByteArray) {
        setBody(
            object : OutgoingContent.WriteChannelContent() {
                override val contentType = ContentType.Application.Json
                override val contentLength: Long? = null
                override suspend fun writeTo(channel: ByteWriteChannel) {
                    bytes.asList().chunked(1024).forEach { channel.writeFully(it.toByteArray()) }
                }
            },
        )
    }

    // --- Errors ---

    @Test
    fun `an unexpected exception is a generic 500 and its details are only in the log`() = testApplication {
        installApp()
        application {
            routing {
                get("/boom") { throw IllegalStateException("secret detail") }
            }
        }

        val response = client.get("/boom")

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        val body = response.bodyAsText()
        assertEquals("internal error", response.errorText())
        assertFalse("secret detail" in body, body)
        assertFalse("IllegalStateException" in body || "\tat " in body, body)
        assertTrue(
            appender.list.any { it.throwableProxy?.message == "secret detail" },
            "the exception is logged",
        )
    }

    @Test
    fun `call logging writes neither the authorization header nor the body`() = testApplication {
        installApp()
        val token = devices.known()

        client.post("/devices/me/alarms") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"marker":"body-marker"}""")
        }

        val logged = appender.list.joinToString("\n") { it.formattedMessage }
        assertTrue("POST /devices/me/alarms" in logged, logged)
        assertFalse(token in logged || "Bearer" in logged || "body-marker" in logged, logged)
    }

    // --- Cross-origin ---

    @Test
    fun `a cross-origin request gets no Access-Control-Allow-Origin header`() = testApplication {
        installApp()

        val response = client.get("/pollen/species") { header(HttpHeaders.Origin, "https://evil.example") }

        assertEquals(HttpStatusCode.OK, response.status)
        assertNull(response.headers[HttpHeaders.AccessControlAllowOrigin])
    }

    @Test
    fun `a CORS preflight gets no Access-Control-Allow-Origin header`() = testApplication {
        installApp()

        val response = client.options("/devices/me/alarms") {
            header(HttpHeaders.Origin, "https://evil.example")
            header(HttpHeaders.AccessControlRequestMethod, HttpMethod.Post.value)
            header(HttpHeaders.AccessControlRequestHeaders, "authorization,content-type")
        }

        assertNull(response.headers[HttpHeaders.AccessControlAllowOrigin])
        assertNull(response.headers[HttpHeaders.AccessControlAllowMethods])
    }
}

/** Devices in memory; records every registration so a test can see whether a handler ran. */
private class RecordingDeviceStore : DeviceStore {
    private val tokens = mutableMapOf<String, DeviceId>()
    val registered = mutableListOf<String>()

    /** A device that exists, by its bearer token. */
    fun known(): String {
        val token = newDeviceToken()
        tokens[token.value] = DeviceId(UUID.randomUUID())
        return token.value
    }

    override suspend fun register(fcmToken: String): DeviceToken {
        registered += fcmToken
        return newDeviceToken().also { tokens[it.value] = DeviceId(UUID.randomUUID()) }
    }

    override suspend fun authenticate(token: DeviceToken): DeviceId? = tokens[token.value]
    override suspend fun updateFcmToken(id: DeviceId, fcmToken: String) = true
    override suspend fun clearFcmToken(id: DeviceId, fcmToken: String) = Unit
    override suspend fun delete(id: DeviceId) = true
    override suspend fun pruneInactive(now: Instant) = 0
}

/** No alarms; records every create so a test can see whether a handler ran. */
private class RecordingAlarmStore : AlarmStore {
    val created = mutableListOf<AlarmSpec>()

    override suspend fun list(deviceId: DeviceId) = emptyList<Alarm>()
    override suspend fun create(deviceId: DeviceId, spec: AlarmSpec): CreateResult {
        created += spec
        return CreateResult.LimitReached
    }

    override suspend fun update(deviceId: DeviceId, alarmId: AlarmId, spec: AlarmSpec) = null
    override suspend fun delete(deviceId: DeviceId, alarmId: AlarmId) = false
    override suspend fun enabledWithDeliverableDevice() = emptyList<AlarmWithToken>()
}

/** Accounts in memory; records every registration so a test can see whether a handler ran. */
private class RecordingUserStore : UserStore {
    val registered = mutableListOf<String>()
    private val accounts = mutableMapOf<String, StoredCredentials>()

    /** The email of an account whose password is "correct horse", created on first use. */
    fun existing(hasher: PasswordHasher): String {
        accounts.getOrPut(EXISTING) { StoredCredentials(UserId(UUID.randomUUID()), hasher.hash("correct horse")) }
        return EXISTING
    }

    override suspend fun register(spec: AccountSpec, passwordHash: String, now: Instant): RegisterResult {
        if (spec.email in registered) return RegisterResult.EmailTaken
        registered += spec.email
        val id = UserId(UUID.randomUUID())
        accounts[spec.email] = StoredCredentials(id, passwordHash)
        return RegisterResult.Registered(id)
    }

    override suspend fun credentials(email: String): StoredCredentials? = accounts[email]

    private companion object {
        const val EXISTING = "existing@example.com"
    }
}

/**
 * Sessions in memory. Deleting one is only recorded, so a test can call the logout route as often
 * as it needs to reach a limit with one token.
 */
private class InMemorySessionStore : SessionStore {
    private val sessions = mutableMapOf<String, UserId>()
    val deleted = mutableListOf<SessionToken>()

    /** A session that exists, by its bearer token. */
    fun known(): String {
        val token = newSessionToken()
        sessions[token.value] = UserId(UUID.randomUUID())
        return token.value
    }

    override suspend fun create(userId: UserId, now: Instant): SessionToken =
        newSessionToken().also { sessions[it.value] = userId }

    override suspend fun authenticate(token: SessionToken, now: Instant): UserId? = sessions[token.value]

    override suspend fun delete(token: SessionToken) {
        deleted += token
    }
}

/** A diary that keeps nothing; no security test reaches the diary routes. */
private object NoDiaryStore : DiaryStore {
    override suspend fun record(userId: UserId, date: LocalDate, feeling: DiaryFeeling, now: Instant): RecordResult =
        RecordResult.Recorded(DiaryEntry(date, feeling))

    override suspend fun entry(userId: UserId, date: LocalDate): DiaryEntry? = null

    override suspend fun entries(userId: UserId, from: LocalDate, until: LocalDate): List<DiaryEntry> = emptyList()
}
