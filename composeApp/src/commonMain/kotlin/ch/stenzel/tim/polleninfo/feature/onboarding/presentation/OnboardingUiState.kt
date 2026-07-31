package ch.stenzel.tim.polleninfo.feature.onboarding.presentation

import ch.stenzel.tim.polleninfo.feature.onboarding.domain.model.Station

sealed interface OnboardingUiState {
    data object Loading : OnboardingUiState

    /**
     * The screen can do its job: [stations] is the list to offer, [selected] the user's current
     * pick (`null` until they make one).
     */
    data class Content(
        val stations: List<Station>,
        val selected: Station? = null,
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
