package ch.stenzel.tim.polleninfo.feature.settings.presentation

import ch.stenzel.tim.polleninfo.core.appinfo.AppVersion

/**
 * What the Settings screen shows beyond its fixed text. There is no `Loading` or `Error`: nothing on
 * the screen depends on the network, so it is always content.
 *
 * [version] is `null` when the platform does not report one, and the version line is then hidden.
 */
data class SettingsUiState(
    val version: AppVersion?,
)
