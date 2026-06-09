package ch.stenzel.tim.polleninfo.feature.pollenforecast.presentation

import ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.model.PollenForecast

sealed interface PollenForecastUiState {
    data object Loading : PollenForecastUiState

    data class Content(
        val forecast: PollenForecast,
        val isRefreshing: Boolean = false,
    ) : PollenForecastUiState

    data class Error(
        val exception: Exception,
        val message: String = exception.message ?: "An unexpected error occurred",
    ) : PollenForecastUiState
}
