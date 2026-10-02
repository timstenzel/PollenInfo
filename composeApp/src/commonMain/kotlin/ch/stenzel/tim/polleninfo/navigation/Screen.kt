package ch.stenzel.tim.polleninfo.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    /** Destination of the reference feature in `feature/example`. */
    @Serializable
    data object Example : Screen

    /** One-time station setup, shown on first launch. */
    @Serializable
    data object Onboarding : Screen

    /** The dashboard: readings for the station chosen during setup. The first bottom-bar tab. */
    @Serializable
    data object Home : Screen

    // Tabs 2–5 are reserved for features not yet defined and show `ComingSoonScreen` until then.
    // Separate objects rather than one parameterised route, so each tab keeps its own saved state
    // and one can be renamed to a real feature without touching the others.

    @Serializable
    data object Feature2 : Screen

    @Serializable
    data object Feature3 : Screen

    @Serializable
    data object Feature4 : Screen

    @Serializable
    data object Feature5 : Screen
}
