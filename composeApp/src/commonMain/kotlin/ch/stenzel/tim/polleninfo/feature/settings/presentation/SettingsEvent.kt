package ch.stenzel.tim.polleninfo.feature.settings.presentation

/**
 * One-shot instructions to the Settings screen — see `OnboardingEvent` for why these are not state.
 * Each concerns only this screen, which shows it in its own snackbar.
 */
sealed interface SettingsEvent {

    /** The crash-report choice could not be stored and the switch is back where it was; say so once. */
    data object CrashReportsSaveFailed : SettingsEvent
}
