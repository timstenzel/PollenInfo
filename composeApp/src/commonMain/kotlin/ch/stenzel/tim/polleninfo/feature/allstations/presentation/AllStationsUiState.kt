package ch.stenzel.tim.polleninfo.feature.allstations.presentation

import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.feature.allstations.domain.model.StationReading
import kotlin.time.Instant

/** What the All stations screen shows. */
sealed interface AllStationsUiState {

    /** The station list is not known yet. Readings never hold this up — see [Content]. */
    data object Loading : AllStationsUiState

    /**
     * Shown as soon as the station list is known, with every reading still pending; each entry of
     * [stations] is replaced as its own reading resolves. Alphabetical by name, never reordered.
     *
     * [selectedAbbr] is the expanded station, `null` when none is. [refreshedAt] is when the last
     * round of readings completed, `null` until the first one has. [isRefreshing] keeps the current
     * readings on screen while a pull-to-refresh runs.
     */
    data class Content(
        val stations: List<StationReading>,
        val selectedAbbr: String? = null,
        val refreshedAt: Instant? = null,
        val isRefreshing: Boolean = false,
    ) : AllStationsUiState

    data class Error(val error: AppError) : AllStationsUiState
}
