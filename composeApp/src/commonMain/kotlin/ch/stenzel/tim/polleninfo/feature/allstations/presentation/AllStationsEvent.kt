package ch.stenzel.tim.polleninfo.feature.allstations.presentation

/**
 * One-shot things the All stations screen must *do*, as opposed to things it must *show* — see
 * `OnboardingEvent` for why these are not flags on the UI state.
 */
sealed interface AllStationsEvent {
    /**
     * [abbr] was just selected; bring its expanded row into view. Sent once per selection, so the
     * list does not jump back to it on recomposition or on returning to the tab.
     */
    data class ScrollToStation(val abbr: String) : AllStationsEvent
}
