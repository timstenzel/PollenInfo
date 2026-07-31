package ch.stenzel.tim.polleninfo.feature.onboarding.presentation

/**
 * One-shot things the onboarding screen must *do*, as opposed to things it must *show*.
 *
 * **Why this is not a flag on `OnboardingUiState`** — and this is the project's first use of the
 * pattern, so it is worth stating: a boolean like `isComplete` describes a state, but "navigate
 * away" is an action that must happen exactly once. A state flag re-fires on every re-emission and
 * every recomposition that reads it, so the screen would navigate again on a configuration change
 * or on any later state update, and it would have to be cleared afterwards — bookkeeping that only
 * exists to work around modelling the action as state. A `Channel` delivers each event to exactly
 * one collector exactly once, and nothing has to be reset.
 *
 * Use this for navigation, one-off snackbars and the like. Anything the screen should keep
 * displaying stays in `OnboardingUiState`.
 */
sealed interface OnboardingEvent {
    /** The selection was persisted; the screen may leave onboarding. */
    data object Completed : OnboardingEvent
}
