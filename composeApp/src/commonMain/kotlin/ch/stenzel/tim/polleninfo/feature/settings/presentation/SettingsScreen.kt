package ch.stenzel.tim.polleninfo.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
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
import ch.stenzel.tim.polleninfo.core.ui.button.PillButtonStyle
import ch.stenzel.tim.polleninfo.core.ui.dialog.ConfirmDialog
import ch.stenzel.tim.polleninfo.core.ui.dialog.DialogTone
import ch.stenzel.tim.polleninfo.core.ui.error.ErrorContext
import ch.stenzel.tim.polleninfo.core.ui.error.message
import ch.stenzel.tim.polleninfo.core.ui.layout.LocalBottomBarPadding
import ch.stenzel.tim.polleninfo.core.ui.layout.MeadowCard
import ch.stenzel.tim.polleninfo.core.ui.layout.ScreenHeader
import ch.stenzel.tim.polleninfo.core.ui.layout.TabScreenFrame
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
import ch.stenzel.tim.polleninfo.resources.settings_crash_reports_description
import ch.stenzel.tim.polleninfo.resources.settings_crash_reports_label
import ch.stenzel.tim.polleninfo.resources.settings_crash_reports_save_failed
import ch.stenzel.tim.polleninfo.resources.settings_data_source_attribution
import ch.stenzel.tim.polleninfo.resources.settings_data_source_disclaimer
import ch.stenzel.tim.polleninfo.resources.settings_developed_by
import ch.stenzel.tim.polleninfo.resources.settings_email_action
import ch.stenzel.tim.polleninfo.resources.settings_language_action
import ch.stenzel.tim.polleninfo.resources.settings_language_restart_note
import ch.stenzel.tim.polleninfo.resources.settings_language_system
import ch.stenzel.tim.polleninfo.resources.settings_meteoswiss_link_action
import ch.stenzel.tim.polleninfo.resources.settings_meteoswiss_url
import ch.stenzel.tim.polleninfo.resources.settings_overline
import ch.stenzel.tim.polleninfo.resources.settings_privacy_title
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
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Names, not wording: the same in every language. */
private const val DEVELOPER_NAME = "Tim Stenzel"
private const val DEVELOPER_EMAIL = "developer.mobile.t3s@gmail.com"
private const val CONTACT_URI = "mailto:$DEVELOPER_EMAIL?subject=PollenInfo"

/**
 * The Settings tab: one scrolling screen of sections — Default station, Account, Language, Privacy
 * (only where the platform reports crashes), Impressum, Data source — and the version line at the
 * bottom. Tapping the default station calls
 * [onChangeStation], "Log in" (while nobody is logged in) [onLogIn].
 *
 * The scroll state is saveable, so it survives a tab switch through `navigateToTab`'s saved state.
 * Its events concern only this screen, so it shows them in its own snackbar, with no lambda from
 * `AppNavigation`.
 */
@Composable
fun SettingsScreen(
    onChangeStation: () -> Unit,
    onLogIn: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.CrashReportsSaveFailed ->
                    snackbarHostState.showSnackbar(getString(Res.string.settings_crash_reports_save_failed))
            }
        }
    }

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
        onCrashReportsToggled = viewModel::onCrashReportsToggled,
        snackbarHostState = snackbarHostState,
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

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onChangeStation: () -> Unit,
    onLogIn: () -> Unit,
    onLogOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onLanguageRowClicked: () -> Unit,
    onCrashReportsToggled: (Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    TabScreenFrame { contentPadding ->
        // The screen runs on behind the glass tab bar; the snackbar is lifted above it.
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            snackbarHost = {
                SnackbarHost(
                    snackbarHostState,
                    // Material's snackbar keeps a margin of its own around it.
                    Modifier.padding(start = 8.dp, end = 8.dp, bottom = LocalBottomBarPadding.current + 4.dp),
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }
            },
        ) { scaffoldPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding)
                    .padding(start = SIDE_MARGIN, end = SIDE_MARGIN, bottom = 8.dp),
            ) {
                ScreenHeader(
                    overline = stringResource(Res.string.settings_overline),
                    title = stringResource(Res.string.settings_title),
                )
                // Waits for the stored login to be read, so the wrong card never flashes up.
                if (uiState.accountChecked) AccountCard(uiState.accountEmail, onLogIn, onLogOut)
                StationSection(uiState.stationName, onChangeStation)
                LanguageSection(uiState.language, uiState.languageAppliesOnRestart, onLanguageRowClicked)
                if (uiState.crashReportsAvailable) {
                    PrivacySection(uiState.crashReportsEnabled, onCrashReportsToggled)
                }
                DataSourceSection()
                ImpressumSection()
                if (uiState.accountChecked && uiState.accountEmail != null) DeleteAccountSection(onDeleteAccount)
                uiState.version?.let { VersionLine(it) }
            }
        }
    }
}

/**
 * The green account card at the top: which account this phone is logged in to with "Log out", or
 * "Not logged in" with "Log in". The words are one focus stop, the button another.
 */
@Composable
private fun AccountCard(email: String?, onLogIn: () -> Unit, onLogOut: () -> Unit) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large)
            .padding(start = 18.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(onPrimary.copy(alpha = TINT_ALPHA), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Decorative: the words beside it say whose account this is.
            Icon(Icons.Default.Person, contentDescription = null, tint = onPrimary, modifier = Modifier.size(26.dp))
        }
        Column(
            modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (email != null) {
                // "Logged in as" small above the address, as in Figma, from the one sentence the
                // translations have; where a language put the address elsewhere, the sentence stays whole.
                val sentence = stringResource(Res.string.settings_account_logged_in_as, email)
                val lead = sentence.substringBeforeLast(email, missingDelimiterValue = "")
                if (lead.isNotBlank() && sentence.endsWith(email)) {
                    Text(lead.trimEnd(), style = MaterialTheme.typography.labelMedium, color = onPrimary.copy(alpha = LEAD_ALPHA))
                    Text(email, style = MaterialTheme.typography.labelLarge, color = onPrimary)
                } else {
                    Text(sentence, style = MaterialTheme.typography.labelLarge, color = onPrimary)
                }
            } else {
                Text(
                    stringResource(Res.string.settings_account_not_logged_in),
                    style = MaterialTheme.typography.labelLarge,
                    color = onPrimary,
                )
            }
        }
        if (email != null) {
            AccountCardButton(
                text = stringResource(Res.string.settings_account_log_out),
                icon = Icons.AutoMirrored.Filled.Logout,
                onClickLabel = stringResource(Res.string.settings_account_log_out_action),
                onClick = onLogOut,
            )
        } else {
            AccountCardButton(
                text = stringResource(Res.string.settings_account_log_in),
                icon = Icons.AutoMirrored.Filled.Login,
                onClickLabel = stringResource(Res.string.settings_account_log_in_action),
                onClick = onLogIn,
            )
        }
    }
}

/** The small tinted pill on the account card; its touch target is the full 48 dp. */
@Composable
private fun AccountCardButton(text: String, icon: ImageVector, onClickLabel: String, onClick: () -> Unit) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .background(onPrimary.copy(alpha = TINT_ALPHA), CircleShape)
                .padding(start = 12.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = onPrimary, modifier = Modifier.size(16.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = onPrimary)
        }
    }
}

/** The default station, as one row that opens the change-station screen. */
@Composable
private fun StationSection(stationName: String?, onChangeStation: () -> Unit) {
    SettingsGroup(stringResource(Res.string.settings_section_station)) {
        val action = stringResource(Res.string.settings_station_action)
        SettingsRow(
            icon = Icons.Default.Place,
            // Station names are shown as published, in every language.
            title = stationName ?: stringResource(Res.string.settings_station_not_set),
            subtitle = action,
            trailing = RowTrailing.Chevron,
            onClickLabel = action,
            onClick = onChangeStation,
        )
    }
}

/** Asks before logging out, and says the diary stays in the account. */
@Composable
private fun LogoutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        icon = Icons.AutoMirrored.Filled.Logout,
        tone = DialogTone.Neutral,
        title = stringResource(Res.string.settings_account_log_out_title),
        text = stringResource(Res.string.settings_account_log_out_text),
        confirmLabel = stringResource(Res.string.settings_account_log_out),
        confirmStyle = PillButtonStyle.Primary,
        onConfirm = onConfirm,
        dismissLabel = stringResource(Res.string.common_cancel),
        onDismiss = onDismiss,
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
    ConfirmDialog(
        icon = Icons.Default.Person,
        tone = DialogTone.Destructive,
        title = stringResource(Res.string.settings_account_delete_title),
        text = stringResource(Res.string.settings_account_delete_text),
        confirmLabel = stringResource(Res.string.settings_account_delete),
        confirmStyle = PillButtonStyle.Danger,
        onConfirm = onConfirm,
        dismissLabel = stringResource(Res.string.common_cancel),
        onDismiss = onDismiss,
        confirmEnabled = uiState.canConfirmDelete,
        confirmBusy = uiState.isDeleting,
        dismissEnabled = !uiState.isDeleting,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    }
}

/**
 * The app language, as one row that opens the choice dialog. Where a change only applies at the next
 * launch (iOS), a note under it says so once the language has been changed.
 */
@Composable
private fun LanguageSection(language: AppLanguage, appliesOnRestart: Boolean, onClick: () -> Unit) {
    SettingsGroup(
        title = stringResource(Res.string.settings_section_language),
        note = if (appliesOnRestart) stringResource(Res.string.settings_language_restart_note) else null,
    ) {
        val action = stringResource(Res.string.settings_language_action)
        SettingsRow(
            icon = Icons.Default.Language,
            title = language.label(),
            subtitle = action,
            trailing = RowTrailing.Chevron,
            onClickLabel = action,
            onClick = onClick,
        )
    }
}

/**
 * The crash-report switch with one sentence on what is sent and to whom. The whole row is the
 * toggle, so a screen reader announces label, explanation and state as one switch; the [Switch]
 * itself takes no clicks of its own.
 */
@Composable
private fun PrivacySection(enabled: Boolean, onToggled: (Boolean) -> Unit) {
    SettingsGroup(stringResource(Res.string.settings_privacy_title)) {
        RowLayout(
            icon = Icons.Default.Shield,
            title = stringResource(Res.string.settings_crash_reports_label),
            subtitle = stringResource(Res.string.settings_crash_reports_description),
            modifier = Modifier.toggleable(value = enabled, role = Role.Switch, onValueChange = onToggled),
        ) {
            Switch(checked = enabled, onCheckedChange = null)
        }
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

/**
 * The attribution is the exact form MeteoSwiss's terms of use prescribe for the language, and the
 * statement below it is what keeps the app from looking endorsed by them; both are required by those
 * terms. The row opens their site in the same language, a courtesy.
 */
@Composable
private fun DataSourceSection() {
    val uriHandler = LocalUriHandler.current
    val url = stringResource(Res.string.settings_meteoswiss_url)
    SettingsGroup(stringResource(Res.string.settings_section_data_source)) {
        SettingsRow(
            icon = Icons.Default.Storage,
            title = stringResource(Res.string.settings_data_source_attribution),
            subtitle = stringResource(Res.string.settings_data_source_disclaimer),
            trailing = RowTrailing.OpenLink,
            onClickLabel = stringResource(Res.string.settings_meteoswiss_link_action),
            onClick = { uriHandler.openSafely(url) },
        )
    }
}

/**
 * Who made the app, as a row that writes them an email, and — for a user who cannot open the app any
 * more — that the account can also be deleted by email request, with the address spelled out.
 */
@Composable
private fun ImpressumSection() {
    val uriHandler = LocalUriHandler.current
    SettingsGroup(
        title = stringResource(Res.string.settings_section_impressum),
        note = stringResource(Res.string.settings_account_deletion_note, DEVELOPER_EMAIL),
    ) {
        val action = stringResource(Res.string.settings_email_action)
        SettingsRow(
            icon = Icons.Default.Email,
            title = stringResource(Res.string.settings_developed_by, DEVELOPER_NAME),
            subtitle = action,
            trailing = RowTrailing.OpenLink,
            onClickLabel = action,
            onClick = { uriHandler.openSafely(CONTACT_URI) },
        )
    }
}

/** Deleting the account, in red, only while logged in. */
@Composable
private fun DeleteAccountSection(onDeleteAccount: () -> Unit) {
    SettingsGroup(stringResource(Res.string.settings_section_account)) {
        val action = stringResource(Res.string.settings_account_delete_action)
        SettingsRow(
            icon = Icons.Default.Person,
            title = stringResource(Res.string.settings_account_delete),
            subtitle = action,
            trailing = RowTrailing.Chevron,
            danger = true,
            onClickLabel = action,
            onClick = onDeleteAccount,
        )
    }
}

@Composable
private fun VersionLine(version: AppVersion) {
    Text(
        text = stringResource(Res.string.settings_version, version.name, version.code),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

/**
 * One section: its name as an overline (a heading for screen readers), then its rows on a raised
 * card, then an optional [note] in small text below the card.
 */
@Composable
private fun SettingsGroup(title: String, note: String? = null, rows: @Composable () -> Unit) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(top = GROUP_GAP, bottom = GROUP_GAP)
            .semantics { heading() },
    )
    MeadowCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column { rows() }
    }
    if (note != null) {
        Text(
            text = note,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
        )
    }
}

/** What sits at a [SettingsRow]'s end: where the row leads. Decorative — the click label says it. */
private enum class RowTrailing { Chevron, OpenLink }

/**
 * A row ≥ 48 dp that opens a screen, a dialog or a link: icon tile, [title] with its [subtitle], and
 * [trailing]. [danger] tints it red for a destructive step. The icons are decorative; [onClickLabel]
 * tells a screen reader what a double tap does.
 */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: RowTrailing,
    onClickLabel: String,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    RowLayout(
        icon = icon,
        title = title,
        subtitle = subtitle,
        danger = danger,
        modifier = Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick),
    ) {
        when (trailing) {
            RowTrailing.Chevron -> Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            RowTrailing.OpenLink -> Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** The look shared by every Settings row; [modifier] makes it clickable or toggleable. */
@Composable
private fun RowLayout(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier,
    danger: Boolean = false,
    trailing: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(if (danger) colors.surfaceContainerHighest else colors.primaryContainer, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (danger) colors.error else colors.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = if (danger) colors.error else colors.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        trailing()
    }
}

/**
 * A device with no mail app or browser has nothing to open the link with; the tap then does nothing
 * rather than crash the screen.
 */
private fun UriHandler.openSafely(uri: String) {
    try {
        openUri(uri)
    } catch (_: Exception) {
    }
}

private val SIDE_MARGIN = 20.dp

/** Between a group's overline and the cards around it, as in Figma. */
private val GROUP_GAP = 12.dp

/** The account card's avatar circle and button: white (dark: forest) at 16 % over the green. */
internal const val TINT_ALPHA = 0.16f

/** "Logged in as" on the account card, dimmed beside the address. */
internal const val LEAD_ALPHA = 0.75f
