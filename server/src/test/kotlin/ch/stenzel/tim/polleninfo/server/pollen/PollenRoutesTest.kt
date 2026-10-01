package ch.stenzel.tim.polleninfo.server.pollen

import ch.stenzel.tim.polleninfo.server.plugins.configureRouting
import ch.stenzel.tim.polleninfo.server.plugins.configureSerialization
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.domain.SpeciesThresholds
import ch.stenzel.tim.polleninfo.server.pollen.measurement.MeasurementService
import ch.stenzel.tim.polleninfo.server.pollen.measurement.MutableClock
import ch.stenzel.tim.polleninfo.server.pollen.measurement.TtlCache
import ch.stenzel.tim.polleninfo.server.pollen.model.SpeciesDto
import ch.stenzel.tim.polleninfo.server.pollen.model.StationDto
import ch.stenzel.tim.polleninfo.server.pollen.model.StationMeasurementDto
import ch.stenzel.tim.polleninfo.server.pollen.model.ThresholdsDto
import ch.stenzel.tim.polleninfo.server.pollen.upstream.FakePollenService
import ch.stenzel.tim.polleninfo.server.pollen.upstream.hourlyCsv
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PollenRoutesTest {

    private val pollenService = FakePollenService()

    private fun ApplicationTestBuilder.installApp(
        thresholds: PollenThresholds = PollenThresholds(),
        measurementService: MeasurementService = MeasurementService(pollenService, thresholds),
    ) {
        application {
            configureSerialization()
            configureRouting(thresholds, measurementService)
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
}
