package ch.stenzel.tim.polleninfo.core.session.data.repository

import ch.stenzel.tim.polleninfo.core.network.createHttpClient
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.session.data.local.SessionStorage
import ch.stenzel.tim.polleninfo.core.session.data.remote.AccountApiService
import ch.stenzel.tim.polleninfo.core.session.domain.model.EmailTakenException
import ch.stenzel.tim.polleninfo.core.session.domain.model.InvalidCredentialsException
import ch.stenzel.tim.polleninfo.core.session.domain.model.SignedOutException
import ch.stenzel.tim.polleninfo.core.session.domain.model.Session
import ch.stenzel.tim.polleninfo.core.session.domain.model.TooManyAttemptsException
import ch.stenzel.tim.polleninfo.core.session.domain.model.toSessionAppError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Through a real Ktor client backed by [MockEngine], so the JSON contract with the backend's account
 * routes is under test alongside what the repository stores.
 */
class SessionRepositoryImplTest {

    private val jsonHeaders = headersOf("Content-Type" to listOf("application/json"))
    private val storage = FakeSessionStorage()
    private val recorded = mutableListOf<HttpRequestData>()

    private fun repository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): SessionRepositoryImpl {
        val engine = MockEngine { request ->
            recorded += request
            handler(request)
        }
        return SessionRepositoryImpl(AccountApiService(createHttpClient(engine), "http://backend.test:8080"), storage)
    }

    private val issuesToken: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = {
        respond("""{"sessionToken":"session-token-1"}""", HttpStatusCode.Created, jsonHeaders)
    }

    @Test
    fun `register posts the email and password with consent true`() = runTest {
        val repository = repository(issuesToken)

        repository.register("tim@example.com", "correct horse")

        val request = recorded.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("http://backend.test:8080/users", request.url.toString())
        val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
        assertEquals(JsonPrimitive("tim@example.com"), body["email"])
        assertEquals(JsonPrimitive("correct horse"), body["password"])
        assertEquals(JsonPrimitive(true), body["consent"])
    }

    @Test
    fun `register stores the issued token with the email as the backend keeps it`() = runTest {
        val repository = repository(issuesToken)

        val result = repository.register("  Tim@Example.com ", "correct horse")

        assertIs<Result.Success<Unit>>(result)
        assertEquals(Session(token = "session-token-1", email = "tim@example.com"), repository.session.first())
        // The address is sent trimmed; the backend lowercases it itself.
        val body = Json.parseToJsonElement((recorded.single().body as TextContent).text).jsonObject
        assertEquals(JsonPrimitive("Tim@Example.com"), body["email"])
    }

    @Test
    fun `the password is sent exactly as typed`() = runTest {
        val repository = repository(issuesToken)

        repository.register("tim@example.com", "  spaced  ")

        val body = Json.parseToJsonElement((recorded.single().body as TextContent).text).jsonObject
        assertEquals(JsonPrimitive("  spaced  "), body["password"])
    }

    @Test
    fun `a 409 is EmailTaken and stores nothing`() = runTest {
        val repository = repository { respond("""{"error":"taken"}""", HttpStatusCode.Conflict, jsonHeaders) }

        val result = repository.register("tim@example.com", "correct horse")

        val failure = assertIs<Result.Failure>(result)
        assertIs<EmailTakenException>(failure.exception)
        assertEquals(AppError.EmailTaken, failure.exception.toSessionAppError())
        assertNull(repository.session.first())
    }

    @Test
    fun `a transport failure is Network and stores nothing`() = runTest {
        val repository = repository { throw IOException("no route to host") }

        val result = repository.register("tim@example.com", "correct horse")

        val failure = assertIs<Result.Failure>(result)
        assertEquals(AppError.Network, failure.exception.toSessionAppError())
        assertNull(repository.session.first())
    }

    @Test
    fun `a server error is ServerUnavailable and stores nothing`() = runTest {
        val repository = repository { respondError(HttpStatusCode.BadGateway) }

        val result = repository.register("tim@example.com", "correct horse")

        assertEquals(AppError.ServerUnavailable, assertIs<Result.Failure>(result).exception.toSessionAppError())
        assertFalse(storage.everStored)
    }

    // --- Login ---

    @Test
    fun `login posts the trimmed email and the password as typed`() = runTest {
        val repository = repository(issuesToken)

        repository.login(" Tim@Example.com ", "  correct horse")

        val request = recorded.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("http://backend.test:8080/users/session", request.url.toString())
        val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
        assertEquals(JsonPrimitive("Tim@Example.com"), body["email"])
        assertEquals(JsonPrimitive("  correct horse"), body["password"])
        assertNull(body["consent"])
    }

    @Test
    fun `login stores the issued token with the email as the backend keeps it`() = runTest {
        val repository = repository(issuesToken)

        val result = repository.login("Tim@Example.com", "correct horse")

        assertIs<Result.Success<Unit>>(result)
        assertEquals(Session(token = "session-token-1", email = "tim@example.com"), repository.session.first())
    }

    @Test
    fun `a 401 on login is InvalidCredentials and stores nothing`() = runTest {
        val repository = repository {
            respond("""{"error":"Email or password is incorrect"}""", HttpStatusCode.Unauthorized, jsonHeaders)
        }

        val result = repository.login("tim@example.com", "wrong horse")

        val failure = assertIs<Result.Failure>(result)
        assertIs<InvalidCredentialsException>(failure.exception)
        assertEquals(AppError.InvalidCredentials, failure.exception.toSessionAppError())
        assertFalse(storage.everStored)
    }

    private val rateLimited: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = {
        respond(
            """{"error":"Too many requests"}""",
            HttpStatusCode.TooManyRequests,
            headersOf(HttpHeaders.ContentType to listOf("application/json"), HttpHeaders.RetryAfter to listOf("360")),
        )
    }

    @Test
    fun `a 429 on register is TooManyAttempts and stores nothing`() = runTest {
        val repository = repository(rateLimited)

        val result = repository.register("tim@example.com", "correct horse")

        val failure = assertIs<Result.Failure>(result)
        assertIs<TooManyAttemptsException>(failure.exception)
        assertEquals(AppError.TooManyAttempts, failure.exception.toSessionAppError())
        assertFalse(storage.everStored)
    }

    @Test
    fun `a 429 on login is TooManyAttempts and stores nothing`() = runTest {
        val repository = repository(rateLimited)

        val result = repository.login("tim@example.com", "correct horse")

        val failure = assertIs<Result.Failure>(result)
        assertIs<TooManyAttemptsException>(failure.exception)
        assertEquals(AppError.TooManyAttempts, failure.exception.toSessionAppError())
        assertFalse(storage.everStored)
    }

    @Test
    fun `a 429 on deleteAccount is TooManyAttempts and keeps the session`() = runTest {
        storage.store(signedIn)
        val repository = repository(rateLimited)

        val result = repository.deleteAccount("correct horse")

        val failure = assertIs<Result.Failure>(result)
        assertIs<TooManyAttemptsException>(failure.exception)
        assertEquals(AppError.TooManyAttempts, failure.exception.toSessionAppError())
        assertEquals(signedIn, repository.session.first())
    }

    @Test
    fun `a transport failure on login is Network`() = runTest {
        val repository = repository { throw IOException("no route to host") }

        val result = repository.login("tim@example.com", "correct horse")

        assertEquals(AppError.Network, assertIs<Result.Failure>(result).exception.toSessionAppError())
        assertNull(repository.session.first())
    }

    // --- Logout ---

    @Test
    fun `logout sends a DELETE with the bearer token and clears the session on 204`() = runTest {
        storage.store(Session(token = "session-token-1", email = "tim@example.com"))
        val repository = repository { respond("", HttpStatusCode.NoContent) }

        val result = repository.logout()

        assertIs<Result.Success<Unit>>(result)
        val request = recorded.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("http://backend.test:8080/users/me/session", request.url.toString())
        assertEquals("Bearer session-token-1", request.headers[HttpHeaders.Authorization])
        assertNull(repository.session.first())
    }

    @Test
    fun `logout clears the session even when the request fails`() = runTest {
        val failures = listOf<suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData>(
            { throw IOException("offline") },
            { respondError(HttpStatusCode.BadGateway) },
            { respond("", HttpStatusCode.Unauthorized) },
        )
        failures.forEach { failure ->
            storage.store(Session(token = "session-token-1", email = "tim@example.com"))
            val repository = repository(failure)

            assertIs<Result.Success<Unit>>(repository.logout())
            assertNull(repository.session.first())
        }
    }

    @Test
    fun `logout without a session sends nothing`() = runTest {
        val repository = repository(issuesToken)

        assertIs<Result.Success<Unit>>(repository.logout())
        assertEquals(emptyList(), recorded)
    }

    @Test
    fun `a 401 on an authenticated call is SignedOut`() = runTest {
        val api = AccountApiService(
            createHttpClient(MockEngine { respond("", HttpStatusCode.Unauthorized) }),
            "http://backend.test:8080",
        )

        val failure = assertFailsWith<SignedOutException> { api.logout("session-token-1") }

        assertEquals(AppError.SignedOut, failure.toSessionAppError())
    }

    // --- Deleting the account ---

    private val signedIn = Session(token = "session-token-1", email = "tim@example.com")

    @Test
    fun `deleteAccount sends a DELETE with the bearer token and the password and clears the session on 204`() = runTest {
        storage.store(signedIn)
        val repository = repository { respond("", HttpStatusCode.NoContent) }

        val result = repository.deleteAccount("  correct horse")

        assertIs<Result.Success<Unit>>(result)
        val request = recorded.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("http://backend.test:8080/users/me", request.url.toString())
        assertEquals("Bearer session-token-1", request.headers[HttpHeaders.Authorization])
        val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
        assertEquals(JsonPrimitive("  correct horse"), body["password"])
        assertNull(repository.session.first())
    }

    @Test
    fun `a 403 on deleteAccount is InvalidCredentials and keeps the session`() = runTest {
        storage.store(signedIn)
        val repository = repository {
            respond("""{"error":"Password is incorrect"}""", HttpStatusCode.Forbidden, jsonHeaders)
        }

        val result = repository.deleteAccount("wrong horse")

        val failure = assertIs<Result.Failure>(result)
        assertIs<InvalidCredentialsException>(failure.exception)
        assertEquals(AppError.InvalidCredentials, failure.exception.toSessionAppError())
        assertEquals(signedIn, repository.session.first())
    }

    @Test
    fun `a failed deleteAccount keeps the session`() = runTest {
        val failures = listOf<suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData>(
            { throw IOException("offline") },
            { respondError(HttpStatusCode.BadGateway) },
        )
        failures.zip(listOf(AppError.Network, AppError.ServerUnavailable)).forEach { (failure, error) ->
            storage.store(signedIn)
            val repository = repository(failure)

            val result = repository.deleteAccount("correct horse")

            assertEquals(error, assertIs<Result.Failure>(result).exception.toSessionAppError())
            assertEquals(signedIn, repository.session.first())
        }
    }

    @Test
    fun `a 401 on deleteAccount is SignedOut and clears the session`() = runTest {
        storage.store(signedIn)
        val repository = repository { respond("", HttpStatusCode.Unauthorized) }

        val result = repository.deleteAccount("correct horse")

        assertIs<SignedOutException>(assertIs<Result.Failure>(result).exception)
        assertNull(repository.session.first())
    }

    @Test
    fun `deleteAccount keeps a newer login that replaced the session meanwhile`() = runTest {
        storage.store(signedIn)
        val newer = Session(token = "session-token-2", email = "anna@example.com")
        val repository = repository {
            storage.store(newer)
            respond("", HttpStatusCode.NoContent)
        }

        assertIs<Result.Success<Unit>>(repository.deleteAccount("correct horse"))
        assertEquals(newer, repository.session.first())
    }

    @Test
    fun `deleteAccount without a session is SignedOut and sends nothing`() = runTest {
        val repository = repository { respond("", HttpStatusCode.NoContent) }

        val result = repository.deleteAccount("correct horse")

        assertIs<SignedOutException>(assertIs<Result.Failure>(result).exception)
        assertEquals(emptyList(), recorded)
    }

    @Test
    fun `clear forgets the stored session without a request`() = runTest {
        storage.store(Session(token = "session-token-1", email = "tim@example.com"))
        val repository = repository(issuesToken)

        repository.clear()

        assertNull(repository.session.first())
        assertEquals(emptyList(), recorded)
    }
}

/** In-memory [SessionStorage]. */
private class FakeSessionStorage : SessionStorage {
    private val stored = MutableStateFlow<Session?>(null)
    override val session: Flow<Session?> = stored

    var everStored = false
        private set

    override suspend fun store(session: Session) {
        everStored = true
        stored.value = session
    }

    override suspend fun clear() {
        stored.value = null
    }
}
