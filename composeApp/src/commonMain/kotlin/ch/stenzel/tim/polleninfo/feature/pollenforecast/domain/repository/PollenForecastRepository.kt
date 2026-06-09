package ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.repository

import ch.stenzel.tim.polleninfo.core.result.AppResult
import ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.model.PollenForecast

interface PollenForecastRepository {
    suspend fun getPollenForecast(latitude: Double, longitude: Double): AppResult<PollenForecast>
}
