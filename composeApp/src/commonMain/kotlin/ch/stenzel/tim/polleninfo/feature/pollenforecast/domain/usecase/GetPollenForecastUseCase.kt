package ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.usecase

import ch.stenzel.tim.polleninfo.core.result.AppResult
import ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.model.PollenForecast
import ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.repository.PollenForecastRepository

class GetPollenForecastUseCase(
    private val repository: PollenForecastRepository,
) {
    suspend operator fun invoke(
        latitude: Double = 47.3769,
        longitude: Double = 8.5417,
    ): AppResult<PollenForecast> = repository.getPollenForecast(latitude, longitude)
}
