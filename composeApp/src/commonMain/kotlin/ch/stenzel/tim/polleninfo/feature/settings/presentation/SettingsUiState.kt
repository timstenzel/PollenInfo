package ch.stenzel.tim.polleninfo.feature.settings.presentation

import ch.stenzel.tim.polleninfo.core.appinfo.AppVersion
import ch.stenzel.tim.polleninfo.core.language.AppLanguage

/**
 * What the Settings screen shows beyond its fixed text. There is no `Loading` or `Error`: the one
 * value that comes from the network, the station's name, has a stored fallback, so the screen is
 * always content.
 *
 * [stationName] is the default station's name — from the station list once it has arrived, the
 * name stored with the selection until then or if it fails — and `null` only while nothing is
 * stored. [version] is `null` when the platform does not report one, and the version line is then
 * hidden.
 *
 * [language] is the app language as the platform reports it; [showLanguageDialog] whether its choice
 * dialog is open. [languageAppliesOnRestart] is set once the language has been changed on a platform
 * that only applies it at the next launch (iOS), and shows a note saying so.
 */
data class SettingsUiState(
    val stationName: String? = null,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val showLanguageDialog: Boolean = false,
    val languageAppliesOnRestart: Boolean = false,
    val version: AppVersion?,
)
