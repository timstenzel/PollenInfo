package ch.stenzel.tim.polleninfo.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object PollenForecast : Screen
}
