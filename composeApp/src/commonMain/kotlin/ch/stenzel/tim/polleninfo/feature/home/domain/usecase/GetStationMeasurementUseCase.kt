package ch.stenzel.tim.polleninfo.feature.home.domain.usecase

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.map
import ch.stenzel.tim.polleninfo.feature.home.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.feature.home.domain.model.SpeciesReading
import ch.stenzel.tim.polleninfo.feature.home.domain.model.StationMeasurement
import ch.stenzel.tim.polleninfo.feature.home.domain.model.StationPollenOverview
import ch.stenzel.tim.polleninfo.feature.home.domain.repository.StationMeasurementRepository

/**
 * Fetches a station's reading and derives what the screen shows on top of it.
 *
 * The derivation lives here rather than in the ViewModel or a composable so it can be tested
 * without either.
 */
class GetStationMeasurementUseCase(
    private val repository: StationMeasurementRepository,
) {

    suspend operator fun invoke(stationAbbr: String): Result<StationPollenOverview> =
        repository.getMeasurement(stationAbbr).map { it.toOverview() }

    private fun StationMeasurement.toOverview(): StationPollenOverview {
        val ordered = species.sortedWith(DISPLAY_ORDER)
        // The display order already puts the worst measured taxon first, so the driver and the
        // overall severity are read off the same list rather than computed a second way.
        val drivenBy = ordered.firstOrNull { it.severity != null }
        return StationPollenOverview(
            measuredAt = measuredAt,
            unit = unit,
            overallSeverity = overallSeverityOf(drivenBy),
            drivenBy = drivenBy,
            species = ordered,
        )
    }

    /**
     * The worst severity among the taxa that **have** a reading.
     *
     * Taxa the station does not measure are skipped rather than counted as [PollenSeverity.NONE]:
     * an unmeasured taxon is not evidence of clean air, and letting it participate would be
     * meaningless anyway since it can only ever lower the maximum. Reading the *worst* rather than
     * an average is the point — a severe reading must never hide behind six mild ones.
     *
     * The fallback covers a reading in which nothing was measured at all. The endpoint answers 404
     * for that case, so it does not arise in practice; it is spelled out rather than forced because
     * a crash is a worse answer than a calm one.
     */
    private fun overallSeverityOf(drivenBy: SpeciesReading?): PollenSeverity =
        drivenBy?.severity ?: PollenSeverity.NONE

    private companion object {
        /**
         * Worst first, so what matters is at the top. Taxa with no reading sink to the bottom
         * rather than being hidden — someone who reacts to ash needs to see that ash is unmeasured
         * here. The alphabetical tie-break, applied within the unmeasured group too, keeps the list
         * from reshuffling between visits.
         */
        val DISPLAY_ORDER: Comparator<SpeciesReading> =
            compareBy<SpeciesReading, PollenSeverity?>(nullsLast(reverseOrder())) { it.severity }
                .thenBy { it.name }
    }
}
