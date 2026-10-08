package ch.stenzel.tim.polleninfo.feature.settings.presentation

import ch.stenzel.tim.polleninfo.core.stationpicker.presentation.StationPickerState
import ch.stenzel.tim.polleninfo.core.stationpicker.presentation.selected

/**
 * The shared [picker] plus what changing the default adds: the station stored when the screen
 * opened ([storedAbbr], read once), the save in flight and its failure.
 *
 * [saveError] is a `Boolean`, not an `AppError`, as on onboarding: the write is local storage.
 */
data class ChangeStationUiState(
    val picker: StationPickerState = StationPickerState.Loading,
    val storedAbbr: String? = null,
    val isSaving: Boolean = false,
    val saveError: Boolean = false,
) {
    /** A pick that differs from the stored station, and no save already running. */
    val canSave: Boolean
        get() = picker.selected.let { it != null && it.abbr != storedAbbr } && !isSaving
}

/** One-shot things the change-station screen must do — see `OnboardingEvent` for the pattern. */
sealed interface ChangeStationEvent {
    /** The new station was stored; the screen may close. */
    data object Done : ChangeStationEvent
}
