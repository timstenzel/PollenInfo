package ch.stenzel.tim.polleninfo.core.stationpicker.presentation

import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station

sealed interface StationPickerState {
    data object Loading : StationPickerState

    /**
     * The picker can do its job: [stations] is the list to offer, [selected] the current pick
     * (`null` until there is one).
     *
     * [isLocating] mirrors the established `isRefreshing` idea — the location button shows a spinner
     * and is disabled, while the dropdown and the screen's confirm button stay usable.
     *
     * [locationError] lives **here**, not in [Error]: a failed lookup leaves the picker perfectly
     * usable, and the whole point of its message is "pick a station manually", so surfacing it as
     * [Error] would blank out the very dropdown the message recommends.
     */
    data class Content(
        val stations: List<Station>,
        val selected: Station? = null,
        val isLocating: Boolean = false,
        val locationError: LocationError? = null,
    ) : StationPickerState

    /**
     * The picker cannot do its job at all — the station list could not be retrieved, so there is
     * nothing to pick from and only a retry makes sense.
     */
    data class Error(val error: AppError) : StationPickerState
}

/** The current pick, or `null` while there is none or no list to pick from. */
val StationPickerState.selected: Station?
    get() = (this as? StationPickerState.Content)?.selected

/**
 * Exactly two ways the location shortcut can fail, as far as the user is concerned.
 *
 * Everything else the platforms can report — no provider, location services off at device level, no
 * fix before the timeout, a delegate error — folds into [UNAVAILABLE]. That coarseness is
 * deliberate: a third message would have to explain a distinction the user cannot act on
 * differently, since the answer is always "pick a station manually".
 */
enum class LocationError { PERMISSION_DENIED, UNAVAILABLE }
