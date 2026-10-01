package ch.stenzel.tim.polleninfo.server.pollen.measurement

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.upstream.ParsedReading
import ch.stenzel.tim.polleninfo.server.pollen.upstream.PollenCsvParser
import ch.stenzel.tim.polleninfo.server.pollen.upstream.PollenService
import java.time.Duration

/**
 * Turns a station's published file into a classified reading.
 *
 * Deliberately thin — it fetches, parses, caches and classifies by delegating all four, and its own
 * logic is only the composition. Classification stays here rather than in either app because the
 * server is the single source of truth for what "High" means.
 *
 * What is cached is the parsed reading, not the classified one, so classification always uses the
 * current [thresholds].
 */
class MeasurementService(
    private val pollenService: PollenService,
    private val thresholds: PollenThresholds,
    private val cache: TtlCache<PollenStation, ParsedReading> = TtlCache(CACHE_TTL),
) {

    /**
     * The station's latest usable reading.
     *
     * [CacheResult.Stale] is a reading kept from an earlier fetch because the latest one failed; it
     * carries its own, older `measuredAt`. [CacheResult.Failed] carries a [NoUsableRowException]
     * when the published file had no row with a value, and whatever [PollenService] threw when the
     * file could not be obtained.
     */
    suspend fun measurementFor(station: PollenStation): CacheResult<StationMeasurement> =
        when (val result = cache.get(station, ::fetchReading)) {
            is CacheResult.Fresh -> CacheResult.Fresh(classify(station, result.value))
            is CacheResult.Stale -> CacheResult.Stale(classify(station, result.value))
            is CacheResult.Failed -> result
        }

    /**
     * A file with no usable row is a failed load rather than a value, so a reading from earlier
     * still covers it. The stale warning in the app says how old that reading is, which is more use
     * than "no reading".
     */
    private suspend fun fetchReading(station: PollenStation): ParsedReading =
        PollenCsvParser.parseHourly(pollenService.hourlyNow(station))
            ?: throw NoUsableRowException(station)

    private fun classify(station: PollenStation, reading: ParsedReading) = StationMeasurement(
        station = station,
        measuredAt = reading.measuredAt,
        // Driven by the enum, not by the parsed map's keys: all seven taxa are present in
        // declaration order whatever the file happened to contain a column for.
        species = PollenSpecies.entries.map { species ->
            val concentration = reading.concentrations[species]
            SpeciesMeasurement(
                species = species,
                concentration = concentration,
                // No reading classifies to no severity — never to NONE, which would report an
                // unmeasured taxon as measured and clean.
                severity = concentration?.let { thresholds.severityOf(species, it) },
            )
        },
    )

    companion object {
        /**
         * The upstream file gains a row roughly hourly; thirty minutes picks up a new row within
         * half an hour of publication without a schedule that could drift out of phase with it.
         */
        val CACHE_TTL: Duration = Duration.ofMinutes(30)
    }
}

/** The station's published file was fetched but holds no row with any value. */
class NoUsableRowException(station: PollenStation) :
    Exception("${station.abbr}: published file holds no usable row")
