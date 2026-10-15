package ch.stenzel.tim.polleninfo.server.account

import ch.stenzel.tim.polleninfo.server.account.domain.SESSION_TOKEN_LENGTH
import ch.stenzel.tim.polleninfo.server.account.domain.SessionToken
import ch.stenzel.tim.polleninfo.server.account.domain.cheapHasher
import ch.stenzel.tim.polleninfo.server.account.domain.hash
import ch.stenzel.tim.polleninfo.server.account.model.SessionDto
import ch.stenzel.tim.polleninfo.server.account.store.ExposedSessionStore
import ch.stenzel.tim.polleninfo.server.account.store.ExposedUserStore
import ch.stenzel.tim.polleninfo.server.account.store.SessionsTable
import ch.stenzel.tim.polleninfo.server.account.store.UsersTable
import ch.stenzel.tim.polleninfo.server.alarm.store.TestPostgres
import ch.stenzel.tim.polleninfo.server.config.RateLimits
import ch.stenzel.tim.polleninfo.server.plugins.configureAccountRouting
import ch.stenzel.tim.polleninfo.server.plugins.configureSecurity
import ch.stenzel.tim.polleninfo.server.plugins.configureSerialization
import ch.stenzel.tim.polleninfo.server.pollen.measurement.MutableClock
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    private val hasher = cheapHasher()
    private val clock = MutableClock(Instant.parse("2026-10-12T08:30:00Z"))

    private fun ApplicationTestBuilder.installApp() {
        application {
            configureSerialization()
            // Generous, so the route tests never meet the account limiter; SecurityTest pins it.
            configureSecurity(RateLimits(accountPerHour = 1_000))
            configureAccountRouting(users = users, sessions = sessions, hasher = hasher, clock = clock)
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
}
