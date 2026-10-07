package ch.stenzel.tim.polleninfo.feature.diary.presentation

import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.core.history.domain.model.StationHistory

sealed interface DiaryUiState {

    data object Loading : DiaryUiState

    /**
     * A history on screen. [speciesIds] are the species to draw, in the backend's order.
     * [isLoading] is set while a new history replaces this one; [history] stays on screen until then.
     *
     * [range] is the period the user has selected; [historyRange] is the one [history] covers, which
     * the chart is laid out and announced for. The two differ only while a range change is loading.
     *
     * [entries] are the user's answers on the days [history] covers — never today's, which no
     * history reaches — sorted by date. They follow [history], not [range], so the feeling line
     * always matches the pollen lines drawn with it.
     */
    data class Content(
        val stationAbbr: String,
        val stationName: String,
        val range: HistoryRange,
        val historyRange: HistoryRange,
        val history: StationHistory,
        val speciesIds: List<String>,
        val entries: List<DiaryEntry> = emptyList(),
        val isLoading: Boolean = false,
    ) : DiaryUiState {
        /** No answer in the period: the chart shows the hint pointing to Home's question. */
        val hasNoEntries: Boolean get() = entries.isEmpty()
    }

    data class Error(val message: String) : DiaryUiState
}
