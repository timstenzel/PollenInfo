package ch.stenzel.tim.polleninfo.server.account

import ch.stenzel.tim.polleninfo.server.account.domain.SESSION_TOKEN_LENGTH
import ch.stenzel.tim.polleninfo.server.account.domain.SessionToken
import ch.stenzel.tim.polleninfo.server.account.domain.cheapHasher
import ch.stenzel.tim.polleninfo.server.account.domain.hash
import ch.stenzel.tim.polleninfo.server.account.domain.newSessionToken
import ch.stenzel.tim.polleninfo.server.account.model.SessionDto
import ch.stenzel.tim.polleninfo.server.account.model.DiaryEntryDto
import ch.stenzel.tim.polleninfo.server.account.store.DiaryEntriesTable
import ch.stenzel.tim.polleninfo.server.account.store.ExposedDiaryStore
import ch.stenzel.tim.polleninfo.server.account.store.ExposedSessionStore
import ch.stenzel.tim.polleninfo.server.account.store.ExposedUserStore
import ch.stenzel.tim.polleninfo.server.account.store.SessionsTable
import ch.stenzel.tim.polleninfo.server.account.store.UsersTable
import ch.stenzel.tim.polleninfo.server.alarm.domain.newDeviceToken
import ch.stenzel.tim.polleninfo.server.alarm.model.RegisterDeviceResponse
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedAlarmStore
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedDeviceStore
import ch.stenzel.tim.polleninfo.server.alarm.store.TestPostgres
import ch.stenzel.tim.polleninfo.server.config.RateLimits
import ch.stenzel.tim.polleninfo.server.plugins.configureAccountRouting
import ch.stenzel.tim.polleninfo.server.plugins.configureAlarmRouting
import ch.stenzel.tim.polleninfo.server.plugins.configureSecurity
import ch.stenzel.tim.polleninfo.server.plugins.configureSerialization
import ch.stenzel.tim.polleninfo.server.pollen.measurement.MutableClock
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class AccountRoutesTest {

    private val database = TestPostgres.cleanDatabase()
    private val users = ExposedUserStore(database)
    private val sessions = ExposedSessionStore(database)
    private val diary = ExposedDiaryStore(database)
    private val hasher = cheapHasher()
    private val clock = MutableClock(Instant.parse("2026-10-12T08:30:00Z"))

    private fun ApplicationTestBuilder.installApp() {
        application {
            configureSerialization()
            // Generous, so the route tests never meet the account limiter; SecurityTest pins it.
            configureSecurity(RateLimits(accountPerHour = 1_000))
            configureAccountRouting(users = users, sessions = sessions, diary = diary, hasher = hasher, clock = clock)
        }
    }

    private suspend fun ApplicationTestBuilder.register(body: String): HttpResponse = client.post("/users") {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private fun registration(
        email: String = "tim@example.com",
        password: String = "correct horse",
        consent: Boolean = true,
    ) = """{"email":"$email","password":"$password","consent":$consent}"""

    private suspend fun HttpResponse.errorText(): String =
        Json.parseToJsonElement(bodyAsText()).jsonObject.getValue("error").jsonPrimitive.content

    private fun userCount() = transaction(database) { UsersTable.selectAll().count() }

    @Test
    fun `a valid registration is 201 with a 43-character session token for the new user`() = testApplication {
        installApp()

        val response = register(registration())

        assertEquals(HttpStatusCode.Created, response.status)
        val token = Json.decodeFromString<SessionDto>(response.bodyAsText()).sessionToken
        assertEquals(SESSION_TOKEN_LENGTH, token.length)
        val (user, session) = transaction(database) {
            UsersTable.selectAll().single() to
                SessionsTable.selectAll().where { SessionsTable.tokenHash eq SessionToken(token).hash() }.single()
        }
        assertEquals(session[SessionsTable.userId], user[UsersTable.id])
        assertEquals(clock.now, user[UsersTable.consentedAt].toInstant())
    }

    @Test
    fun `the email is stored normalised and the password only as an Argon2id hash`() = testApplication {
        installApp()

        register(registration(email = "  Tim@Example.COM "))

        val row = transaction(database) { UsersTable.selectAll().single() }
        assertEquals("tim@example.com", row[UsersTable.email])
        val stored = row[UsersTable.passwordHash]
        assertTrue(stored.startsWith("\$argon2id\$"), stored)
        assertFalse("correct horse" in stored)
        assertTrue(hasher.verify("correct horse", stored))
    }

    @Test
    fun `an email already registered in other capitals is 409`() = testApplication {
        installApp()
        assertEquals(HttpStatusCode.Created, register(registration(email = "tim@example.com")).status)

        val response = register(registration(email = "Tim@Example.com", password = "another one"))

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("An account with this email exists already", response.errorText())
        assertEquals(1L, userCount())
    }

    @Test
    fun `each invalid registration is 400 with an error and stores nothing`() = testApplication {
        installApp()
        val invalid = mapOf(
            "malformed email" to registration(email = "tim.example.com"),
            "email of 255 characters" to registration(email = "a".repeat(243) + "@example.com"),
            "password of 7 characters" to registration(password = "1234567"),
            "password of 129 characters" to registration(password = "a".repeat(129)),
            "no consent" to registration(consent = false),
            "consent missing" to """{"email":"tim@example.com","password":"correct horse"}""",
            "password missing" to """{"email":"tim@example.com","consent":true}""",
            "consent not a boolean" to """{"email":"tim@example.com","password":"correct horse","consent":"yes"}""",
            "not JSON" to "email=tim@example.com",
        )

        invalid.forEach { (case, body) ->
            val response = register(body)
            assertEquals(HttpStatusCode.BadRequest, response.status, case)
            assertTrue(response.errorText().isNotBlank(), case)
        }
        assertEquals(0L, userCount())
    }

    @Test
    fun `the boundary registrations are accepted`() = testApplication {
        installApp()

        assertEquals(HttpStatusCode.Created, register(registration(email = "a".repeat(242) + "@example.com")).status)
        assertEquals(HttpStatusCode.Created, register(registration(email = "b@example.com", password = "12345678")).status)
        assertEquals(HttpStatusCode.Created, register(registration(email = "c@example.com", password = "a".repeat(128))).status)
    }

    @Test
    fun `an error body never repeats the email or the password`() = testApplication {
        installApp()
        register(registration())

        val bodies = listOf(
            register(registration()).bodyAsText(),
            register(registration(email = "tim@", password = "secret-password")).bodyAsText(),
            register(registration(email = "anna@example.com", password = "shorty")).bodyAsText(),
        )

        bodies.forEach { body ->
            assertFalse("tim@" in body || "secret-password" in body || "shorty" in body || "anna" in body, body)
        }
    }

    @Test
    fun `two registrations give two different sessions`() = testApplication {
        installApp()

        val first = register(registration(email = "tim@example.com")).bodyAsText()
        val second = register(registration(email = "anna@example.com")).bodyAsText()

        assertNotNull(first)
        assertFalse(first == second)
        assertEquals(2L, transaction(database) { SessionsTable.selectAll().count() })
    }

    // --- Login ---

    private suspend fun ApplicationTestBuilder.login(body: String): HttpResponse = client.post("/users/session") {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private fun credentials(email: String = "tim@example.com", password: String = "correct horse") =
        """{"email":"$email","password":"$password"}"""

    private suspend fun ApplicationTestBuilder.logout(token: String?): HttpResponse =
        client.delete("/users/me/session") { token?.let { bearerAuth(it) } }

    private suspend fun HttpResponse.sessionToken(): String =
        Json.decodeFromString<SessionDto>(bodyAsText()).sessionToken

    @Test
    fun `the right credentials in any capitals are 201 with a new session token`() = testApplication {
        installApp()
        val registered = register(registration()).sessionToken()

        val response = login(credentials(email = "  TIM@Example.com "))

        assertEquals(HttpStatusCode.Created, response.status)
        val token = response.sessionToken()
        assertEquals(SESSION_TOKEN_LENGTH, token.length)
        assertNotEquals(registered, token)
        assertEquals(HttpStatusCode.NoContent, logout(token).status)
    }

    @Test
    fun `a wrong password and an unknown email get identical 401 answers`() = testApplication {
        installApp()
        register(registration())

        val wrongPassword = login(credentials(password = "wrong horse"))
        val unknownEmail = login(credentials(email = "anna@example.com"))
        val notAnEmail = login(credentials(email = "anna"))

        val responses = listOf(wrongPassword, unknownEmail, notAnEmail)
        responses.forEach { assertEquals(HttpStatusCode.Unauthorized, it.status) }
        assertEquals("Email or password is incorrect", wrongPassword.errorText())
        assertEquals(1, responses.map { it.bodyAsText() }.distinct().size)
        assertEquals(1L, transaction(database) { SessionsTable.selectAll().count() })
    }

    @Test
    fun `the password of a login is not trimmed`() = testApplication {
        installApp()
        register(registration())

        assertEquals(HttpStatusCode.Unauthorized, login(credentials(password = " correct horse ")).status)
    }

    @Test
    fun `a malformed login is 400 with an error`() = testApplication {
        installApp()
        listOf(
            """{"email":"tim@example.com"}""",
            """{"password":"correct horse"}""",
            "email=tim@example.com",
        ).forEach { body ->
            val response = login(body)
            assertEquals(HttpStatusCode.BadRequest, response.status, body)
            assertTrue(response.errorText().isNotBlank(), body)
        }
    }

    // --- Logout and the user provider ---

    @Test
    fun `after logout that token is 401 while another session of the same user still works`() = testApplication {
        installApp()
        val first = register(registration()).sessionToken()
        val second = login(credentials()).sessionToken()

        assertEquals(HttpStatusCode.NoContent, logout(first).status)

        assertEquals(HttpStatusCode.Unauthorized, logout(first).status)
        assertEquals(HttpStatusCode.NoContent, logout(second).status)
    }

    @Test
    fun `a missing malformed or unknown token is 401 with WWW-Authenticate Bearer`() = testApplication {
        installApp()
        val cases = mapOf(
            "missing" to client.delete("/users/me/session"),
            "not the issued shape" to logout("too-short"),
            "unknown" to logout(newSessionToken().value),
            "another scheme" to client.delete("/users/me/session") {
                header(HttpHeaders.Authorization, "Basic dGltOnNlY3JldA==")
            },
            "unparseable header" to client.delete("/users/me/session") {
                header(HttpHeaders.Authorization, "Bearer a b")
            },
        )

        cases.forEach { (case, response) ->
            assertEquals(HttpStatusCode.Unauthorized, response.status, case)
            assertTrue(
                response.headers[HttpHeaders.WWWAuthenticate].orEmpty().startsWith("Bearer"),
                "$case: ${response.headers[HttpHeaders.WWWAuthenticate]}",
            )
        }
    }

    @Test
    fun `a device token is not a session token`() = testApplication {
        installApp()

        assertEquals(HttpStatusCode.Unauthorized, logout(newDeviceToken().value).status)
    }

    @Test
    fun `a session unused for more than 90 days is 401 and one unused for exactly 90 days is not`() = testApplication {
        installApp()
        val kept = register(registration(email = "tim@example.com")).sessionToken()
        val expired = register(registration(email = "anna@example.com")).sessionToken()

        clock.now = clock.now.plus(Duration.ofDays(90))
        assertEquals(HttpStatusCode.NoContent, logout(kept).status)

        clock.now = clock.now.plusMillis(1)
        assertEquals(HttpStatusCode.Unauthorized, logout(expired).status)
    }

    @Test
    fun `the device and the account routes can be installed together`() = testApplication {
        application {
            configureSerialization()
            configureSecurity(RateLimits(accountPerHour = 1_000))
            configureAlarmRouting(devices = ExposedDeviceStore(database), alarms = ExposedAlarmStore(database))
            configureAccountRouting(users = users, sessions = sessions, diary = diary, hasher = hasher, clock = clock)
        }
        val session = register(registration()).sessionToken()
        val device = client.post("/devices") {
            contentType(ContentType.Application.Json)
            setBody("""{"fcmToken":"token-1"}""")
        }.let { Json.decodeFromString<RegisterDeviceResponse>(it.bodyAsText()).deviceToken }

        assertEquals(HttpStatusCode.OK, client.get("/devices/me/alarms") { bearerAuth(device) }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/devices/me/alarms") { bearerAuth(session) }.status)
        assertEquals(HttpStatusCode.Unauthorized, logout(device).status)
        assertEquals(HttpStatusCode.NoContent, logout(session).status)
    }

    // --- Diary ---

    private suspend fun ApplicationTestBuilder.answer(token: String?, body: String): HttpResponse =
        client.post("/users/me/diary") {
            token?.let { bearerAuth(it) }
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private fun feeling(name: String) = """{"feeling":"$name"}"""

    private suspend fun ApplicationTestBuilder.today(token: String?): HttpResponse =
        client.get("/users/me/diary/today") { token?.let { bearerAuth(it) } }

    private suspend fun HttpResponse.entry(): DiaryEntryDto = Json.decodeFromString(bodyAsText())

    @Test
    fun `the first answer of a Swiss day is 201 with that date and a second is 200 with the first`() = testApplication {
        installApp()
        val token = register(registration()).sessionToken()

        val first = answer(token, feeling("GOOD"))
        val second = answer(token, feeling("VERY_BAD"))

        assertEquals(HttpStatusCode.Created, first.status)
        assertEquals(DiaryEntryDto(date = "2026-10-12", feeling = "GOOD"), first.entry())
        assertEquals(HttpStatusCode.OK, second.status)
        assertEquals(DiaryEntryDto(date = "2026-10-12", feeling = "GOOD"), second.entry())
        assertEquals(1L, transaction(database) { DiaryEntriesTable.selectAll().count() })
    }

    @Test
    fun `answers either side of Swiss midnight get different dates`() = testApplication {
        installApp()
        val token = register(registration()).sessionToken()

        // 23:59:59 in Zürich on 12 October (summer time, UTC+2) and the next second, already the 13th.
        clock.now = Instant.parse("2026-10-12T21:59:59Z")
        val evening = answer(token, feeling("BAD"))
        clock.now = Instant.parse("2026-10-12T22:00:00Z")
        val midnight = answer(token, feeling("VERY_GOOD"))

        assertEquals(HttpStatusCode.Created, evening.status)
        assertEquals(DiaryEntryDto(date = "2026-10-12", feeling = "BAD"), evening.entry())
        assertEquals(HttpStatusCode.Created, midnight.status)
        assertEquals(DiaryEntryDto(date = "2026-10-13", feeling = "VERY_GOOD"), midnight.entry())
    }

    @Test
    fun `today is 204 before the day's answer and 200 with it after`() = testApplication {
        installApp()
        val token = register(registration()).sessionToken()

        assertEquals(HttpStatusCode.NoContent, today(token).status)
        answer(token, feeling("GOOD"))

        val after = today(token)
        assertEquals(HttpStatusCode.OK, after.status)
        assertEquals(DiaryEntryDto(date = "2026-10-12", feeling = "GOOD"), after.entry())

        // A new Swiss day has no answer yet.
        clock.now = Instant.parse("2026-10-12T22:00:00Z")
        assertEquals(HttpStatusCode.NoContent, today(token).status)
    }

    @Test
    fun `another user's answer never shows in today`() = testApplication {
        installApp()
        val tim = register(registration(email = "tim@example.com")).sessionToken()
        val anna = register(registration(email = "anna@example.com")).sessionToken()

        answer(tim, feeling("GOOD"))

        assertEquals(HttpStatusCode.NoContent, today(anna).status)
        // Anna's own first answer is still the first of her day.
        assertEquals(HttpStatusCode.Created, answer(anna, feeling("BAD")).status)
    }

    @Test
    fun `an unknown or malformed feeling is 400 with an error and stores nothing`() = testApplication {
        installApp()
        val token = register(registration()).sessionToken()

        listOf(
            feeling("MEH"),
            feeling("good"),
            """{"feeling":3}""",
            """{}""",
            "feeling=GOOD",
        ).forEach { body ->
            val response = answer(token, body)
            assertEquals(HttpStatusCode.BadRequest, response.status, body)
            assertTrue(response.errorText().isNotBlank(), body)
        }
        assertEquals(0L, transaction(database) { DiaryEntriesTable.selectAll().count() })
    }

    @Test
    fun `the diary without a valid session is 401 before the body is read`() = testApplication {
        installApp()

        listOf(null, newSessionToken().value, "too-short").forEach { token ->
            val malformed = answer(token, "not json at all")
            assertEquals(HttpStatusCode.Unauthorized, malformed.status, "$token")
            assertTrue(malformed.headers[HttpHeaders.WWWAuthenticate].orEmpty().startsWith("Bearer"))
            assertEquals(HttpStatusCode.Unauthorized, answer(token, feeling("GOOD")).status, "$token")
            assertEquals(HttpStatusCode.Unauthorized, today(token).status, "$token")
        }
        assertEquals(0L, transaction(database) { DiaryEntriesTable.selectAll().count() })
    }
}
