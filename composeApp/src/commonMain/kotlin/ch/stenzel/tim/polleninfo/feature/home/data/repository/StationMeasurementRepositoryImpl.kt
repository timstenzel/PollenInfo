package ch.stenzel.tim.polleninfo.feature.home.data.repository

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.safeCall
import ch.stenzel.tim.polleninfo.feature.home.data.mapper.toDomain
import ch.stenzel.tim.polleninfo.feature.home.data.remote.StationMeasurementApiService
import ch.stenzel.tim.polleninfo.feature.home.domain.model.StationMeasurement
import ch.stenzel.tim.polleninfo.feature.home.domain.repository.StationMeasurementRepository

class StationMeasurementRepositoryImpl(
    private val apiService: StationMeasurementApiService,
) : StationMeasurementRepository {

    override suspend fun getMeasurement(stationAbbr: String): Result<StationMeasurement> =
        safeCall { apiService.getMeasurement(stationAbbr).toDomain() }
}
