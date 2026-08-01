package ch.stenzel.tim.polleninfo.feature.home.domain.repository

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.home.domain.model.StationMeasurement

interface StationMeasurementRepository {

    /** The latest reading our backend holds for [stationAbbr]. */
    suspend fun getMeasurement(stationAbbr: String): Result<StationMeasurement>
}
