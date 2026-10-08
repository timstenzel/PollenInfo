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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.appinfo.AppVersion
import ch.stenzel.tim.polleninfo.core.language.AppLanguage
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.common_cancel
import ch.stenzel.tim.polleninfo.resources.settings_data_source_attribution
import ch.stenzel.tim.polleninfo.resources.settings_data_source_disclaimer
import ch.stenzel.tim.polleninfo.resources.settings_developed_by
import ch.stenzel.tim.polleninfo.resources.settings_email_action
import ch.stenzel.tim.polleninfo.resources.settings_language_action
import ch.stenzel.tim.polleninfo.resources.settings_language_restart_note
import ch.stenzel.tim.polleninfo.resources.settings_language_system
import ch.stenzel.tim.polleninfo.resources.settings_meteoswiss_link_action
import ch.stenzel.tim.polleninfo.resources.settings_meteoswiss_url
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
 * The Settings tab: one scrolling screen of sections — Default station, Language, Impressum, Data
 * source — and the version line at the bottom. Tapping the default station calls [onChangeStation].
 *
 * The scroll state is saveable, so it survives a tab switch through `navigateToTab`'s saved state.
 */
@Composable
fun SettingsScreen(
    onChangeStation: () -> Unit,
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
        onLanguageRowClicked = viewModel::onLanguageRowClicked,
    )

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
