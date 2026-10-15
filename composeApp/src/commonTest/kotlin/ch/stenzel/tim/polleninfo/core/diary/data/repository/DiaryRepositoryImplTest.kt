package ch.stenzel.tim.polleninfo.core.diary.data.repository

import ch.stenzel.tim.polleninfo.core.diary.data.remote.DiaryApiService
import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.network.createHttpClient
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.session.FakeSessionRepository
import ch.stenzel.tim.polleninfo.core.session.domain.model.Session
import ch.stenzel.tim.polleninfo.core.session.domain.model.SignedOutException
import ch.stenzel.tim.polleninfo.core.session.domain.model.toSessionAppError
import ch.stenzel.tim.polleninfo.core.session.testSession
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Through a real Ktor client backed by [MockEngine], so the JSON contract with the backend's diary
 * routes is under test alongside the session handling.
 */
class DiaryRepositoryImplTest {

    private val jsonHeaders = headersOf("Content-Type" to listOf("application/json"))
    private val sessions = FakeSessionRepository(initial = testSession)
    private val recorded = mutableListOf<HttpRequestData>()

    private fun repository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): DiaryRepositoryImpl {
        val engine = MockEngine { request ->
            recorded += request
            handler(request)
        }
        return DiaryRepositoryImpl(DiaryApiService(createHttpClient(engine), "http://backend.test:8080"), sessions)
    }

    private fun MockRequestHandleScope.entry(status: HttpStatusCode, feeling: String = "GOOD") =
        respond("""{"date":"2026-10-15","feeling":"$feeling"}""", status, jsonHeaders)

    private fun <T> Result<T>.failure(): Exception = assertIs<Result.Failure>(this).exception

    @Test
    fun `record posts the feeling with the session's bearer token`() = runTest {
        val repository = repository { entry(HttpStatusCode.Created, feeling = "VERY_BAD") }

        repository.record(Feeling.VERY_BAD)

        val request = recorded.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("http://backend.test:8080/users/me/diary", request.url.toString())
        assertEquals("Bearer ${testSession.token}", request.headers[HttpHeaders.Authorization])
        val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
        assertEquals(setOf("feeling"), body.keys)
        assertEquals(JsonPrimitive("VERY_BAD"), body["feeling"])
    }

    @Test
    fun `record maps a 201 to the stored answer`() = runTest {
        val repository = repository { entry(HttpStatusCode.Created) }

        assertEquals(
            Result.Success(DiaryEntry(LocalDate(2026, 10, 15), Feeling.GOOD)),
            repository.record(Feeling.GOOD),
        )
    }

    @Test
    fun `record maps a 200 to the first answer the account kept`() = runTest {
        // Today was answered already; the backend returns that answer, not the one just sent.
        val repository = repository { entry(HttpStatusCode.OK, feeling = "GOOD") }

        assertEquals(
            Result.Success(DiaryEntry(LocalDate(2026, 10, 15), Feeling.GOOD)),
            repository.record(Feeling.VERY_BAD),
        )
    }

    @Test
    fun `today maps a 200 to today's answer`() = runTest {
        val repository = repository { entry(HttpStatusCode.OK, feeling = "BAD") }

        assertEquals(Result.Success(DiaryEntry(LocalDate(2026, 10, 15), Feeling.BAD)), repository.today())
        val request = recorded.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("http://backend.test:8080/users/me/diary/today", request.url.toString())
        assertEquals("Bearer ${testSession.token}", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun `today maps a 204 to no answer`() = runTest {
        val repository = repository { respond("", HttpStatusCode.NoContent) }

        assertEquals(Result.Success(null), repository.today())
    }

    @Test
    fun `without a session nothing is sent and both calls fail as signed out`() = runTest {
        sessions.current = null
        val repository = repository { entry(HttpStatusCode.Created) }

        assertIs<SignedOutException>(repository.record(Feeling.GOOD).failure())
        assertIs<SignedOutException>(repository.today().failure())
        assertEquals(emptyList(), recorded)
    }

    @Test
    fun `a 401 clears the session and fails as SignedOut`() = runTest {
        val repository = repository { respondError(HttpStatusCode.Unauthorized) }

        val failure = repository.record(Feeling.GOOD).failure()

        assertIs<SignedOutException>(failure)
        assertEquals(AppError.SignedOut, failure.toSessionAppError())
        assertNull(sessions.current)
    }

    @Test
    fun `a 401 on today clears the session too`() = runTest {
        val repository = repository { respondError(HttpStatusCode.Unauthorized) }

        assertIs<SignedOutException>(repository.today().failure())
        assertNull(sessions.current)
    }

    @Test
    fun `a 401 for a session already replaced keeps the new one`() = runTest {
        val newer = Session(token = "session-token-2", email = "tim@example.com")
        val repository = repository {
            // Logged in again on another screen while this request was under way.
            sessions.current = newer
            respondError(HttpStatusCode.Unauthorized)
        }

        assertIs<SignedOutException>(repository.today().failure())
        assertEquals(newer, sessions.current)
    }

    @Test
    fun `a transport failure is a Network error and keeps the session`() = runTest {
        val repository = repository { throw IOException("connection refused") }

        assertEquals(AppError.Network, repository.record(Feeling.GOOD).failure().toSessionAppError())
        assertEquals(testSession, sessions.current)
    }

    @Test
    fun `a server error is ServerUnavailable and keeps the session`() = runTest {
        val repository = repository { respondError(HttpStatusCode.BadGateway) }

        assertEquals(AppError.ServerUnavailable, repository.today().failure().toSessionAppError())
        assertEquals(testSession, sessions.current)
    }

    @Test
    fun `an answer the app does not understand is an Unknown failure`() = runTest {
        val repository = repository { entry(HttpStatusCode.OK, feeling = "MEH") }

        assertEquals(AppError.Unknown, repository.today().failure().toSessionAppError())
    }
}
