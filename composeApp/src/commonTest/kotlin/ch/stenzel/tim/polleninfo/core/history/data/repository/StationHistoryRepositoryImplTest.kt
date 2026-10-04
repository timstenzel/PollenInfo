package ch.stenzel.tim.polleninfo.core.history.data.repository

import ch.stenzel.tim.polleninfo.core.history.data.remote.StationHistoryApiService
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.core.history.domain.model.StationHistory
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.network.createHttpClient
import ch.stenzel.tim.polleninfo.core.result.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Through a real Ktor client backed by [MockEngine], so the JSON contract with our backend and the
 * production serialization config are under test alongside the mapping.
 */
class StationHistoryRepositoryImplTest {

    private val jsonHeaders = headersOf("Content-Type" to listOf("application/json"))

    private val recorded = mutableListOf<HttpRequestData>()

    private fun repository(
        baseUrl: String = "http://backend.test:8080",
        handler: MockRequestHandler,
    ): StationHistoryRepositoryImpl {
        val engine = MockEngine { request ->
            recorded += request
            handler(request)
        }
        return StationHistoryRepositoryImpl(StationHistoryApiService(createHttpClient(engine), baseUrl))
    }

    private val historyJson = """
        {
          "stationAbbr": "PZH", "range": "week", "from": "2026-09-27", "until": "2026-10-03",
          "days": [
            { "date": "2026-09-27", "species": [
                { "id": "BIRCH", "concentration": 12, "severity": "LOW" },
                { "id": "GRASSES", "concentration": 0, "severity": "NONE" },
                { "id": "ASH", "concentration": null, "severity": null } ] },
            { "date": "2026-09-28", "species": [
                { "id": "BIRCH", "concentration": null, "severity": null },
                { "id": "GRASSES", "concentration": null, "severity": null },
                { "id": "ASH", "concentration": null, "severity": null } ] },
            { "date": "2026-10-03", "species": [
                { "id": "BIRCH", "concentration": 2000, "severity": "VERY_HIGH" },
                { "id": "GRASSES", "concentration": 25, "severity": "HIGH" },
                { "id": "ASH", "concentration": null, "severity": null } ] }
          ]
        }
    """.trimIndent()

    @Test
    fun `requests the history path with the range as a query value`() = runTest {
        val repository = repository(baseUrl = "http://10.0.2.2:8080") {
            respond(historyJson, HttpStatusCode.OK, jsonHeaders)
        }

        repository.history("PLU", HistoryRange.MONTH)

        val url = recorded.single().url
        assertEquals("/pollen/stations/PLU/history", url.encodedPath)
        assertEquals("10.0.2.2", url.host)
        assertEquals(8080, url.port)
        assertEquals("month", url.parameters["range"])
    }

    @Test
    fun `every range is sent as its lowercase wire value`() = runTest {
        val repository = repository { respond(historyJson, HttpStatusCode.OK, jsonHeaders) }

        HistoryRange.entries.forEach { repository.history("PZH", it) }

        assertEquals(listOf("week", "month", "year"), recorded.map { it.url.parameters["range"] })
    }

    @Test
    fun `maps the station dates and every day in order`() = runTest {
        val repository = repository { respond(historyJson, HttpStatusCode.OK, jsonHeaders) }

        val history = assertIs<Result.Success<StationHistory>>(repository.history("PZH", HistoryRange.WEEK)).data

        assertEquals("PZH", history.stationAbbr)
        assertEquals(LocalDate(2026, 9, 27), history.from)
        assertEquals(LocalDate(2026, 10, 3), history.until)
        assertEquals(
            listOf(LocalDate(2026, 9, 27), LocalDate(2026, 9, 28), LocalDate(2026, 10, 3)),
            history.days.map { it.date },
        )
    }

    @Test
    fun `maps severities and keeps a day without values as null levels`() = runTest {
        val repository = repository { respond(historyJson, HttpStatusCode.OK, jsonHeaders) }

        val days = assertIs<Result.Success<StationHistory>>(repository.history("PZH", HistoryRange.WEEK)).data.days

        assertEquals(PollenSeverity.LOW, days[0].levels["BIRCH"])
        assertEquals(PollenSeverity.NONE, days[0].levels["GRASSES"])
        assertNull(days[0].levels.getValue("ASH"))
        assertEquals(setOf("BIRCH", "GRASSES", "ASH"), days[1].levels.keys)
        assertEquals(listOf(null, null, null), days[1].levels.values.toList())
        assertEquals(PollenSeverity.VERY_HIGH, days[2].levels["BIRCH"])
        assertEquals(PollenSeverity.HIGH, days[2].levels["GRASSES"])
    }

    @Test
    fun `returns Failure on a bad request`() = runTest {
        val repository = repository {
            respond("""{"error":"Expected range=week"}""", HttpStatusCode.BadRequest, jsonHeaders)
        }

        assertIs<Result.Failure>(repository.history("PZH", HistoryRange.WEEK))
    }

    @Test
    fun `returns Failure when the station is not found`() = runTest {
        val repository = repository { respondError(HttpStatusCode.NotFound) }

        assertIs<Result.Failure>(repository.history("XXX", HistoryRange.WEEK))
    }

    @Test
    fun `returns Failure when the backend cannot reach its upstream`() = runTest {
        val repository = repository { respondError(HttpStatusCode.BadGateway) }

        assertIs<Result.Failure>(repository.history("PZH", HistoryRange.WEEK))
    }

    @Test
    fun `returns Failure when the backend responds with a server error`() = runTest {
        val repository = repository { respondError(HttpStatusCode.InternalServerError) }

        assertIs<Result.Failure>(repository.history("PZH", HistoryRange.WEEK))
    }

    @Test
    fun `returns Failure when the backend sends a severity the app does not know`() = runTest {
        val body = historyJson.replace("\"VERY_HIGH\"", "\"CATASTROPHIC\"")
        val repository = repository { respond(body, HttpStatusCode.OK, jsonHeaders) }

        assertIs<Result.Failure>(repository.history("PZH", HistoryRange.WEEK))
    }

    @Test
    fun `returns Failure when a date is not an ISO date`() = runTest {
        val body = historyJson.replace("\"2026-09-28\"", "\"yesterday\"")
        val repository = repository { respond(body, HttpStatusCode.OK, jsonHeaders) }

        assertIs<Result.Failure>(repository.history("PZH", HistoryRange.WEEK))
    }

    @Test
    fun `returns Failure instead of throwing when the backend is unreachable`() = runTest {
        val repository = repository { throw RuntimeException("Connection refused") }

        assertIs<Result.Failure>(repository.history("PZH", HistoryRange.WEEK))
    }
}
