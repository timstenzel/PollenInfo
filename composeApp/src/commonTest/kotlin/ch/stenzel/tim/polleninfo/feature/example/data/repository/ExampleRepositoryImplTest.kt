package ch.stenzel.tim.polleninfo.feature.example.data.repository

import ch.stenzel.tim.polleninfo.core.network.createHttpClient
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.example.data.remote.ExampleApiService
import ch.stenzel.tim.polleninfo.feature.example.domain.model.PollenSnapshot
import ch.stenzel.tim.polleninfo.feature.example.domain.model.PollenLevel
import ch.stenzel.tim.polleninfo.feature.example.domain.model.PollenType
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Exercises the repository through a real Ktor client backed by [MockEngine], so the JSON
 * contract and the client's serialization config are covered alongside the domain mapping.
 */
class ExampleRepositoryImplTest {

    private val jsonHeaders = headersOf("Content-Type" to listOf("application/json"))

    private val recorded = mutableListOf<HttpRequestData>()

    private fun repository(handler: MockRequestHandler): ExampleRepositoryImpl {
        val engine = MockEngine { request ->
            recorded += request
            handler(request)
        }
        return ExampleRepositoryImpl(ExampleApiService(createHttpClient(engine)))
    }

    @Test
    fun `returns Success with mapped domain data for a well formed response`() = runTest {
        val body = """
            {
              "latitude": 47.375,
              "longitude": 8.5417,
              "timezone": "Europe/Zurich",
              "hourly": {
                "time": ["2026-07-31T00:00", "2026-07-31T01:00"],
                "birch": [12.0, 0.0],
                "grass": [250.0, 4.0],
                "mugwort": [null, null],
                "alder": [0.0, 0.0],
                "olive": [0.0, 0.0],
                "ragweed": [0.0, 0.0]
              }
            }
        """.trimIndent()
        val repository = repository { respond(body, HttpStatusCode.OK, jsonHeaders) }

        val result = repository.getPollenSnapshot(47.3769, 8.5417)

        assertIs<Result.Success<PollenSnapshot>>(result)
        val snapshot = result.data
        assertEquals(2, snapshot.hourlyReadings.size)
        assertEquals(
            PollenLevel.MODERATE,
            snapshot.currentReadings.single { it.type == PollenType.BIRCH }.level,
        )
        assertEquals(
            PollenLevel.VERY_HIGH,
            snapshot.currentReadings.single { it.type == PollenType.GRASS }.level,
        )
        assertEquals(
            PollenLevel.NONE,
            snapshot.currentReadings.single { it.type == PollenType.MUGWORT }.level,
        )
    }

    @Test
    fun `tolerates unknown fields so upstream additions do not break the app`() = runTest {
        val body = """
            {
              "latitude": 47.0,
              "longitude": 8.0,
              "timezone": "Europe/Zurich",
              "elevation": 500.0,
              "units": {"birch": "grains/m³"},
              "hourly": {"time": ["2026-07-31T00:00"], "birch": [5.0]}
            }
        """.trimIndent()
        val repository = repository { respond(body, HttpStatusCode.OK, jsonHeaders) }

        val result = repository.getPollenSnapshot(47.0, 8.0)

        assertIs<Result.Success<PollenSnapshot>>(result)
        assertEquals(
            PollenLevel.LOW,
            result.data.currentReadings.single { it.type == PollenType.BIRCH }.level,
        )
    }

    @Test
    fun `forwards the requested coordinates and the pollen query parameters`() = runTest {
        val body = """{"latitude":47.0,"longitude":8.0,"timezone":"Europe/Zurich","hourly":{"time":[]}}"""
        val repository = repository { respond(body, HttpStatusCode.OK, jsonHeaders) }

        repository.getPollenSnapshot(46.9503, 7.4447)

        val url = recorded.single().url
        assertEquals("46.9503", url.parameters["latitude"])
        assertEquals("7.4447", url.parameters["longitude"])
        assertEquals("auto", url.parameters["timezone"])
        assertEquals("72", url.parameters["hours"])
        val species = url.parameters["species"].orEmpty()
        PollenType.entries.forEach { type ->
            assertTrue(
                species.contains(type.name.lowercase()),
                "species parameter should request ${type.name}, was: $species",
            )
        }
    }

    @Test
    fun `returns Failure when the upstream responds with a server error`() = runTest {
        val repository = repository { respondError(HttpStatusCode.InternalServerError) }

        assertIs<Result.Failure>(repository.getPollenSnapshot(47.3769, 8.5417))
    }

    @Test
    fun `returns Failure when the upstream responds with malformed json`() = runTest {
        val repository = repository { respond("{ not json", HttpStatusCode.OK, jsonHeaders) }

        assertIs<Result.Failure>(repository.getPollenSnapshot(47.3769, 8.5417))
    }

    @Test
    fun `returns Failure when a required field is absent from the response`() = runTest {
        val repository = repository {
            respond("""{"latitude":47.0,"longitude":8.0}""", HttpStatusCode.OK, jsonHeaders)
        }

        assertIs<Result.Failure>(repository.getPollenSnapshot(47.3769, 8.5417))
    }

    @Test
    fun `returns Failure instead of throwing when the transport fails`() = runTest {
        val repository = repository { throw RuntimeException("offline") }

        assertIs<Result.Failure>(repository.getPollenSnapshot(47.3769, 8.5417))
    }
}
