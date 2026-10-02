package ch.stenzel.tim.polleninfo.feature.onboarding.presentation

import ch.stenzel.tim.polleninfo.core.station.domain.model.Station

sealed interface OnboardingUiState {
    data object Loading : OnboardingUiState

    /**
     * The screen can do its job: [stations] is the list to offer, [selected] the user's current
     * pick (`null` until they make one).
     *
     * [isLocating] mirrors the established `isRefreshing` idea — the location button shows a spinner
     * and is disabled, while the dropdown and Continue stay usable.
     *
     * [locationError] lives **here**, not in [Error]: a failed lookup leaves the screen perfectly
     * usable, and the whole point of its message is "pick a station manually", so surfacing it as
     * [Error] would blank out the very dropdown the message recommends.
     *
     * [saveError] is a failure the screen must keep *showing*; it deliberately does not stand for
     * "already navigated" — completion is a one-shot [OnboardingEvent], not a state flag.
     */
    data class Content(
        val stations: List<Station>,
        val selected: Station? = null,
        val isLocating: Boolean = false,
        val locationError: LocationError? = null,
        val saveError: Boolean = false,
    ) : OnboardingUiState

    /**
     * The screen cannot do its job at all — the station list could not be retrieved, so there is
     * nothing to pick from and only a retry makes sense.
     */
    data class Error(
        val exception: Exception,
        val message: String = exception.message ?: "An unexpected error occurred",
    ) : OnboardingUiState
}

/**
 * Exactly two ways the location shortcut can fail, as far as the user is concerned.
 *
 * Everything else the platforms can report — no provider, location services off at device level, no
 * fix before the timeout, a delegate error — folds into [UNAVAILABLE]. That coarseness is
 * deliberate: a third message would have to explain a distinction the user cannot act on
 * differently, since the answer is always "pick a station manually".
 */
enum class LocationError { PERMISSION_DENIED, UNAVAILABLE }
