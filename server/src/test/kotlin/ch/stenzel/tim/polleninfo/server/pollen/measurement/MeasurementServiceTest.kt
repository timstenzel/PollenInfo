package ch.stenzel.tim.polleninfo.server.pollen.measurement

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.domain.SpeciesThresholds
import ch.stenzel.tim.polleninfo.server.pollen.upstream.FakePollenService
import ch.stenzel.tim.polleninfo.server.pollen.upstream.hourlyCsv
import ch.stenzel.tim.polleninfo.server.pollen.upstream.ParsedReading
import ch.stenzel.tim.polleninfo.server.pollen.upstream.PollenService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementServiceTest {

    private val thresholds = PollenThresholds()
    private val pollenService = FakePollenService()
    private val clock = MutableClock()
    private val service = MeasurementService(
        pollenService,
        thresholds,
        TtlCache(MeasurementService.CACHE_TTL, clock),
    )

    private suspend fun MeasurementService.freshFor(station: PollenStation): StationMeasurement =
        assertIs<CacheResult.Fresh<StationMeasurement>>(measurementFor(station)).value

    private suspend fun severityOf(species: PollenSpecies, concentration: Int): PollenSeverity? {
        pollenService.bytes = hourlyCsv(
            rows = listOf("01.08.2026 09:00" to mapOf(species to concentration)),
        )
        // A service per reading, since a shared one would answer every call after the first from
        // its cache.
        val cache = TtlCache<PollenStation, ParsedReading>(MeasurementService.CACHE_TTL, clock)
        return MeasurementService(pollenService, thresholds, cache)
            .freshFor(PollenStation.ZUERICH)
            .species
            .single { it.species == species }
            .severity
    }

    @Test
    fun `asks the pollen service for the station it was given`() = runTest {
        service.measurementFor(PollenStation.LUGANO)

        assertEquals(listOf(PollenStation.LUGANO), pollenService.requested)
    }

    @Test
    fun `reports all seven taxa and the row's timestamp`() = runTest {
        val measurement = service.freshFor(PollenStation.ZUERICH)

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
        pollenService.bytes = hourlyCsv(
            rows = listOf("01.08.2026 09:00" to mapOf(PollenSpecies.GRASSES to 20)),
            columns = listOf(PollenSpecies.GRASSES),
        )

        val ash = service.freshFor(PollenStation.ZUERICH)
            .species.single { it.species == PollenSpecies.ASH }

        assertNull(ash.concentration)
        assertNull(ash.severity)
    }

    @Test
    fun `fails with a no usable row cause when the file holds no usable row`() = runTest {
        pollenService.bytes = hourlyCsv(rows = listOf("01.08.2026 09:00" to emptyMap()))

        val result = assertIs<CacheResult.Failed>(service.measurementFor(PollenStation.ZUERICH))
        assertIs<NoUsableRowException>(result.cause)
    }

    @Test
    fun `fails with the pollen service's exception rather than reporting an empty station`() =
        runTest {
            val cause = IllegalStateException("upstream down")
            pollenService.failure = cause

            val result = assertIs<CacheResult.Failed>(service.measurementFor(PollenStation.ZUERICH))
            assertSame(cause, result.cause)
        }

    @Test
    fun `a second request inside the cache period does not contact the pollen service`() =
        runTest {
            val first = service.freshFor(PollenStation.ZUERICH)
            clock.advanceBy(MeasurementService.CACHE_TTL.minusSeconds(1))

            assertEquals(first, service.freshFor(PollenStation.ZUERICH))
            assertEquals(1, pollenService.requested.size)
        }

    @Test
    fun `a request after the cache period fetches again`() = runTest {
        service.freshFor(PollenStation.ZUERICH)
        clock.advanceBy(MeasurementService.CACHE_TTL)
        pollenService.bytes = hourlyCsv(
            rows = listOf("01.08.2026 10:00" to mapOf(PollenSpecies.BIRCH to 42)),
        )

        val second = service.freshFor(PollenStation.ZUERICH)

        assertEquals(Instant.parse("2026-08-01T10:00:00Z"), second.measuredAt)
        assertEquals(2, pollenService.requested.size)
    }

    @Test
    fun `stations are cached separately`() = runTest {
        service.freshFor(PollenStation.ZUERICH)
        service.freshFor(PollenStation.LUGANO)

        assertEquals(listOf(PollenStation.ZUERICH, PollenStation.LUGANO), pollenService.requested)
    }

    @Test
    fun `a failed fetch serves the previous reading with its original timestamp`() = runTest {
        service.freshFor(PollenStation.ZUERICH)
        clock.advanceBy(MeasurementService.CACHE_TTL)
        pollenService.failure = IllegalStateException("upstream down")

        val stale = assertIs<CacheResult.Stale<StationMeasurement>>(
            service.measurementFor(PollenStation.ZUERICH),
        ).value

        assertEquals(Instant.parse("2026-08-01T09:00:00Z"), stale.measuredAt)
        val birch = stale.species.single { it.species == PollenSpecies.BIRCH }
        assertEquals(PollenSeverity.MODERATE, birch.severity)
    }

    @Test
    fun `a file with no usable row after a good one serves the previous reading`() = runTest {
        service.freshFor(PollenStation.ZUERICH)
        clock.advanceBy(MeasurementService.CACHE_TTL)
        pollenService.bytes = hourlyCsv(rows = listOf("02.08.2026 00:00" to emptyMap()))

        val stale = assertIs<CacheResult.Stale<StationMeasurement>>(
            service.measurementFor(PollenStation.ZUERICH),
        ).value

        assertEquals(Instant.parse("2026-08-01T09:00:00Z"), stale.measuredAt)
    }

    @Test
    fun `reflects a retuned threshold table`() = runTest {
        val retuned = MeasurementService(
            pollenService,
            PollenThresholds(
                PollenThresholds.DEFAULTS + (PollenSpecies.BIRCH to SpeciesThresholds(3, 10, 40)),
            ),
        )
        pollenService.bytes = hourlyCsv(
            rows = listOf("01.08.2026 09:00" to mapOf(PollenSpecies.BIRCH to 20)),
        )

        val birch = retuned.freshFor(PollenStation.ZUERICH)
            .species.single { it.species == PollenSpecies.BIRCH }

        assertEquals(PollenSeverity.HIGH, birch.severity)
    }

    @Test
    fun `simultaneous first requests for one station fetch once without holding up another`() =
        runTest {
            val zurichGate = CompletableDeferred<Unit>()
            val gated = object : PollenService {
                val requested = mutableListOf<PollenStation>()

                override suspend fun hourlyNow(station: PollenStation): ByteArray {
                    requested += station
                    if (station == PollenStation.ZUERICH) zurichGate.await()
                    return hourlyCsv()
                }

                override suspend fun dailyRecent(station: PollenStation): ByteArray =
                    error("not used by MeasurementService")
            }
            val gatedService = MeasurementService(
                gated,
                thresholds,
                TtlCache(MeasurementService.CACHE_TTL, clock),
            )

            val zurich = List(5) { async { gatedService.freshFor(PollenStation.ZUERICH) } }
            runCurrent()

            // Zürich's fetch is still hanging; Lugano answers regardless.
            gatedService.freshFor(PollenStation.LUGANO)
            assertTrue(zurich.none { it.isCompleted })

            zurichGate.complete(Unit)
            assertEquals(1, zurich.awaitAll().distinct().size)
            assertEquals(listOf(PollenStation.ZUERICH, PollenStation.LUGANO), gated.requested)
        }
}
