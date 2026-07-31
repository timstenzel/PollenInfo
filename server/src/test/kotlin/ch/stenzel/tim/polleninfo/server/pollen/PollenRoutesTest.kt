package ch.stenzel.tim.polleninfo.server.pollen

import ch.stenzel.tim.polleninfo.server.plugins.configureRouting
import ch.stenzel.tim.polleninfo.server.plugins.configureSerialization
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.domain.SpeciesThresholds
import ch.stenzel.tim.polleninfo.server.pollen.model.SpeciesDto
import ch.stenzel.tim.polleninfo.server.pollen.model.StationDto
import ch.stenzel.tim.polleninfo.server.pollen.model.ThresholdsDto
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PollenRoutesTest {

    private fun ApplicationTestBuilder.installApp(
        thresholds: PollenThresholds = PollenThresholds(),
    ) {
        application {
            configureSerialization()
            configureRouting(thresholds)
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
}
