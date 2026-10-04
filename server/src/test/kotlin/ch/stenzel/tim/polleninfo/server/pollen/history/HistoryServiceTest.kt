package ch.stenzel.tim.polleninfo.server.pollen.history

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.domain.SpeciesThresholds
import ch.stenzel.tim.polleninfo.server.pollen.measurement.CacheResult
import ch.stenzel.tim.polleninfo.server.pollen.measurement.MutableClock
import ch.stenzel.tim.polleninfo.server.pollen.upstream.FakePollenService
import ch.stenzel.tim.polleninfo.server.pollen.upstream.dailyCsv
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryServiceTest {

    private val pollenService = FakePollenService()

    // 09:00 UTC on 4 October 2026 is 11:00 in Zürich, so "yesterday" is 3 October either way.
    private val clock = MutableClock(Instant.parse("2026-10-04T09:00:00Z"))

    private fun service(thresholds: PollenThresholds = PollenThresholds()) =
        HistoryService(pollenService, thresholds, clock)

    private suspend fun HistoryService.freshMonth(): StationHistory {
        val result = historyFor(PollenStation.ZUERICH, HistoryRange.MONTH)
        return assertIs<CacheResult.Fresh<StationHistory>>(result).value
    }

    private fun StationHistory.on(date: LocalDate, species: PollenSpecies) =
        days.single { it.date == date }.species.single { it.species == species }

    @Test
    fun `a month covers thirty consecutive days ending yesterday oldest first`() = runTest {
        val history = service().freshMonth()

        assertEquals(LocalDate.of(2026, 9, 4), history.from)
        assertEquals(LocalDate.of(2026, 10, 3), history.until)
        assertEquals(30, history.days.size)
        history.days.zipWithNext().forEach { (a, b) -> assertEquals(a.date.plusDays(1), b.date) }
        assertEquals(history.from, history.days.first().date)
        assertEquals(history.until, history.days.last().date)
    }

    @Test
    fun `yesterday is the Swiss date just after Swiss midnight while UTC is still on the day before`() =
        runTest {
            // 22:30 UTC on 3 October is 00:30 on 4 October in Zürich (UTC+2 in summer time).
            clock.now = Instant.parse("2026-10-03T22:30:00Z")

            assertEquals(LocalDate.of(2026, 10, 3), service().freshMonth().until)
        }

    @Test
    fun `yesterday is still the Swiss day before just before Swiss midnight`() = runTest {
        // 21:59 UTC on 3 October is 23:59 on 3 October in Zürich.
        clock.now = Instant.parse("2026-10-03T21:59:00Z")

        assertEquals(LocalDate.of(2026, 10, 2), service().freshMonth().until)
    }

    @Test
    fun `every day carries all seven taxa in declaration order`() = runTest {
        val history = service().freshMonth()

        history.days.forEach { day -> assertEquals(PollenSpecies.entries, day.species.map { it.species }) }
    }

    @Test
    fun `a date the file has no row for is a day with every taxon empty`() = runTest {
        pollenService.dailyRecentBytes = dailyCsv(
            rows = listOf(
                "02.10.2026" to mapOf(PollenSpecies.GRASSES to 12),
                "03.10.2026" to mapOf(PollenSpecies.GRASSES to 0),
            ),
        )

        val history = service().freshMonth()

        val missing = history.days.single { it.date == LocalDate.of(2026, 10, 1) }
        assertTrue(missing.species.all { it.concentration == null && it.severity == null })
        assertEquals(12, history.on(LocalDate.of(2026, 10, 2), PollenSpecies.GRASSES).concentration)
        // A measured zero is not a gap.
        assertEquals(PollenSeverity.NONE, history.on(LocalDate.of(2026, 10, 3), PollenSpecies.GRASSES).severity)
    }

    @Test
    fun `an empty cell is no value with no severity`() = runTest {
        pollenService.dailyRecentBytes = dailyCsv(
            rows = listOf("03.10.2026" to mapOf(PollenSpecies.GRASSES to 12)),
        )

        val ash = service().freshMonth().on(LocalDate.of(2026, 10, 3), PollenSpecies.ASH)

        assertNull(ash.concentration)
        assertNull(ash.severity)
    }

    @Test
    fun `rows outside the window are not returned`() = runTest {
        pollenService.dailyRecentBytes = dailyCsv(
            rows = listOf(
                "03.09.2026" to mapOf(PollenSpecies.GRASSES to 99),
                "04.10.2026" to mapOf(PollenSpecies.GRASSES to 99),
            ),
        )

        val history = service().freshMonth()

        assertTrue(history.days.flatMap { it.species }.all { it.concentration == null })
    }

    @Test
    fun `classification uses the injected thresholds at the band edge and just below it`() = runTest {
        val thresholds = PollenThresholds(
            PollenThresholds.DEFAULTS + (PollenSpecies.BIRCH to SpeciesThresholds(3, 10, 40)),
        )
        pollenService.dailyRecentBytes = dailyCsv(
            rows = listOf(
                "02.10.2026" to mapOf(PollenSpecies.BIRCH to 9),
                "03.10.2026" to mapOf(PollenSpecies.BIRCH to 10),
            ),
        )

        val history = service(thresholds).freshMonth()

        assertEquals(PollenSeverity.MODERATE, history.on(LocalDate.of(2026, 10, 2), PollenSpecies.BIRCH).severity)
        assertEquals(PollenSeverity.HIGH, history.on(LocalDate.of(2026, 10, 3), PollenSpecies.BIRCH).severity)
    }

    @Test
    fun `the daily recent file is not fetched again within three hours`() = runTest {
        val service = service()
        service.freshMonth()

        clock.advanceBy(HistoryService.RECENT_TTL.minusSeconds(1))
        service.freshMonth()

        assertEquals(1, pollenService.dailyRecentRequested.size)
    }

    @Test
    fun `the daily recent file is fetched again once three hours have passed`() = runTest {
        val service = service()
        service.freshMonth()

        clock.advanceBy(HistoryService.RECENT_TTL)
        service.freshMonth()

        assertEquals(2, pollenService.dailyRecentRequested.size)
    }

    @Test
    fun `the cache is per station`() = runTest {
        val service = service()

        service.historyFor(PollenStation.ZUERICH, HistoryRange.MONTH)
        service.historyFor(PollenStation.BASEL, HistoryRange.MONTH)

        assertEquals(listOf(PollenStation.ZUERICH, PollenStation.BASEL), pollenService.dailyRecentRequested)
    }

    @Test
    fun `a failed reload serves the rows kept from the earlier fetch`() = runTest {
        pollenService.dailyRecentBytes = dailyCsv(
            rows = listOf("03.10.2026" to mapOf(PollenSpecies.GRASSES to 12)),
        )
        val service = service()
        service.freshMonth()

        clock.advanceBy(HistoryService.RECENT_TTL)
        pollenService.dailyRecentFailure = IOException("upstream down")
        val result = service.historyFor(PollenStation.ZUERICH, HistoryRange.MONTH)

        val history = assertIs<CacheResult.Stale<StationHistory>>(result).value
        assertEquals(12, history.on(LocalDate.of(2026, 10, 3), PollenSpecies.GRASSES).concentration)
    }

    @Test
    fun `kept rows that do not reach yesterday leave the newer days empty`() = runTest {
        pollenService.dailyRecentBytes = dailyCsv(
            rows = listOf("03.10.2026" to mapOf(PollenSpecies.GRASSES to 12)),
        )
        val service = service()
        service.freshMonth()

        // A day later the window ends on 4 October, which the kept copy cannot know about.
        clock.advanceBy(Duration.ofDays(1))
        pollenService.dailyRecentFailure = IOException("upstream down")
        val history = assertIs<CacheResult.Stale<StationHistory>>(
            service.historyFor(PollenStation.ZUERICH, HistoryRange.MONTH),
        ).value

        assertEquals(LocalDate.of(2026, 10, 4), history.until)
        assertTrue(history.days.last().species.all { it.concentration == null })
        assertEquals(12, history.on(LocalDate.of(2026, 10, 3), PollenSpecies.GRASSES).concentration)
    }

    @Test
    fun `a failure with nothing kept is a failed result`() = runTest {
        val failure = IOException("upstream down")
        pollenService.dailyRecentFailure = failure

        val result = service().historyFor(PollenStation.ZUERICH, HistoryRange.MONTH)

        assertEquals(failure, assertIs<CacheResult.Failed>(result).cause)
    }

    @Test
    fun `the history does not read the hourly file`() = runTest {
        service().freshMonth()

        assertTrue(pollenService.requested.isEmpty())
    }

    private suspend fun HistoryService.freshYear(): StationHistory {
        val result = historyFor(PollenStation.ZUERICH, HistoryRange.YEAR)
        return assertIs<CacheResult.Fresh<StationHistory>>(result).value
    }

    @Test
    fun `a year in October reads the earlier years and returns their values before 1 January`() = runTest {
        pollenService.dailyHistoricalBytes = dailyCsv(
            rows = listOf(
                "04.10.2025" to mapOf(PollenSpecies.GRASSES to 3),
                "31.12.2025" to mapOf(PollenSpecies.HAZEL to 7),
            ),
        )
        pollenService.dailyRecentBytes = dailyCsv(
            rows = listOf("01.01.2026" to mapOf(PollenSpecies.HAZEL to 9)),
        )

        val history = service().freshYear()

        assertEquals(listOf(PollenStation.ZUERICH), pollenService.dailyHistoricalRequested)
        assertEquals(LocalDate.of(2025, 10, 4), history.from)
        assertEquals(365, history.days.size)
        assertEquals(3, history.on(LocalDate.of(2025, 10, 4), PollenSpecies.GRASSES).concentration)
        assertEquals(7, history.on(LocalDate.of(2025, 12, 31), PollenSpecies.HAZEL).concentration)
        assertEquals(9, history.on(LocalDate.of(2026, 1, 1), PollenSpecies.HAZEL).concentration)
    }

    @Test
    fun `a week or a month inside the current year never reads the earlier years`() = runTest {
        val service = service()

        service.historyFor(PollenStation.ZUERICH, HistoryRange.WEEK)
        service.historyFor(PollenStation.ZUERICH, HistoryRange.MONTH)

        assertTrue(pollenService.dailyHistoricalRequested.isEmpty())
    }

    @Test
    fun `a month starting on 1 January does not read the earlier years`() = runTest {
        // 31 January: the month runs from 1 January to 30 January.
        clock.now = Instant.parse("2026-01-31T09:00:00Z")

        val history = service().freshMonth()

        assertEquals(LocalDate.of(2026, 1, 1), history.from)
        assertTrue(pollenService.dailyHistoricalRequested.isEmpty())
    }

    @Test
    fun `a month starting on 31 December reads the earlier years`() = runTest {
        // 30 January: the month runs from 31 December to 29 January.
        clock.now = Instant.parse("2026-01-30T09:00:00Z")
        pollenService.dailyHistoricalBytes = dailyCsv(
            rows = listOf("31.12.2025" to mapOf(PollenSpecies.HAZEL to 7)),
        )

        val history = service().freshMonth()

        assertEquals(LocalDate.of(2025, 12, 31), history.from)
        assertEquals(listOf(PollenStation.ZUERICH), pollenService.dailyHistoricalRequested)
        assertEquals(7, history.on(LocalDate.of(2025, 12, 31), PollenSpecies.HAZEL).concentration)
    }

    @Test
    fun `on a date both files hold the year to date value wins`() = runTest {
        pollenService.dailyHistoricalBytes = dailyCsv(
            rows = listOf("01.01.2026" to mapOf(PollenSpecies.HAZEL to 1)),
        )
        pollenService.dailyRecentBytes = dailyCsv(
            rows = listOf("01.01.2026" to mapOf(PollenSpecies.HAZEL to 9)),
        )

        val history = service().freshYear()

        assertEquals(9, history.on(LocalDate.of(2026, 1, 1), PollenSpecies.HAZEL).concentration)
    }

    @Test
    fun `the earlier years are not fetched again within a day`() = runTest {
        val service = service()
        service.freshYear()

        clock.advanceBy(HistoryService.HISTORICAL_TTL.minusSeconds(1))
        service.freshYear()

        assertEquals(1, pollenService.dailyHistoricalRequested.size)
        // The year-to-date file kept its own shorter period meanwhile.
        assertEquals(2, pollenService.dailyRecentRequested.size)
    }

    @Test
    fun `the earlier years are fetched again once a day has passed`() = runTest {
        val service = service()
        service.freshYear()

        clock.advanceBy(HistoryService.HISTORICAL_TTL)
        service.freshYear()

        assertEquals(2, pollenService.dailyHistoricalRequested.size)
    }

    @Test
    fun `a failed reload of the earlier years serves the rows kept from the earlier fetch`() = runTest {
        pollenService.dailyHistoricalBytes = dailyCsv(
            rows = listOf("31.12.2025" to mapOf(PollenSpecies.HAZEL to 7)),
        )
        val service = service()
        service.freshYear()

        clock.advanceBy(HistoryService.HISTORICAL_TTL)
        pollenService.dailyHistoricalFailure = IOException("upstream down")
        val result = service.historyFor(PollenStation.ZUERICH, HistoryRange.YEAR)

        val history = assertIs<CacheResult.Stale<StationHistory>>(result).value
        assertEquals(7, history.on(LocalDate.of(2025, 12, 31), PollenSpecies.HAZEL).concentration)
    }

    @Test
    fun `a year fails when the earlier years fail with nothing kept even though this year answers`() =
        runTest {
            val failure = IOException("upstream down")
            pollenService.dailyHistoricalFailure = failure

            val result = service().historyFor(PollenStation.ZUERICH, HistoryRange.YEAR)

            assertEquals(failure, assertIs<CacheResult.Failed>(result).cause)
            assertEquals(1, pollenService.dailyRecentRequested.size)
        }
}
