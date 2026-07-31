package ch.stenzel.tim.polleninfo.feature.example.presentation

import ch.stenzel.tim.polleninfo.feature.example.domain.model.PollenSnapshot

sealed interface ExampleUiState {
    data object Loading : ExampleUiState

    data class Content(
        val snapshot: PollenSnapshot,
        val isRefreshing: Boolean = false,
    ) : ExampleUiState

    data class Error(
        val exception: Exception,
        val message: String = exception.message ?: "An unexpected error occurred",
    ) : ExampleUiState
}
