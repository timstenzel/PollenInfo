package ch.stenzel.tim.polleninfo.feature.home.domain.usecase

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.map
import ch.stenzel.tim.polleninfo.feature.home.domain.model.PollenSeverity
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

    private fun StationMeasurement.toOverview() = StationPollenOverview(
        unit = unit,
        overallSeverity = overallSeverityOf(this),
        species = species,
    )

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
    private fun overallSeverityOf(measurement: StationMeasurement): PollenSeverity =
        measurement.species.mapNotNull { it.severity }.maxOrNull() ?: PollenSeverity.NONE
}
