package ch.stenzel.tim.polleninfo.core.session.data.repository

import ch.stenzel.tim.polleninfo.core.network.createHttpClient
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.session.data.local.SessionStorage
import ch.stenzel.tim.polleninfo.core.session.data.remote.AccountApiService
import ch.stenzel.tim.polleninfo.core.session.domain.model.EmailTakenException
import ch.stenzel.tim.polleninfo.core.session.domain.model.Session
import ch.stenzel.tim.polleninfo.core.session.domain.model.toSessionAppError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
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
