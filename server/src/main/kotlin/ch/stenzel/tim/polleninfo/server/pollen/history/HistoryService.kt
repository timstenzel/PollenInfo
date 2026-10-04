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
 * Reads the station's year-to-date daily file through a [TtlCache] of its own, cached for
 * [RECENT_TTL]: the file gains one row a day, so a few hours bound how late a new day appears while
 * keeping the cost at a handful of upstream requests per station per day however many people look.
 * What is cached is the parsed rows; classification happens per request so a threshold change
 * applies at once.
 *
 * Failures follow [ch.stenzel.tim.polleninfo.server.pollen.measurement.MeasurementService]: rows
 * kept from an earlier fetch are served when a reload fails (dates they do not cover yet come back
 * empty), and with nothing kept the result is [CacheResult.Failed].
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

    suspend fun historyFor(station: PollenStation, range: HistoryRange): CacheResult<StationHistory> {
        val window = historyWindow(range, today = clock.instant().atZone(SWISS_ZONE).toLocalDate())
        val recent = recentCache.get(station) { PollenCsvParser.parseDaily(pollenService.dailyRecent(it)) }
        return when (recent) {
            is CacheResult.Fresh -> CacheResult.Fresh(classify(station, range, window, recent.value))
            is CacheResult.Stale -> CacheResult.Stale(classify(station, range, window, recent.value))
            is CacheResult.Failed -> recent
        }
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
    }
}
