package ch.stenzel.tim.polleninfo.core.measurement.data.repository

import ch.stenzel.tim.polleninfo.core.measurement.data.mapper.toDomain
import ch.stenzel.tim.polleninfo.core.measurement.data.remote.StationMeasurementApiService
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.StationMeasurement
import ch.stenzel.tim.polleninfo.core.measurement.domain.repository.StationMeasurementRepository
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.safeCall

class StationMeasurementRepositoryImpl(
    private val apiService: StationMeasurementApiService,
) : StationMeasurementRepository {

    override suspend fun getMeasurement(stationAbbr: String): Result<StationMeasurement> =
        safeCall { apiService.getMeasurement(stationAbbr).toDomain() }
}
