package ch.stenzel.tim.polleninfo.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    /** Destination of the reference feature in `feature/example`. */
    @Serializable
    data object Example : Screen

    /** One-time station setup, shown on first launch. */
    @Serializable
    data object Onboarding : Screen

    /** Placeholder main screen — stands in for the real dashboard. */
    @Serializable
    data object Home : Screen
}
