package ch.stenzel.tim.polleninfo.core.station.data.repository

import ch.stenzel.tim.polleninfo.core.network.HttpStatusException
import ch.stenzel.tim.polleninfo.core.network.createHttpClient
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.station.data.remote.StationApiService
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.station.expectedStationNamesAlphabetical
import ch.stenzel.tim.polleninfo.core.station.stationDtosInServerOrder
import ch.stenzel.tim.polleninfo.core.station.stationsJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

/**
 * Exercises the repository through a real Ktor client backed by [MockEngine], so the JSON contract
 * with **our own backend** and the client's serialization config are under test alongside the
 * mapping — that is where a change to the server's `StationDto` would actually bite.
 */
class StationRepositoryImplTest {

    private val jsonHeaders = headersOf("Content-Type" to listOf("application/json"))

    private val recorded = mutableListOf<HttpRequestData>()

    private fun repository(
        baseUrl: String = "http://backend.test:8080",
        handler: MockRequestHandler,
    ): StationRepositoryImpl {
        val engine = MockEngine { request ->
            recorded += request
            handler(request)
        }
        return StationRepositoryImpl(StationApiService(createHttpClient(engine), baseUrl))
    }

    @Test
    fun `returns Success with all 15 stations in alphabetical order`() = runTest {
        val repository = repository { respond(stationsJson, HttpStatusCode.OK, jsonHeaders) }

        val result = repository.getStations()

        assertIs<Result.Success<List<Station>>>(result)
        assertEquals(15, result.data.size)
        assertEquals(expectedStationNamesAlphabetical, result.data.map { it.name })
    }

    @Test
    fun `maps every field of every station`() = runTest {
        val repository = repository { respond(stationsJson, HttpStatusCode.OK, jsonHeaders) }

        val result = repository.getStations()

        val byAbbr = assertIs<Result.Success<List<Station>>>(result).data.associateBy { it.abbr }
        stationDtosInServerOrder.forEach { dto ->
            val station = byAbbr.getValue(dto.abbr)
            assertEquals(dto.name, station.name)
            assertEquals(dto.latitude, station.latitude)
            assertEquals(dto.longitude, station.longitude)
        }
    }

    @Test
    fun `requests the stations endpoint on the base url it was given`() = runTest {
        val repository = repository(baseUrl = "http://10.0.2.2:8080") {
            respond(stationsJson, HttpStatusCode.OK, jsonHeaders)
        }

        repository.getStations()

        assertEquals("http://10.0.2.2:8080/pollen/stations", recorded.single().url.toString())
    }

    @Test
    fun `tolerates unknown fields so a backend addition does not break the app`() = runTest {
        val body = """
            [
              {
                "abbr": "PZH",
                "name": "Zürich",
                "canton": "ZH",
                "latitude": 47.378225,
                "longitude": 8.565644,
                "altitudeMasl": 559,
                "measuringSince": "2023-01-01",
                "species": ["ALDER", "BIRCH"]
              }
            ]
        """.trimIndent()
        val repository = repository { respond(body, HttpStatusCode.OK, jsonHeaders) }

        val result = repository.getStations()

        assertIs<Result.Success<List<Station>>>(result)
        assertEquals("Zürich", result.data.single().name)
    }

    @Test
    fun `returns Failure when the backend responds with a server error`() = runTest {
        val repository = repository { respondError(HttpStatusCode.InternalServerError) }

        assertIs<Result.Failure>(repository.getStations())
    }

    @Test
    fun `a 502 fails with HttpStatusException rather than a deserialization error`() = runTest {
        // A JSON error body, so a service that skipped the status check would try to read it.
        val repository = repository {
            respond("""{"error":"upstream failed"}""", HttpStatusCode.BadGateway, jsonHeaders)
        }

        val failure = assertIs<Result.Failure>(repository.getStations())
        assertEquals(502, assertIs<HttpStatusException>(failure.exception).status)
    }

    @Test
    fun `returns Failure when the backend responds with malformed json`() = runTest {
        val repository = repository { respond("[ not json", HttpStatusCode.OK, jsonHeaders) }

        assertIs<Result.Failure>(repository.getStations())
    }

    @Test
    fun `returns Failure when a required field is absent from the response`() = runTest {
        val repository = repository {
            respond("""[{"abbr":"PZH","canton":"ZH"}]""", HttpStatusCode.OK, jsonHeaders)
        }

        assertIs<Result.Failure>(repository.getStations())
    }

    @Test
    fun `returns Failure instead of throwing when the backend is unreachable`() = runTest {
        val repository = repository { throw RuntimeException("Connection refused") }

        assertIs<Result.Failure>(repository.getStations())
    }
}
