package ch.stenzel.tim.polleninfo.feature.home.data.mapper

import ch.stenzel.tim.polleninfo.feature.home.data.remote.dto.SpeciesReadingDto
import ch.stenzel.tim.polleninfo.feature.home.data.remote.dto.StationMeasurementDto
import ch.stenzel.tim.polleninfo.feature.home.domain.model.PollenSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class StationMeasurementMapperTest {

    private fun dto(
        species: List<SpeciesReadingDto> = listOf(speciesDto()),
    ) = StationMeasurementDto(
        stationAbbr = "PZH",
        measuredAt = "2026-08-01T09:00:00Z",
        unit = "grains/m3",
        species = species,
    )

    private fun speciesDto(
        id: String = "BIRCH",
        name: String = "Birch",
        concentration: Int? = 42,
        severity: String? = "MODERATE",
    ) = SpeciesReadingDto(
        id = id,
        name = name,
        latinName = "Betula",
        concentration = concentration,
        severity = severity,
    )

    @Test
    fun `maps the station abbreviation the unit and every taxon`() {
        val domain = dto(
            species = listOf(speciesDto(), speciesDto(id = "GRASSES", name = "Grasses")),
        ).toDomain()

        assertEquals("PZH", domain.stationAbbr)
        assertEquals("grains/m3", domain.unit)
        assertEquals(listOf("BIRCH", "GRASSES"), domain.species.map { it.id })
        assertEquals(listOf("Birch", "Grasses"), domain.species.map { it.name })
        assertEquals(42, domain.species.first().concentration)
    }

    @Test
    fun `every severity constant maps to its exact wire string`() {
        // The app declares its own PollenSeverity because it cannot depend on `:server`. This is
        // what holds the two copies together: adding or renaming a constant fails here rather than
        // at runtime on a user's phone.
        PollenSeverity.entries.forEach { severity ->
            val domain = dto(species = listOf(speciesDto(severity = severity.name))).toDomain()

            assertEquals(severity, domain.species.single().severity, "wire string '${severity.name}'")
        }
    }

    @Test
    fun `the wire strings are the ones the server sends`() {
        // Spelled out literally as well, so the test cannot pass by agreeing with a renamed enum.
        val wire = listOf("NONE", "LOW", "MODERATE", "HIGH", "VERY_HIGH")

        assertEquals(wire, PollenSeverity.entries.map { it.name })
    }

    @Test
    fun `an unrecognised severity fails the mapping rather than defaulting`() {
        // Defaulting to NONE would render a band we do not know about as a calm day.
        assertFailsWith<IllegalArgumentException> {
            dto(species = listOf(speciesDto(severity = "CATASTROPHIC"))).toDomain()
        }
    }

    @Test
    fun `a taxon with no reading maps to a null concentration and a null severity`() {
        val domain = dto(
            species = listOf(speciesDto(id = "ASH", name = "Ash", concentration = null, severity = null)),
        ).toDomain()

        val ash = domain.species.single()
        assertNull(ash.concentration)
        assertNull(ash.severity)
        // Still named, so the screen can list it as unmeasured rather than dropping it.
        assertEquals("Ash", ash.name)
    }

    @Test
    fun `a measured zero maps to NONE rather than to an absent reading`() {
        val domain = dto(species = listOf(speciesDto(concentration = 0, severity = "NONE"))).toDomain()

        assertEquals(0, domain.species.single().concentration)
        assertEquals(PollenSeverity.NONE, domain.species.single().severity)
    }

    @Test
    fun `all seven taxa survive the mapping`() {
        val seven = listOf("ALDER", "BIRCH", "HAZEL", "BEECH", "ASH", "OAK", "GRASSES")

        val domain = dto(species = seven.map { speciesDto(id = it, name = it.lowercase()) }).toDomain()

        assertEquals(seven, domain.species.map { it.id })
    }
}
