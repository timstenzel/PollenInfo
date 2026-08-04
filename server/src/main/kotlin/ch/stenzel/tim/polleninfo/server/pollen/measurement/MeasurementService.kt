package ch.stenzel.tim.polleninfo.server.pollen.measurement

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.upstream.PollenCsvParser
import ch.stenzel.tim.polleninfo.server.pollen.upstream.PollenService

/**
 * Turns a station's published file into a classified reading.
 *
 * Deliberately thin — it fetches, parses and classifies by delegating all three, and its own logic
 * is only the composition. Classification stays here rather than in either app because the server
 * is the single source of truth for what "High" means.
 */
class MeasurementService(
    private val pollenService: PollenService,
    private val thresholds: PollenThresholds,
) {

    /**
     * The station's latest usable reading, or `null` when its published file contains no row with
     * a value.
     *
     * Propagates whatever [PollenService] throws: measurements that could not be obtained are not
     * a station with nothing to report.
     */
    suspend fun measurementFor(station: PollenStation): StationMeasurement? {
        val reading = PollenCsvParser.parseHourly(pollenService.hourlyNow(station)) ?: return null

        return StationMeasurement(
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
    }
}
