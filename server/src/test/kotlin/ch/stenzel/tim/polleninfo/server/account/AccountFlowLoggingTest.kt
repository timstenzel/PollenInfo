package ch.stenzel.tim.polleninfo.server.account

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.read.ListAppender
import ch.stenzel.tim.polleninfo.server.account.domain.cheapHasher
import ch.stenzel.tim.polleninfo.server.account.model.SessionDto
import ch.stenzel.tim.polleninfo.server.account.store.ExposedSessionStore
import ch.stenzel.tim.polleninfo.server.account.store.ExposedUserStore
import ch.stenzel.tim.polleninfo.server.alarm.store.TestPostgres
import ch.stenzel.tim.polleninfo.server.plugins.configureAccountRouting
import ch.stenzel.tim.polleninfo.server.plugins.configureLogging
import ch.stenzel.tim.polleninfo.server.plugins.configureSecurity
import ch.stenzel.tim.polleninfo.server.plugins.configureSerialization
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.time.Clock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * What a whole account flow writes to the log, at the levels the server is configured with: neither
 * the email, the password nor the session token may appear, so log access reveals nothing about a
 * user and grants access to no account. Each account endpoint joins the flow as it is added.
 */
class AccountFlowLoggingTest {

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

    private val email = "logged.flow@example.com"
    private val password = "flow-password-7f3k"

    @Test
    fun `a full account flow logs neither the email nor the password nor the session token`() = testApplication {
        application {
            configureSerialization()
            configureSecurity()
            configureLogging()
            configureAccountRouting(
                users = ExposedUserStore(database),
                sessions = ExposedSessionStore(database),
                hasher = cheapHasher(),
                clock = Clock.systemUTC(),
            )
        }

        val registered = client.post("/users") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password","consent":true}""")
        }
        assertEquals(HttpStatusCode.Created, registered.status)
        val token = Json.decodeFromString<SessionDto>(registered.bodyAsText()).sessionToken
        // A refused second registration exercises the error path too.
        val taken = client.post("/users") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password","consent":true}""")
        }
        assertEquals(HttpStatusCode.Conflict, taken.status)
        // A failed and a successful login, then logging both sessions out.
        val refused = client.post("/users/session") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"wrong-$password"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, refused.status)
        val loggedIn = client.post("/users/session") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password"}""")
        }
        assertEquals(HttpStatusCode.Created, loggedIn.status)
        val loginToken = Json.decodeFromString<SessionDto>(loggedIn.bodyAsText()).sessionToken
        listOf(token, loginToken).forEach {
            assertEquals(HttpStatusCode.NoContent, client.delete("/users/me/session") { bearerAuth(it) }.status)
        }
        // A logged-out token is refused, on the error path once more.
        assertEquals(HttpStatusCode.Unauthorized, client.delete("/users/me/session") { bearerAuth(token) }.status)

        val logged = appender.list.map { event ->
            event.formattedMessage + event.throwableProxy?.let(ThrowableProxyUtil::asString).orEmpty()
        }
        // Call logging wrote a line per request, so the capture itself works.
        assertTrue(logged.any { "POST /users" in it }, "captured: $logged")
        assertTrue(logged.any { "POST /users/session" in it } && logged.any { "DELETE /users/me/session" in it })
        listOf(token, loginToken, email, password, "logged.flow").forEach { secret ->
            assertTrue(logged.none { secret in it }, "a log line contains $secret: $logged")
        }
    }
}
