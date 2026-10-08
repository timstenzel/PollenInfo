package ch.stenzel.tim.polleninfo.feature.onboarding.presentation

import ch.stenzel.tim.polleninfo.core.stationpicker.presentation.StationPickerState

/**
 * The shared [picker] — list, pick, location shortcut, list error — plus what onboarding adds.
 *
 * [saveError] is a failure the screen must keep *showing*; it deliberately does not stand for
 * "already navigated" — completion is a one-shot [OnboardingEvent], not a state flag. It is a
 * `Boolean`, not an `AppError`: the write is local storage, and there is one thing to say about it.
 */
data class OnboardingUiState(
    val picker: StationPickerState = StationPickerState.Loading,
    val saveError: Boolean = false,
)
