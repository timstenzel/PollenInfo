package ch.stenzel.tim.polleninfo.feature.onboarding.domain.usecase

import ch.stenzel.tim.polleninfo.core.station.allStations
import ch.stenzel.tim.polleninfo.core.station.station
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FindNearestStationUseCaseTest {

    private val findNearestStation = FindNearestStationUseCase()

    @Test
    fun `resolves a position near Winterthur to the Zurich station`() {
        val nearest = findNearestStation(
            latitude = 47.4989,
            longitude = 8.7286,
            stations = allStations,
        )

        assertEquals("PZH", nearest?.abbr)
    }

    @Test
    fun `resolves a position in Valais to the Sion station`() {
        val nearest = findNearestStation(latitude = 46.2000, longitude = 7.3000, stations = allStations)

        assertEquals("PSN", nearest?.abbr)
    }

    @Test
    fun `resolves a position in Germany to a Swiss station without an error`() {
        // Stuttgart — 127 km from Münsterlingen, the closest Swiss station to it. There is no
        // distance cap on purpose: the user confirms the proposal, so an out-of-country position
        // gets a visible, overridable answer rather than a failure.
        val nearest = findNearestStation(latitude = 48.7758, longitude = 9.1829, stations = allStations)

        assertEquals("PMU", nearest?.abbr)
        assertTrue(nearest in allStations, "the proposal must be one of the 15 stations")
    }

    @Test
    fun `resolves a station's own coordinates to that station`() {
        val geneve = allStations.single { it.abbr == "PGE" }

        val nearest = findNearestStation(geneve.latitude, geneve.longitude, allStations)

        assertEquals(geneve, nearest)
    }

    @Test
    fun `resolves an exact tie to the first station in the list`() {
        // Same latitude, half a degree of longitude either side: the two distances are equal down to
        // the last bit, so only the list order can decide. It has to be the first entry — the list
        // is alphabetical, which makes the outcome predictable rather than arbitrary.
        val west = station(abbr = "PWW", name = "West", latitude = 47.0, longitude = 8.0)
        val east = station(abbr = "PEE", name = "East", latitude = 47.0, longitude = 9.0)

        assertEquals(west, findNearestStation(47.0, 8.5, listOf(west, east)))
        assertEquals(east, findNearestStation(47.0, 8.5, listOf(east, west)))
    }

    @Test
    fun `resolves to nothing when there are no stations to choose from`() {
        assertNull(findNearestStation(latitude = 47.0, longitude = 8.0, stations = emptyList()))
    }

    @Test
    fun `resolves the antipode of a station to some station rather than failing`() {
        // Guards the asin() domain: a point diametrically opposite pushes the haversine term to 1.0,
        // where floating-point rounding can nudge it just above and make asin return NaN.
        val zurich = allStations.single { it.abbr == "PZH" }

        // The expected answer is the station *furthest* from Zürich, which is Genève. A NaN distance
        // would make every comparison false and silently hand back the first entry (Basel) instead.
        val nearest = findNearestStation(-zurich.latitude, zurich.longitude - 180, allStations)

        assertEquals("PGE", nearest?.abbr)
    }
}
