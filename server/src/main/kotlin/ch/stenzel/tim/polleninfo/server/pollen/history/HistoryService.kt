package ch.stenzel.tim.polleninfo.server.pollen.history

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.domain.SWISS_ZONE
import ch.stenzel.tim.polleninfo.server.pollen.measurement.CacheResult
import ch.stenzel.tim.polleninfo.server.pollen.measurement.SpeciesMeasurement
import ch.stenzel.tim.polleninfo.server.pollen.measurement.TtlCache
import ch.stenzel.tim.polleninfo.server.pollen.upstream.PollenCsvParser
import ch.stenzel.tim.polleninfo.server.pollen.upstream.PollenService
import java.time.Clock
import java.time.Duration
import java.time.LocalDate

/** A station's published daily rows, as parsed: date → taxon → daily mean, `null` for no value. */
private typealias DailyRows = Map<LocalDate, Map<PollenSpecies, Int?>>

/** One day of a station's history: every taxon's daily mean, classified. */
data class HistoryDay(
    val date: LocalDate,
    /** All seven taxa in [PollenSpecies] order; `null` concentration and severity mean no value. */
    val species: List<SpeciesMeasurement>,
)

/** A station's daily history over one [HistoryRange]: one [HistoryDay] per date, oldest first. */
data class StationHistory(
    val station: PollenStation,
    val range: HistoryRange,
    val from: LocalDate,
    val until: LocalDate,
    val days: List<HistoryDay>,
)

/**
 * A station's daily pollen levels over the last week, month or year, classified with the same
 * [thresholds] as every reading. The daily means are what those bands are defined over, so unlike
 * the hourly measurements this does not skew high.
 *
 * Reads two published files, each through a [TtlCache] of its own:
 *
 * - the year-to-date file, always, cached for [RECENT_TTL]: it gains one row a day, so a few hours
 *   bound how late a new day appears while keeping the cost at a handful of upstream requests per
 *   station per day however many people look;
 * - the file of earlier years, only when the window starts before 1 January of the current Swiss
 *   year, cached for [HISTORICAL_TTL]: it changes once a year, and at around half a megabyte it is
 *   not worth fetching more often.
 *
 * On a date both files hold, the year-to-date value wins. What is cached is the parsed rows;
 * classification happens per request so a threshold change applies at once.
 *
 * Failures follow [ch.stenzel.tim.polleninfo.server.pollen.measurement.MeasurementService], per
 * file: rows kept from an earlier fetch are served when a reload fails (dates they do not cover yet
 * come back empty), and the result is [CacheResult.Stale] if either file was. If a file the window
 * needs has failed with nothing kept, the whole result is [CacheResult.Failed] — a year with a
 * silent hole where last year should be would read as a clean season.
 *
 * "Today" is the Swiss date ([SWISS_ZONE]) of [clock]'s instant, since the publisher's days are
 * Swiss days.
 */
class HistoryService(
    private val pollenService: PollenService,
    private val thresholds: PollenThresholds,
    private val clock: Clock = Clock.systemUTC(),
) {

    private val recentCache = TtlCache<PollenStation, DailyRows>(RECENT_TTL, clock)
    private val historicalCache = TtlCache<PollenStation, DailyRows>(HISTORICAL_TTL, clock)

    suspend fun historyFor(station: PollenStation, range: HistoryRange): CacheResult<StationHistory> {
        val today = today()
        val window = historyWindow(range, today)
        val results = buildList {
            add(recentCache.get(station) { PollenCsvParser.parseDaily(pollenService.dailyRecent(it)) })
            if (window.start < today.withDayOfYear(1)) {
                add(historicalCache.get(station) { loadHistorical(it) })
            }
        }
        // Historical first, so a recent row replaces it on the same date.
        val rows = results.asReversed().fold(emptyMap<LocalDate, Map<PollenSpecies, Int?>>()) { merged, result ->
            when (result) {
                is CacheResult.Fresh -> merged + result.value
                is CacheResult.Stale -> merged + result.value
                is CacheResult.Failed -> return result
            }
        }
        val history = classify(station, range, window, rows)
        return if (results.any { it is CacheResult.Stale }) CacheResult.Stale(history) else CacheResult.Fresh(history)
    }

    private fun today(): LocalDate = clock.instant().atZone(SWISS_ZONE).toLocalDate()

    /**
     * The file of earlier years reaches back to the 1980s, but no window can start before
     * 1 January of last year, so only rows from then on are kept — the rest would sit in memory
     * for every station and never be read.
     */
    private suspend fun loadHistorical(station: PollenStation): DailyRows {
        val firstUsable = today().minusYears(1).withDayOfYear(1)
        return PollenCsvParser.parseDaily(pollenService.dailyHistorical(station))
            .filterKeys { it >= firstUsable }
    }

    /**
     * Every date of [window] gets a day, whether or not the file has a row for it: the publisher
     * leaves days out altogether, and a client must see the gap where it is rather than a shorter
     * list whose dates it has to line up itself.
     */
    private fun classify(
        station: PollenStation,
        range: HistoryRange,
        window: ClosedRange<LocalDate>,
        rows: DailyRows,
    ) = StationHistory(
        station = station,
        range = range,
        from = window.start,
        until = window.endInclusive,
        days = window.start.datesUntil(window.endInclusive.plusDays(1)).toList().map { date ->
            val row = rows[date]
            HistoryDay(
                date = date,
                species = PollenSpecies.entries.map { species ->
                    val concentration = row?.get(species)
                    SpeciesMeasurement(
                        species = species,
                        concentration = concentration,
                        severity = concentration?.let { thresholds.severityOf(species, it) },
                    )
                },
            )
        },
    )

    companion object {
        /**
         * The year-to-date file gains one row a day, published some time after midnight. Three hours
         * bound how long yesterday can stay missing after it appears, at eight requests per station
         * per day at most.
         */
        val RECENT_TTL: Duration = Duration.ofHours(3)

        /**
         * The file of earlier years changes once a year, when the old year moves out of the
         * year-to-date file. A day is plenty; the fetch is about half a megabyte per station.
         */
        val HISTORICAL_TTL: Duration = Duration.ofHours(24)
    }
}
