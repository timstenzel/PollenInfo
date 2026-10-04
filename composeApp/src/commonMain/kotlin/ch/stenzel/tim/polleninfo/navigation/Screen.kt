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

    /** Every station's current reading side by side. The second bottom-bar tab. */
    @Serializable
    data object AllStations : Screen

    /** The user's pollen alarms, behind the notification permission. The fourth bottom-bar tab. */
    @Serializable
    data object Alarms : Screen

    /**
     * Creates an alarm, or edits the one with [alarmId]. Not a tab, so the bottom bar is hidden on
     * it.
     */
    @Serializable
    data class AlarmEditor(val alarmId: String? = null) : Screen

    @Serializable
    data object Diary : Screen

    // Tab 5 is reserved for a feature not yet defined and shows `ComingSoonScreen` until then. A
    // separate object rather than a parameterised route, so the tab keeps its own saved state and
    // can be renamed to a real feature without touching the others.

    @Serializable
    data object Feature5 : Screen
}
