package ch.stenzel.tim.polleninfo.server.pollen

import ch.stenzel.tim.polleninfo.server.plugins.configureRouting
import ch.stenzel.tim.polleninfo.server.plugins.configureSerialization
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.domain.SpeciesThresholds
import ch.stenzel.tim.polleninfo.server.pollen.history.HistoryService
import ch.stenzel.tim.polleninfo.server.pollen.measurement.MeasurementService
import ch.stenzel.tim.polleninfo.server.pollen.measurement.MutableClock
import ch.stenzel.tim.polleninfo.server.pollen.measurement.TtlCache
import ch.stenzel.tim.polleninfo.server.pollen.model.ErrorDto
import ch.stenzel.tim.polleninfo.server.pollen.model.SpeciesDto
import ch.stenzel.tim.polleninfo.server.pollen.model.StationDto
import ch.stenzel.tim.polleninfo.server.pollen.model.StationHistoryDto
import ch.stenzel.tim.polleninfo.server.pollen.model.StationMeasurementDto
import ch.stenzel.tim.polleninfo.server.pollen.model.ThresholdsDto
import ch.stenzel.tim.polleninfo.server.pollen.upstream.FakePollenService
import ch.stenzel.tim.polleninfo.server.pollen.upstream.dailyCsv
import ch.stenzel.tim.polleninfo.server.pollen.upstream.hourlyCsv
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PollenRoutesTest {

    private val pollenService = FakePollenService()

    // 09:00 UTC on 4 October 2026: 11:00 in Zürich, so the history's "yesterday" is 3 October.
    private val historyClock = MutableClock(Instant.parse("2026-10-04T09:00:00Z"))

    private fun ApplicationTestBuilder.installApp(
        thresholds: PollenThresholds = PollenThresholds(),
        measurementService: MeasurementService = MeasurementService(pollenService, thresholds),
        historyService: HistoryService = HistoryService(pollenService, thresholds, historyClock),
    ) {
        application {
            configureSerialization()
            configureRouting(thresholds, measurementService, historyService)
        }
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) { json() }
    }

    @Test
    fun `stations endpoint lists all fifteen stations`() = testApplication {
        installApp()

        val stations = jsonClient().get("/pollen/stations").body<List<StationDto>>()

        assertEquals(15, stations.size)
        assertTrue(stations.any { it.abbr == "PZH" && it.name == "Zürich" })
        assertTrue(stations.all { it.canton.length == 2 })
    }

    @Test
    fun `a single station can be looked up by abbreviation`() = testApplication {
        installApp()

        val station = jsonClient().get("/pollen/stations/PZH").body<StationDto>()

        assertEquals("PZH", station.abbr)
        assertEquals("Zürich", station.name)
        assertEquals("ZH", station.canton)
        assertEquals(559, station.altitudeMasl)
    }

    @Test
    fun `station lookup is case insensitive`() = testApplication {
        installApp()

        val response = jsonClient().get("/pollen/stations/pzh")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("PZH", response.body<StationDto>().abbr)
    }

    @Test
    fun `an unknown station abbreviation returns 404`() = testApplication {
        installApp()

        assertEquals(HttpStatusCode.NotFound, jsonClient().get("/pollen/stations/XXX").status)
    }

    @Test
    fun `species endpoint lists the seven MeteoSwiss taxa`() = testApplication {
        installApp()

        val species = jsonClient().get("/pollen/species").body<List<SpeciesDto>>()

        assertEquals(7, species.size)
        assertEquals(
            PollenSpecies.entries.map { it.name }.toSet(),
            species.map { it.id }.toSet(),
        )
        assertTrue(species.any { it.id == "GRASSES" && it.latinName == "Poaceae" })
    }

    @Test
    fun `thresholds endpoint publishes a band set for every species`() = testApplication {
        installApp()

        val body = jsonClient().get("/pollen/thresholds").body<ThresholdsDto>()

        assertEquals("grains/m3", body.unit)
        assertEquals(PollenSpecies.entries.map { it.name }.toSet(), body.bySpecies.keys)
    }

    @Test
    fun `thresholds endpoint exposes different bands per species`() = testApplication {
        installApp()

        val bySpecies = jsonClient().get("/pollen/thresholds").body<ThresholdsDto>().bySpecies

        assertEquals(SpeciesThresholds(15, 90, 1500), bySpecies.getValue("BIRCH"))
        assertEquals(SpeciesThresholds(5, 20, 200), bySpecies.getValue("GRASSES"))
    }

    @Test
    fun `thresholds endpoint reflects a server side reconfiguration`() = testApplication {
        val retuned = PollenThresholds(
            PollenThresholds.DEFAULTS + (PollenSpecies.BIRCH to SpeciesThresholds(3, 10, 40)),
        )
        installApp(retuned)

        val bySpecies = jsonClient().get("/pollen/thresholds").body<ThresholdsDto>().bySpecies

        assertEquals(SpeciesThresholds(3, 10, 40), bySpecies.getValue("BIRCH"))
        assertEquals(SpeciesThresholds(15, 90, 1500), bySpecies.getValue("OAK"))
    }

    @Test
    fun `measurements endpoint returns the station, the timestamp, the unit and seven taxa`() =
        testApplication {
            pollenService.bytes = hourlyCsv(
                rows = listOf("01.08.2026 09:00" to mapOf(PollenSpecies.BIRCH to 42)),
            )
            installApp()

            val body = jsonClient()
                .get("/pollen/stations/PZH/measurements")
                .body<StationMeasurementDto>()

            assertEquals("PZH", body.stationAbbr)
            assertEquals("2026-08-01T09:00:00Z", body.measuredAt)
            assertEquals("grains/m3", body.unit)
            assertEquals(PollenSpecies.entries.map { it.name }, body.species.map { it.id })
        }

    @Test
    fun `an unmeasured taxon is distinguishable from one measuring zero`() = testApplication {
        pollenService.bytes = hourlyCsv(
            rows = listOf("01.08.2026 09:00" to mapOf(PollenSpecies.BIRCH to 0)),
            columns = listOf(PollenSpecies.BIRCH),
        )
        installApp()

        val species = jsonClient()
            .get("/pollen/stations/PZH/measurements")
            .body<StationMeasurementDto>()
            .species.associateBy { it.id }

        val birch = species.getValue("BIRCH")
        assertEquals(0, birch.concentration)
        assertEquals(PollenSeverity.NONE, birch.severity)

        val ash = species.getValue("ASH")
        assertNull(ash.concentration)
        assertNull(ash.severity)
        // Still named, so a client can list it as unmeasured rather than omitting it.
        assertEquals("Ash", ash.name)
        assertEquals("Fraxinus", ash.latinName)
    }

    @Test
    fun `measurements are classified per taxon`() = testApplication {
        pollenService.bytes = hourlyCsv(
            rows = listOf(
                "01.08.2026 09:00" to mapOf(
                    PollenSpecies.BIRCH to 20,
                    PollenSpecies.GRASSES to 20,
                ),
            ),
        )
        installApp()

        val species = jsonClient()
            .get("/pollen/stations/PZH/measurements")
            .body<StationMeasurementDto>()
            .species.associateBy { it.id }

        assertEquals(PollenSeverity.MODERATE, species.getValue("BIRCH").severity)
        assertEquals(PollenSeverity.HIGH, species.getValue("GRASSES").severity)
    }

    @Test
    fun `measurements can be requested with a lowercase abbreviation`() = testApplication {
        installApp()

        assertEquals(
            HttpStatusCode.OK,
            jsonClient().get("/pollen/stations/pzh/measurements").status,
        )
    }

    @Test
    fun `measurements for an unknown station return 404`() = testApplication {
        installApp()

        assertEquals(
            HttpStatusCode.NotFound,
            jsonClient().get("/pollen/stations/XXX/measurements").status,
        )
    }

    @Test
    fun `a station whose file holds no usable row returns 404`() = testApplication {
        pollenService.bytes = hourlyCsv(rows = listOf("01.08.2026 09:00" to emptyMap()))
        installApp()

        // A 200 with seven blank rows would be indistinguishable from a calm day.
        assertEquals(
            HttpStatusCode.NotFound,
            jsonClient().get("/pollen/stations/PZH/measurements").status,
        )
    }

    @Test
    fun `an upstream failure with nothing cached returns 502`() = testApplication {
        pollenService.failure = IOException("unreachable")
        installApp()

        assertEquals(
            HttpStatusCode.BadGateway,
            jsonClient().get("/pollen/stations/PZH/measurements").status,
        )
    }

    @Test
    fun `an upstream failure after a good fetch returns the previous reading and its timestamp`() =
        testApplication {
            val clock = MutableClock()
            val thresholds = PollenThresholds()
            installApp(
                thresholds = thresholds,
                measurementService = MeasurementService(
                    pollenService,
                    thresholds,
                    TtlCache(MeasurementService.CACHE_TTL, clock),
                ),
            )
            val client = jsonClient()
            val first = client.get("/pollen/stations/PZH/measurements").body<StationMeasurementDto>()

            clock.advanceBy(MeasurementService.CACHE_TTL)
            pollenService.failure = IOException("unreachable")
            val response = client.get("/pollen/stations/PZH/measurements")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(first, response.body<StationMeasurementDto>())
            assertEquals("2026-08-01T09:00:00Z", response.body<StationMeasurementDto>().measuredAt)
            assertEquals(2, pollenService.requested.size)
        }

    @Test
    fun `history returns thirty consecutive days ending yesterday oldest first`() = testApplication {
        installApp()

        val history = jsonClient().get("/pollen/stations/PZH/history?range=month").body<StationHistoryDto>()

        assertEquals("PZH", history.stationAbbr)
        assertEquals("month", history.range)
        assertEquals("2026-09-04", history.from)
        assertEquals("2026-10-03", history.until)
        val dates = history.days.map { LocalDate.parse(it.date) }
        assertEquals(30, dates.size)
        assertEquals(LocalDate.of(2026, 9, 4), dates.first())
        assertEquals(LocalDate.of(2026, 10, 3), dates.last())
        dates.zipWithNext().forEach { (a, b) -> assertEquals(a.plusDays(1), b) }
    }

    @Test
    fun `history days carry all seven taxa in declaration order`() = testApplication {
        installApp()

        val history = jsonClient().get("/pollen/stations/PZH/history?range=month").body<StationHistoryDto>()

        history.days.forEach { day ->
            assertEquals(PollenSpecies.entries.map { it.name }, day.species.map { it.id })
        }
    }

    @Test
    fun `history concentration and severity are null together on a day without a value`() =
        testApplication {
            pollenService.dailyRecentBytes = dailyCsv(
                rows = listOf("03.10.2026" to mapOf(PollenSpecies.GRASSES to 25)),
            )
            installApp()

            val history = jsonClient().get("/pollen/stations/PZH/history?range=month").body<StationHistoryDto>()

            val yesterday = history.days.last().species
            assertEquals(25, yesterday.single { it.id == "GRASSES" }.concentration)
            assertEquals(PollenSeverity.HIGH, yesterday.single { it.id == "GRASSES" }.severity)
            val ash = yesterday.single { it.id == "ASH" }
            assertNull(ash.concentration)
            assertNull(ash.severity)
            // 2 October has no row at all.
            assertTrue(history.days[history.days.size - 2].species.all { it.concentration == null && it.severity == null })
        }

    @Test
    fun `history accepts a lowercase station abbreviation`() = testApplication {
        installApp()

        val response = jsonClient().get("/pollen/stations/pzh/history?range=month")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("PZH", response.body<StationHistoryDto>().stationAbbr)
    }

    @Test
    fun `history without a range is 400`() = testApplication {
        installApp()

        val response = jsonClient().get("/pollen/stations/PZH/history")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.body<ErrorDto>().error.isNotBlank())
    }

    @Test
    fun `history with an unknown range is 400`() = testApplication {
        installApp()

        assertEquals(HttpStatusCode.BadRequest, jsonClient().get("/pollen/stations/PZH/history?range=day").status)
        assertEquals(HttpStatusCode.BadRequest, jsonClient().get("/pollen/stations/PZH/history?range=Month").status)
    }

    @Test
    fun `history with a bad range is 400 even for an unknown station`() = testApplication {
        installApp()

        assertEquals(HttpStatusCode.BadRequest, jsonClient().get("/pollen/stations/XXX/history?range=day").status)
        assertEquals(HttpStatusCode.BadRequest, jsonClient().get("/pollen/stations/XXX/history").status)
    }

    @Test
    fun `history for an unknown station with a valid range is 404`() = testApplication {
        installApp()

        assertEquals(HttpStatusCode.NotFound, jsonClient().get("/pollen/stations/XXX/history?range=month").status)
        assertTrue(pollenService.dailyRecentRequested.isEmpty())
    }

    @Test
    fun `history is 502 when the upstream fails with nothing retained`() = testApplication {
        pollenService.dailyRecentFailure = IOException("upstream down")
        installApp()

        assertEquals(HttpStatusCode.BadGateway, jsonClient().get("/pollen/stations/PZH/history?range=month").status)
    }

    @Test
    fun `history serves the retained rows when a reload fails`() = testApplication {
        pollenService.dailyRecentBytes = dailyCsv(
            rows = listOf("03.10.2026" to mapOf(PollenSpecies.GRASSES to 25)),
        )
        installApp()
        val client = jsonClient()
        client.get("/pollen/stations/PZH/history?range=month")

        historyClock.advanceBy(HistoryService.RECENT_TTL)
        pollenService.dailyRecentFailure = IOException("upstream down")
        val response = client.get("/pollen/stations/PZH/history?range=month")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(25, response.body<StationHistoryDto>().days.last().species.single { it.id == "GRASSES" }.concentration)
    }

    @Test
    fun `history for a week returns seven days ending yesterday`() = testApplication {
        installApp()

        val history = jsonClient().get("/pollen/stations/PZH/history?range=week").body<StationHistoryDto>()

        assertEquals("week", history.range)
        assertEquals("2026-09-27", history.from)
        assertEquals("2026-10-03", history.until)
        assertEquals(7, history.days.size)
    }

    @Test
    fun `history for a year returns 365 days ending yesterday`() = testApplication {
        installApp()

        val history = jsonClient().get("/pollen/stations/PZH/history?range=year").body<StationHistoryDto>()

        assertEquals("year", history.range)
        assertEquals("2025-10-04", history.from)
        assertEquals("2026-10-03", history.until)
        assertEquals(365, history.days.size)
        assertEquals(LocalDate.of(2025, 10, 4), LocalDate.parse(history.days.first().date))
        assertEquals(LocalDate.of(2026, 10, 3), LocalDate.parse(history.days.last().date))
    }

    @Test
    fun `a year is 502 when the earlier years fail with nothing retained even though this year answers`() =
        testApplication {
            pollenService.dailyHistoricalFailure = IOException("upstream down")
            installApp()
            val client = jsonClient()

            assertEquals(HttpStatusCode.BadGateway, client.get("/pollen/stations/PZH/history?range=year").status)
            // The month does not need the earlier years and still answers.
            assertEquals(HttpStatusCode.OK, client.get("/pollen/stations/PZH/history?range=month").status)
        }
}
