package ch.stenzel.tim.polleninfo.feature.alarms.presentation

/** One-shot instructions to the Alarms screen — see `OnboardingEvent` for why these are not state. */
sealed interface AlarmsEvent {

    /** Switching an alarm on or off failed and its switch is back where it was; say so once. */
    data class ToggleFailed(val message: String) : AlarmsEvent
}
