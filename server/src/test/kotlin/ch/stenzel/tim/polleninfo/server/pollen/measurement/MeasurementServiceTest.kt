package ch.stenzel.tim.polleninfo.server.pollen.measurement

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.domain.SpeciesThresholds
import ch.stenzel.tim.polleninfo.server.pollen.upstream.FakePollenFileSource
import ch.stenzel.tim.polleninfo.server.pollen.upstream.hourlyCsv
import kotlinx.coroutines.test.runTest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MeasurementServiceTest {

    private val thresholds = PollenThresholds()
    private val fileSource = FakePollenFileSource()
    private val service = MeasurementService(fileSource, thresholds)

    private suspend fun severityOf(species: PollenSpecies, concentration: Int): PollenSeverity? {
        fileSource.bytes = hourlyCsv(
            rows = listOf("01.08.2026 09:00" to mapOf(species to concentration)),
        )
        return service.measurementFor(PollenStation.ZUERICH)
            ?.species
            ?.single { it.species == species }
            ?.severity
    }

    @Test
    fun `asks the file source for the station it was given`() = runTest {
        service.measurementFor(PollenStation.LUGANO)

        assertEquals(listOf(PollenStation.LUGANO), fileSource.requested)
    }

    @Test
    fun `reports all seven taxa and the row's timestamp`() = runTest {
        val measurement = checkNotNull(service.measurementFor(PollenStation.ZUERICH))

        assertEquals(PollenStation.ZUERICH, measurement.station)
        assertEquals(Instant.parse("2026-08-01T09:00:00Z"), measurement.measuredAt)
        assertEquals(PollenSpecies.entries, measurement.species.map { it.species })
    }

    @Test
    fun `classifies each taxon against its own bands`() = runTest {
        // 20 grains/m³ is the same air for both, and lands in different bands: the tree bounds are
        // 15 / 90 / 1500, the grass bounds 5 / 20 / 200.
        assertEquals(PollenSeverity.MODERATE, severityOf(PollenSpecies.BIRCH, 20))
        assertEquals(PollenSeverity.HIGH, severityOf(PollenSpecies.GRASSES, 20))
    }

    @Test
    fun `every band bound classifies correctly at the bound and immediately below it`() = runTest {
        // Pinned from both sides for all seven taxa: an off-by-one here silently mislabels the
        // severity a user acts on, and reads as plausible either way.
        PollenSpecies.entries.forEach { species ->
            val bands = thresholds.forSpecies(species)
            val bounds = listOf(
                bands.moderate to PollenSeverity.MODERATE,
                bands.high to PollenSeverity.HIGH,
                bands.veryHigh to PollenSeverity.VERY_HIGH,
            )
            bounds.forEachIndexed { index, (bound, expected) ->
                val below = bounds.getOrNull(index - 1)?.second ?: PollenSeverity.LOW
                assertEquals(expected, severityOf(species, bound), "$species at $bound")
                assertEquals(below, severityOf(species, bound - 1), "$species at ${bound - 1}")
            }
        }
    }

    @Test
    fun `a measured zero is NONE, not an absent reading`() = runTest {
        assertEquals(PollenSeverity.NONE, severityOf(PollenSpecies.BIRCH, 0))
    }

    @Test
    fun `a taxon with no column has neither a concentration nor a severity`() = runTest {
        fileSource.bytes = hourlyCsv(
            rows = listOf("01.08.2026 09:00" to mapOf(PollenSpecies.GRASSES to 20)),
            columns = listOf(PollenSpecies.GRASSES),
        )

        val ash = checkNotNull(service.measurementFor(PollenStation.ZUERICH))
            .species.single { it.species == PollenSpecies.ASH }

        assertNull(ash.concentration)
        assertNull(ash.severity)
    }

    @Test
    fun `returns null when the file holds no usable row`() = runTest {
        fileSource.bytes = hourlyCsv(rows = listOf("01.08.2026 09:00" to emptyMap()))

        assertNull(service.measurementFor(PollenStation.ZUERICH))
    }

    @Test
    fun `propagates a file source failure rather than reporting an empty station`() = runTest {
        fileSource.failure = IllegalStateException("upstream down")

        assertFailsWith<IllegalStateException> { service.measurementFor(PollenStation.ZUERICH) }
    }

    @Test
    fun `reflects a retuned threshold table`() = runTest {
        val retuned = MeasurementService(
            fileSource,
            PollenThresholds(
                PollenThresholds.DEFAULTS + (PollenSpecies.BIRCH to SpeciesThresholds(3, 10, 40)),
            ),
        )
        fileSource.bytes = hourlyCsv(
            rows = listOf("01.08.2026 09:00" to mapOf(PollenSpecies.BIRCH to 20)),
        )

        val birch = checkNotNull(retuned.measurementFor(PollenStation.ZUERICH))
            .species.single { it.species == PollenSpecies.BIRCH }

        assertEquals(PollenSeverity.HIGH, birch.severity)
    }
}
