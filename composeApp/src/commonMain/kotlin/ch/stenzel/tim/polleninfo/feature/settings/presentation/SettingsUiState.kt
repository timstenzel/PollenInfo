package ch.stenzel.tim.polleninfo.feature.settings.presentation

import ch.stenzel.tim.polleninfo.core.appinfo.AppVersion
import ch.stenzel.tim.polleninfo.core.language.AppLanguage
import ch.stenzel.tim.polleninfo.core.result.AppError

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
 *
 * [accountEmail] is the address of the account this phone is logged in to, `null` while nobody is;
 * the account rows appear only once [accountChecked] — the stored login has been read — so "Not
 * logged in" never flashes up for a logged-in user. [showLogoutDialog] is whether the logout
 * confirmation is open.
 *
 * [showDeleteDialog] is whether the account-deletion dialog is open, with the password typed into it
 * ([deletePassword], shown in clear text while [deletePasswordVisible]); [isDeleting] while the
 * deletion runs, and [deleteError] why the last attempt failed — a wrong password among them — until
 * the next edit or attempt.
 */
data class SettingsUiState(
    val stationName: String? = null,
    val accountEmail: String? = null,
    val accountChecked: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val deletePassword: String = "",
    val deletePasswordVisible: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: AppError? = null,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val showLanguageDialog: Boolean = false,
    val languageAppliesOnRestart: Boolean = false,
    val version: AppVersion?,
) {
    /** A password has been typed and no deletion is running. */
    val canConfirmDelete: Boolean get() = deletePassword.isNotEmpty() && !isDeleting
}
