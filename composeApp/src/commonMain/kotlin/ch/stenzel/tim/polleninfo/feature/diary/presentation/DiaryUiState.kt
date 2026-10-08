package ch.stenzel.tim.polleninfo.feature.diary.presentation

import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.core.history.domain.model.StationHistory
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.species.domain.model.Species
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station

sealed interface DiaryUiState {

    data object Loading : DiaryUiState

    /**
     * A history on screen. [isLoading] is set while a new history replaces this one; [history] stays
     * on screen until then.
     *
     * [stationAbbr] and [range] are what the user has selected; [history] (with its own
     * `stationAbbr`) and [historyRange] are what is drawn, which the chart is laid out and announced
     * for. The two differ only while a change is loading.
     *
     * [species] are every pollen type, in the backend's order — one checkbox each. [checked] holds
     * the ids the user wants drawn, including any the station does not measure, so the choice
     * survives moving to a station that does.
     *
     * [entries] are the user's answers on the days [history] covers — never today's, which no
     * history reaches — sorted by date. They follow [history], not [range], so the feeling line
     * always matches the pollen lines drawn with it.
     */
    data class Content(
        val stations: List<Station>,
        val stationAbbr: String,
        val range: HistoryRange,
        val historyRange: HistoryRange,
        val history: StationHistory,
        val species: List<Species>,
        val checked: Set<String>,
        val entries: List<DiaryEntry> = emptyList(),
        val isLoading: Boolean = false,
    ) : DiaryUiState {
        /** No answer in the period: the chart shows the hint pointing to Home's question. */
        val hasNoEntries: Boolean get() = entries.isEmpty()

        /**
         * The pollen types with no value on any day of [history] — "Not measured here". A station
         * that does not report a type is not a station reporting none of it.
         */
        val notMeasured: Set<String>
            get() = species.map { it.id }.filterTo(mutableSetOf()) { id -> history.days.all { it.levels[id] == null } }

        /** The lines to draw: checked and measured, in [species] order. */
        val shownSpeciesIds: List<String>
            get() {
                val notMeasured = notMeasured
                return species.map { it.id }.filter { it in checked && it !in notMeasured }
            }
    }

    data class Error(val error: AppError) : DiaryUiState
}
