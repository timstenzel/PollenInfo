package ch.stenzel.tim.polleninfo.feature.onboarding.data.mapper

import ch.stenzel.tim.polleninfo.feature.onboarding.data.remote.dto.StationDto
import ch.stenzel.tim.polleninfo.feature.onboarding.expectedStationNamesAlphabetical
import ch.stenzel.tim.polleninfo.feature.onboarding.stationDtosInServerOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StationMapperTest {

    private val mapped = stationDtosInServerOrder.toDomain()

    @Test
    fun `sorts the stations alphabetically by display name`() {
        assertEquals(expectedStationNamesAlphabetical, mapped.map { it.name })
    }

    @Test
    fun `puts Basel first and Zurich last`() {
        assertEquals("Basel", mapped.first().name)
        assertEquals("Zürich", mapped.last().name)
    }

    @Test
    // Comma-free name: Kotlin/Native rejects `,` in a declaration name.
    fun `orders Lausanne before Locarno - the case the server's own ordering gets wrong`() {
        // The server emits `PollenStation` enum order — by abbreviation — so PLO Locarno arrives
        // before PLS Lausanne. This is the case a missing sort would silently pass through.
        val incoming = stationDtosInServerOrder.map { it.abbr }
        assertTrue(
            incoming.indexOf("PLO") < incoming.indexOf("PLS"),
            "fixture must reproduce the server's abbreviation ordering",
        )

        val names = mapped.map { it.name }
        assertTrue(
            names.indexOf("Lausanne") < names.indexOf("Locarno / Monti"),
            "Lausanne must precede Locarno / Monti, was: $names",
        )
    }

    @Test
    fun `sorts the accented names where a reader would expect them`() {
        val names = mapped.map { it.name }
        // Genève between Davos and La Chaux-de-Fonds, Münsterlingen before Neuchâtel, Zürich last:
        // the accents never reach the comparison because the preceding characters already differ.
        assertEquals("Genève", names[names.indexOf("Davos / Wolfgang") + 1])
        assertEquals("La Chaux-de-Fonds", names[names.indexOf("Genève") + 1])
        assertEquals("Neuchâtel", names[names.indexOf("Münsterlingen") + 1])
        assertEquals(names.lastIndex, names.indexOf("Zürich"))
    }

    @Test
    fun `sorting an already alphabetical list leaves it untouched`() {
        val sortedInput = stationDtosInServerOrder.sortedBy { it.name }

        assertEquals(expectedStationNamesAlphabetical, sortedInput.toDomain().map { it.name })
    }

    @Test
    fun `carries the abbreviation and name and coordinates into the domain model`() {
        val zurich = mapped.single { it.abbr == "PZH" }

        assertEquals("Zürich", zurich.name)
        assertEquals(47.378225, zurich.latitude)
        assertEquals(8.565644, zurich.longitude)
    }

    @Test
    fun `maps every station rather than dropping any`() {
        assertEquals(15, mapped.size)
        assertEquals(stationDtosInServerOrder.map { it.abbr }.sorted(), mapped.map { it.abbr }.sorted())
    }

    @Test
    fun `maps an empty payload to an empty list`() {
        assertEquals(emptyList(), emptyList<StationDto>().toDomain())
    }
}
