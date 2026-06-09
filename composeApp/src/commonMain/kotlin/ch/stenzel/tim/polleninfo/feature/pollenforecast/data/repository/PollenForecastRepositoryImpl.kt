package ch.stenzel.tim.polleninfo.feature.pollenforecast.data.repository

import ch.stenzel.tim.polleninfo.core.result.AppResult
import ch.stenzel.tim.polleninfo.core.result.safeCall
import ch.stenzel.tim.polleninfo.feature.pollenforecast.data.mapper.toDomain
import ch.stenzel.tim.polleninfo.feature.pollenforecast.data.remote.PollenApiService
import ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.model.PollenForecast
import ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.repository.PollenForecastRepository

class PollenForecastRepositoryImpl(
    private val apiService: PollenApiService,
) : PollenForecastRepository {

    override suspend fun getPollenForecast(
        latitude: Double,
        longitude: Double,
    ): AppResult<PollenForecast> = safeCall {
        apiService.getAirQuality(latitude, longitude).toDomain()
    }
}
