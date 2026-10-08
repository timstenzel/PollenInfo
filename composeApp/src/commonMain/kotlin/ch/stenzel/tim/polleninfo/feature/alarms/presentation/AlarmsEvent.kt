package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import ch.stenzel.tim.polleninfo.core.result.AppError

/** One-shot instructions to the Alarms screen — see `OnboardingEvent` for why these are not state. */
sealed interface AlarmsEvent {

    /**
     * Switching an alarm on ([enabling]) or off failed and its switch is back where it was; say so
     * once.
     */
    data class ToggleFailed(val error: AppError, val enabling: Boolean) : AlarmsEvent
}
