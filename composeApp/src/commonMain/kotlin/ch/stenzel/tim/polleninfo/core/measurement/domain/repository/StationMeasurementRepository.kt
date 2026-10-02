package ch.stenzel.tim.polleninfo.core.measurement.domain.repository

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.StationMeasurement
import ch.stenzel.tim.polleninfo.core.result.Result

interface StationMeasurementRepository {

    /** The latest reading our backend holds for [stationAbbr]. */
    suspend fun getMeasurement(stationAbbr: String): Result<StationMeasurement>
}
