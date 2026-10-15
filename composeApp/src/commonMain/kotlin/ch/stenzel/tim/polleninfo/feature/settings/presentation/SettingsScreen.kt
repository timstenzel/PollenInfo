package ch.stenzel.tim.polleninfo.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.appinfo.AppVersion
import ch.stenzel.tim.polleninfo.core.language.AppLanguage
import ch.stenzel.tim.polleninfo.core.ui.error.ErrorContext
import ch.stenzel.tim.polleninfo.core.ui.error.message
import ch.stenzel.tim.polleninfo.core.ui.password.PasswordField
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.account_hide_password
import ch.stenzel.tim.polleninfo.resources.account_password_label
import ch.stenzel.tim.polleninfo.resources.account_show_password
import ch.stenzel.tim.polleninfo.resources.common_cancel
import ch.stenzel.tim.polleninfo.resources.settings_account_delete
import ch.stenzel.tim.polleninfo.resources.settings_account_delete_action
import ch.stenzel.tim.polleninfo.resources.settings_account_delete_text
import ch.stenzel.tim.polleninfo.resources.settings_account_delete_title
import ch.stenzel.tim.polleninfo.resources.settings_account_deletion_note
import ch.stenzel.tim.polleninfo.resources.settings_account_log_in
import ch.stenzel.tim.polleninfo.resources.settings_account_log_in_action
import ch.stenzel.tim.polleninfo.resources.settings_account_log_out
import ch.stenzel.tim.polleninfo.resources.settings_account_log_out_action
import ch.stenzel.tim.polleninfo.resources.settings_account_log_out_text
import ch.stenzel.tim.polleninfo.resources.settings_account_log_out_title
import ch.stenzel.tim.polleninfo.resources.settings_account_logged_in_as
import ch.stenzel.tim.polleninfo.resources.settings_account_not_logged_in
import ch.stenzel.tim.polleninfo.resources.settings_data_source_attribution
import ch.stenzel.tim.polleninfo.resources.settings_data_source_disclaimer
import ch.stenzel.tim.polleninfo.resources.settings_developed_by
import ch.stenzel.tim.polleninfo.resources.settings_email_action
import ch.stenzel.tim.polleninfo.resources.settings_language_action
import ch.stenzel.tim.polleninfo.resources.settings_language_restart_note
import ch.stenzel.tim.polleninfo.resources.settings_language_system
import ch.stenzel.tim.polleninfo.resources.settings_meteoswiss_link_action
import ch.stenzel.tim.polleninfo.resources.settings_meteoswiss_url
import ch.stenzel.tim.polleninfo.resources.settings_section_account
import ch.stenzel.tim.polleninfo.resources.settings_section_data_source
import ch.stenzel.tim.polleninfo.resources.settings_section_impressum
import ch.stenzel.tim.polleninfo.resources.settings_section_language
import ch.stenzel.tim.polleninfo.resources.settings_section_station
import ch.stenzel.tim.polleninfo.resources.settings_station_action
import ch.stenzel.tim.polleninfo.resources.settings_station_not_set
import ch.stenzel.tim.polleninfo.resources.settings_title
import ch.stenzel.tim.polleninfo.resources.settings_version
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Names, not wording: the same in every language. */
private const val DEVELOPER_NAME = "Tim Stenzel"
private const val DEVELOPER_EMAIL = "developer.mobile.t3s@gmail.com"
private const val CONTACT_URI = "mailto:$DEVELOPER_EMAIL?subject=PollenInfo"

/**
 * The Settings tab: one scrolling screen of sections — Default station, Account, Language,
 * Impressum, Data source — and the version line at the bottom. Tapping the default station calls
 * [onChangeStation], "Log in" (while nobody is logged in) [onLogIn].
 *
 * The scroll state is saveable, so it survives a tab switch through `navigateToTab`'s saved state.
 */
@Composable
fun SettingsScreen(
    onChangeStation: () -> Unit,
    onLogIn: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Back from Android's per-app language screen, among others: the language may have changed there.
    LaunchedEffect(lifecycle) {
        lifecycle.currentStateFlow
            .filter { it == Lifecycle.State.RESUMED }
            .collect { viewModel.onResume() }
    }

    SettingsContent(
        uiState = uiState,
        onChangeStation = onChangeStation,
        onLogIn = onLogIn,
        onLogOut = viewModel::onLogoutClicked,
        onDeleteAccount = viewModel::onDeleteAccountClicked,
        onLanguageRowClicked = viewModel::onLanguageRowClicked,
    )

    if (uiState.showLogoutDialog) {
        LogoutDialog(onConfirm = viewModel::onLogoutConfirmed, onDismiss = viewModel::onLogoutDismissed)
    }

    if (uiState.showDeleteDialog) {
        DeleteAccountDialog(
            uiState = uiState,
            onPasswordChange = viewModel::onDeletePasswordChanged,
            onToggleVisible = viewModel::onDeletePasswordVisibilityToggled,
            onConfirm = viewModel::onDeleteConfirmed,
            onDismiss = viewModel::onDeleteDismissed,
        )
    }

    if (uiState.showLanguageDialog) {
        LanguageDialog(
            current = uiState.language,
            onSelected = viewModel::onLanguageSelected,
            onDismiss = viewModel::onLanguageDialogDismissed,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onChangeStation: () -> Unit,
    onLogIn: () -> Unit,
    onLogOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onLanguageRowClicked: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(Res.string.settings_title)) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            StationSection(uiState.stationName, onChangeStation)
            HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            AccountSection(uiState.accountChecked, uiState.accountEmail, onLogIn, onLogOut, onDeleteAccount)
            HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            LanguageSection(uiState.language, uiState.languageAppliesOnRestart, onLanguageRowClicked)
            HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            ImpressumSection()
            HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            DataSourceSection()
            uiState.version?.let { VersionLine(it) }
        }
    }
}

/** The default station, as one row that opens the change-station screen. */
@Composable
private fun StationSection(stationName: String?, onChangeStation: () -> Unit) {
    SectionTitle(stringResource(Res.string.settings_section_station))
    ValueRow(
        // Station names are shown as published, in every language.
        text = stationName ?: stringResource(Res.string.settings_station_not_set),
        onClickLabel = stringResource(Res.string.settings_station_action),
        onClick = onChangeStation,
    )
}

/**
 * Which account this phone is logged in to, with "Log out" and "Delete account" — or "Not logged in"
 * with "Log in". The rows wait for the stored login to be read, so the wrong one never flashes up.
 */
@Composable
private fun AccountSection(
    checked: Boolean,
    email: String?,
    onLogIn: () -> Unit,
    onLogOut: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    SectionTitle(stringResource(Res.string.settings_section_account))
    if (!checked) return
    if (email != null) {
        BodyText(stringResource(Res.string.settings_account_logged_in_as, email))
        ActionRow(
            text = stringResource(Res.string.settings_account_log_out),
            icon = Icons.AutoMirrored.Filled.Logout,
            onClickLabel = stringResource(Res.string.settings_account_log_out_action),
            onClick = onLogOut,
        )
        ActionRow(
            text = stringResource(Res.string.settings_account_delete),
            icon = Icons.Default.DeleteForever,
            onClickLabel = stringResource(Res.string.settings_account_delete_action),
            onClick = onDeleteAccount,
        )
    } else {
        BodyText(stringResource(Res.string.settings_account_not_logged_in))
        ActionRow(
            text = stringResource(Res.string.settings_account_log_in),
            icon = Icons.AutoMirrored.Filled.Login,
            onClickLabel = stringResource(Res.string.settings_account_log_in_action),
            onClick = onLogIn,
        )
    }
}

/** Asks before logging out, and says the diary stays in the account. */
@Composable
private fun LogoutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_account_log_out_title)) },
        text = { Text(stringResource(Res.string.settings_account_log_out_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(Res.string.settings_account_log_out)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) }
        },
    )
}

/**
 * Deleting the account: the warning that the account and every answer go for good, the password
 * asked again, and the confirm button with a progress indicator while it runs. A wrong password —
 * or any other failure — stays in the dialog under the field. Dismissing is ignored while deleting.
 */
@Composable
private fun DeleteAccountDialog(
    uiState: SettingsUiState,
    onPasswordChange: (String) -> Unit,
    onToggleVisible: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_account_delete_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(Res.string.settings_account_delete_text))
                PasswordField(
                    value = uiState.deletePassword,
                    onValueChange = onPasswordChange,
                    label = Res.string.account_password_label,
                    visible = uiState.deletePasswordVisible,
                    onToggleVisible = onToggleVisible,
                    showLabel = Res.string.account_show_password,
                    hideLabel = Res.string.account_hide_password,
                    enabled = !uiState.isDeleting,
                    isError = uiState.deleteError != null,
                )
                uiState.deleteError?.let { error ->
                    Text(
                        text = error.message(ErrorContext.DELETE_ACCOUNT),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = uiState.canConfirmDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                if (uiState.isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(Res.string.settings_account_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uiState.isDeleting) {
                Text(stringResource(Res.string.common_cancel))
            }
        },
    )
}

/**
 * The app language, as one row that opens the choice dialog. Where a change only applies at the next
 * launch (iOS), a note under it says so once the language has been changed.
 */
@Composable
private fun LanguageSection(language: AppLanguage, appliesOnRestart: Boolean, onClick: () -> Unit) {
    SectionTitle(stringResource(Res.string.settings_section_language))
    ValueRow(
        text = language.label(),
        onClickLabel = stringResource(Res.string.settings_language_action),
        onClick = onClick,
    )
    if (appliesOnRestart) {
        Text(
            text = stringResource(Res.string.settings_language_restart_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
    }
}

/** Every language, [current] marked. Choosing one applies it and closes the dialog. */
@Composable
private fun LanguageDialog(current: AppLanguage, onSelected: (AppLanguage) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_section_language)) },
        text = {
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = language == current,
                                role = Role.RadioButton,
                                onClick = { onSelected(language) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = language == current, onClick = null)
                        Text(language.label(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) }
        },
    )
}

/**
 * Each language in its own name, so it can be found from any language the app is in; only "System
 * default" is translated.
 */
@Composable
private fun AppLanguage.label(): String = when (this) {
    AppLanguage.SYSTEM -> stringResource(Res.string.settings_language_system)
    AppLanguage.EN -> "English"
    AppLanguage.DE -> "Deutsch"
    AppLanguage.FR -> "Français"
    AppLanguage.IT -> "Italiano"
}

/** A full-width row ≥ 48 dp showing a current value, which opens the screen or dialog that changes it. */
@Composable
private fun ValueRow(text: String, onClickLabel: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A full-width row ≥ 48 dp that does something, in the primary colour, with its icon in front. The
 * icon is decorative; [onClickLabel] tells a screen reader what a double tap does.
 */
@Composable
private fun ActionRow(text: String, icon: ImageVector, onClickLabel: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ImpressumSection() {
    SectionTitle(stringResource(Res.string.settings_section_impressum))
    BodyText(stringResource(Res.string.settings_developed_by, DEVELOPER_NAME))
    LinkRow(
        text = DEVELOPER_EMAIL,
        icon = Icons.Default.Email,
        onClickLabel = stringResource(Res.string.settings_email_action),
        uri = CONTACT_URI,
    )
    // For a user who cannot open the app any more: deletion by email request.
    Text(
        text = stringResource(Res.string.settings_account_deletion_note, DEVELOPER_EMAIL),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
    )
}

/**
 * The attribution is the exact form MeteoSwiss's terms of use prescribe for the language, and the
 * statement below it is what keeps the app from looking endorsed by them; both are required by those
 * terms. The link is a courtesy, to their site in the same language.
 */
@Composable
private fun DataSourceSection() {
    val url = stringResource(Res.string.settings_meteoswiss_url)
    SectionTitle(stringResource(Res.string.settings_section_data_source))
    BodyText(stringResource(Res.string.settings_data_source_attribution))
    LinkRow(
        text = url.removePrefix("https://"),
        icon = Icons.AutoMirrored.Filled.OpenInNew,
        onClickLabel = stringResource(Res.string.settings_meteoswiss_link_action),
        uri = url,
    )
    Text(
        text = stringResource(Res.string.settings_data_source_disclaimer),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
    )
}

@Composable
private fun VersionLine(version: AppVersion) {
    Spacer(Modifier.height(24.dp))
    Text(
        text = stringResource(Res.string.settings_version, version.name, version.code),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
    )
}

/**
 * A full-width row, at least 48 dp tall so it is an adequate touch target, that opens [uri]. The
 * icon is decorative; [onClickLabel] tells a screen reader what a double tap does.
 */
@Composable
private fun LinkRow(text: String, icon: ImageVector, onClickLabel: String, uri: String) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = onClickLabel, role = Role.Button) {
                // A device with no mail app or browser has nothing to open the link with; the tap
                // then does nothing rather than crash the screen.
                try {
                    uriHandler.openUri(uri)
                } catch (_: Exception) {
                }
            }
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}
