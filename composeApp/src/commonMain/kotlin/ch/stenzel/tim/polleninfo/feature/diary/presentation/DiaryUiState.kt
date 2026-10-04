package ch.stenzel.tim.polleninfo.feature.diary.presentation

import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.core.history.domain.model.StationHistory

sealed interface DiaryUiState {

    data object Loading : DiaryUiState

    /**
     * A history on screen. [speciesIds] are the species to draw, in the backend's order.
     * [isLoading] is set while a new history replaces this one; [history] stays on screen until then.
     */
    data class Content(
        val stationAbbr: String,
        val stationName: String,
        val range: HistoryRange,
        val history: StationHistory,
        val speciesIds: List<String>,
        val isLoading: Boolean = false,
    ) : DiaryUiState

    data class Error(val message: String) : DiaryUiState
}
