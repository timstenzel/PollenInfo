package ch.stenzel.tim.polleninfo.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    /** Destination of the reference feature in `feature/example`. */
    @Serializable
    data object Example : Screen
}
