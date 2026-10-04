package ch.stenzel.tim.polleninfo.feature.alarms.presentation

/** One-shot instructions to the editor screen — see `OnboardingEvent` for why these are not state. */
sealed interface AlarmEditorEvent {

    /** The alarm is saved: leave the editor, back to the list. */
    data object Done : AlarmEditorEvent
}
