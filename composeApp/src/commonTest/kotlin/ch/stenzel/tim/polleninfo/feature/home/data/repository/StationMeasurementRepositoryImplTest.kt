package ch.stenzel.tim.polleninfo.feature.home.data.repository

import ch.stenzel.tim.polleninfo.core.network.createHttpClient
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.home.data.remote.StationMeasurementApiService
import ch.stenzel.tim.polleninfo.feature.home.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.feature.home.domain.model.StationMeasurement
import ch.stenzel.tim.polleninfo.feature.home.measurementJson
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
import kotlin.test.assertNull

/**
 * Exercises the repository through a real Ktor client backed by [MockEngine], so the JSON contract
 * with **our own backend** and the production serialization config are under test alongside the
 * mapping — that is where a change to the server's DTO would actually bite.
 */
class StationMeasurementRepositoryImplTest {

    private val jsonHeaders = headersOf("Content-Type" to listOf("application/json"))

    private val recorded = mutableListOf<HttpRequestData>()

    private fun repository(
        baseUrl: String = "http://backend.test:8080",
        handler: MockRequestHandler,
    ): StationMeasurementRepositoryImpl {
        val engine = MockEngine { request ->
            recorded += request
            handler(request)
        }
        return StationMeasurementRepositoryImpl(
            StationMeasurementApiService(createHttpClient(engine), baseUrl),
        )
    }

    @Test
    fun `returns Success with all seven taxa`() = runTest {
        val repository = repository { respond(measurementJson, HttpStatusCode.OK, jsonHeaders) }

        val result = repository.getMeasurement("PZH")

        assertIs<Result.Success<StationMeasurement>>(result)
        assertEquals("PZH", result.data.stationAbbr)
        assertEquals("grains/m3", result.data.unit)
        assertEquals(7, result.data.species.size)
    }

    @Test
    fun `maps severities and distinguishes an unmeasured taxon from a measured zero`() = runTest {
        val repository = repository { respond(measurementJson, HttpStatusCode.OK, jsonHeaders) }

        val result = repository.getMeasurement("PZH")

        val bySpecies = assertIs<Result.Success<StationMeasurement>>(result)
            .data.species.associateBy { it.id }
        assertEquals(PollenSeverity.VERY_HIGH, bySpecies.getValue("OAK").severity)
        assertEquals(PollenSeverity.NONE, bySpecies.getValue("ALDER").severity)
        assertEquals(0, bySpecies.getValue("ALDER").concentration)
        assertNull(bySpecies.getValue("ASH").severity)
        assertNull(bySpecies.getValue("ASH").concentration)
    }

    @Test
    fun `requests the measurements endpoint for the station on the base url it was given`() =
        runTest {
            val repository = repository(baseUrl = "http://10.0.2.2:8080") {
                respond(measurementJson, HttpStatusCode.OK, jsonHeaders)
            }

            repository.getMeasurement("PLU")

            assertEquals(
                "http://10.0.2.2:8080/pollen/stations/PLU/measurements",
                recorded.single().url.toString(),
            )
        }

    @Test
    fun `tolerates unknown fields so a backend addition does not break the app`() = runTest {
        val body = """
            {
              "stationAbbr": "PZH",
              "measuredAt": "2026-08-01T09:00:00Z",
              "unit": "grains/m3",
              "source": "meteoswiss-ogd",
              "species": [
                { "id": "BIRCH", "name": "Birch", "latinName": "Betula",
                  "concentration": 42, "severity": "MODERATE", "trend": "rising" }
              ]
            }
        """.trimIndent()
        val repository = repository { respond(body, HttpStatusCode.OK, jsonHeaders) }

        val result = repository.getMeasurement("PZH")

        assertIs<Result.Success<StationMeasurement>>(result)
        assertEquals(PollenSeverity.MODERATE, result.data.species.single().severity)
    }

    @Test
    fun `returns Failure when the station is not found`() = runTest {
        val repository = repository { respondError(HttpStatusCode.NotFound) }

        assertIs<Result.Failure>(repository.getMeasurement("XXX"))
    }

    @Test
    fun `returns Failure when the backend responds with a server error`() = runTest {
        val repository = repository { respondError(HttpStatusCode.InternalServerError) }

        assertIs<Result.Failure>(repository.getMeasurement("PZH"))
    }

    @Test
    fun `returns Failure when the backend cannot reach its upstream`() = runTest {
        val repository = repository { respondError(HttpStatusCode.BadGateway) }

        assertIs<Result.Failure>(repository.getMeasurement("PZH"))
    }

    @Test
    fun `returns Failure when the backend responds with malformed json`() = runTest {
        val repository = repository { respond("{ not json", HttpStatusCode.OK, jsonHeaders) }

        assertIs<Result.Failure>(repository.getMeasurement("PZH"))
    }

    @Test
    fun `returns Failure when the timestamp is absent from the response`() = runTest {
        // `measuredAt` is mandatory on the wire: a reading that can be served from a cache has to
        // say when it is from, so a response without it is not a usable reading.
        val body = """{"stationAbbr":"PZH","unit":"grains/m3","species":[]}"""
        val repository = repository { respond(body, HttpStatusCode.OK, jsonHeaders) }

        assertIs<Result.Failure>(repository.getMeasurement("PZH"))
    }

    @Test
    fun `returns Failure when the timestamp is not an ISO-8601 instant`() = runTest {
        val body = measurementJson.replace("2026-08-01T09:00:00Z", "yesterday")
        val repository = repository { respond(body, HttpStatusCode.OK, jsonHeaders) }

        assertIs<Result.Failure>(repository.getMeasurement("PZH"))
    }

    @Test
    fun `returns Failure when the backend sends a severity the app does not know`() = runTest {
        val body = """
            {
              "stationAbbr": "PZH", "measuredAt": "2026-08-01T09:00:00Z", "unit": "grains/m3",
              "species": [ { "id": "BIRCH", "name": "Birch", "latinName": "Betula",
                             "concentration": 42, "severity": "CATASTROPHIC" } ]
            }
        """.trimIndent()
        val repository = repository { respond(body, HttpStatusCode.OK, jsonHeaders) }

        assertIs<Result.Failure>(repository.getMeasurement("PZH"))
    }

    @Test
    fun `returns Failure instead of throwing when the backend is unreachable`() = runTest {
        val repository = repository { throw RuntimeException("Connection refused") }

        assertIs<Result.Failure>(repository.getMeasurement("PZH"))
    }
}
