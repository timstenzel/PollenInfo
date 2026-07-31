package ch.stenzel.tim.polleninfo.core.startup

/**
 * Whether the app already knows which station the user picked.
 *
 * Deliberately **not** a navigation destination: [Loading] is a moment before the graph exists, not
 * a place the user can be at or navigate back to. Modelling it as a screen would put a splash
 * destination on the back stack that `popUpTo` bookkeeping would then have to remove again.
 */
sealed interface StartupState {
    /** The stored selection has not been read yet. */
    data object Loading : StartupState

    /** Nothing stored — the app starts at onboarding. */
    data object NeedsOnboarding : StartupState

    /** A station is stored — the app starts at its main content. */
    data object Ready : StartupState
}
