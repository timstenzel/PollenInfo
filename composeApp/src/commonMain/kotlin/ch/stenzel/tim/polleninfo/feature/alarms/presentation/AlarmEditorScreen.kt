package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.ui.button.PillButton
import ch.stenzel.tim.polleninfo.core.ui.button.PillButtonStyle
import ch.stenzel.tim.polleninfo.core.ui.dialog.ConfirmDialog
import ch.stenzel.tim.polleninfo.core.ui.dialog.DialogTone
import ch.stenzel.tim.polleninfo.core.ui.error.ErrorContext
import ch.stenzel.tim.polleninfo.core.ui.error.message
import ch.stenzel.tim.polleninfo.core.ui.field.SelectField
import ch.stenzel.tim.polleninfo.core.ui.format.rememberDateWording
import ch.stenzel.tim.polleninfo.core.ui.layout.ScreenFrame
import ch.stenzel.tim.polleninfo.core.ui.layout.ScreenHeader
import ch.stenzel.tim.polleninfo.core.ui.selection.ChipLabel
import ch.stenzel.tim.polleninfo.core.ui.selection.MeadowFilterChip
import ch.stenzel.tim.polleninfo.core.ui.species.speciesName
import ch.stenzel.tim.polleninfo.core.ui.status.StatusCard
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmSchedule
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmType
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.alarm_delete
import ch.stenzel.tim.polleninfo.resources.alarm_delete_confirm
import ch.stenzel.tim.polleninfo.resources.alarm_delete_text
import ch.stenzel.tim.polleninfo.resources.alarm_delete_title
import ch.stenzel.tim.polleninfo.resources.alarm_deleting
import ch.stenzel.tim.polleninfo.resources.alarm_discard_confirm
import ch.stenzel.tim.polleninfo.resources.alarm_discard_keep
import ch.stenzel.tim.polleninfo.resources.alarm_discard_text
import ch.stenzel.tim.polleninfo.resources.alarm_discard_title
import ch.stenzel.tim.polleninfo.resources.alarm_editor_load_failed
import ch.stenzel.tim.polleninfo.resources.alarm_editor_title_edit
import ch.stenzel.tim.polleninfo.resources.alarm_editor_title_new
import ch.stenzel.tim.polleninfo.resources.alarm_hint_no_days
import ch.stenzel.tim.polleninfo.resources.alarm_hint_no_species
import ch.stenzel.tim.polleninfo.resources.alarm_hint_window
import ch.stenzel.tim.polleninfo.resources.alarm_minimum_note_any
import ch.stenzel.tim.polleninfo.resources.alarm_minimum_note_daily
import ch.stenzel.tim.polleninfo.resources.alarm_minimum_note_threshold
import ch.stenzel.tim.polleninfo.resources.alarm_report_time
import ch.stenzel.tim.polleninfo.resources.alarm_save
import ch.stenzel.tim.polleninfo.resources.alarm_saving
import ch.stenzel.tim.polleninfo.resources.alarm_section_days
import ch.stenzel.tim.polleninfo.resources.alarm_section_minimum_daily
import ch.stenzel.tim.polleninfo.resources.alarm_section_minimum_threshold
import ch.stenzel.tim.polleninfo.resources.alarm_section_species
import ch.stenzel.tim.polleninfo.resources.alarm_section_station
import ch.stenzel.tim.polleninfo.resources.alarm_section_time
import ch.stenzel.tim.polleninfo.resources.alarm_section_window
import ch.stenzel.tim.polleninfo.resources.alarm_swiss_time


import ch.stenzel.tim.polleninfo.resources.alarm_type_daily_description
import ch.stenzel.tim.polleninfo.resources.alarm_type_threshold_description
import ch.stenzel.tim.polleninfo.resources.alarm_window_end
import ch.stenzel.tim.polleninfo.resources.alarm_window_start
import ch.stenzel.tim.polleninfo.resources.common_cancel
import ch.stenzel.tim.polleninfo.resources.common_retry
import ch.stenzel.tim.polleninfo.resources.nav_alarms
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Creates a daily report or a threshold alert, or edits alarm [alarmId]. Shown without the bottom
 * bar — it is a task with a clear way back, not a tab. [onDone] leaves the editor, after a save or a
 * delete, or when the user goes back without unsaved changes or chooses to discard them.
 */
@Composable
fun AlarmEditorScreen(
    alarmId: String?,
    onDone: () -> Unit,
    viewModel: AlarmEditorViewModel = koinViewModel(parameters = { parametersOf(alarmId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // The system back asks about unsaved changes just as the back button does.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        onBackCompleted = viewModel::onBack,
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AlarmEditorEvent.Done -> onDone()
            }
        }
    }

    AlarmEditorContent(
        uiState = uiState,
        title = stringResource(if (alarmId == null) Res.string.alarm_editor_title_new else Res.string.alarm_editor_title_edit),
        onBack = viewModel::onBack,
        onRetry = viewModel::retry,
        onStationSelected = viewModel::onStationSelected,
        onSpeciesToggled = viewModel::onSpeciesToggled,
        onMinSeveritySelected = viewModel::onMinSeveritySelected,
        onDayToggled = viewModel::onDayToggled,
        onTypeSelected = viewModel::onTypeSelected,
        onTimeSelected = viewModel::onTimeSelected,
        onWindowStartSelected = viewModel::onWindowStartSelected,
        onWindowEndSelected = viewModel::onWindowEndSelected,
        onSave = viewModel::save,
        onDiscardConfirmed = viewModel::onDiscardConfirmed,
        onDiscardDismissed = viewModel::onDiscardDismissed,
        onDeleteRequested = viewModel::onDeleteRequested,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onDeleteDismissed = viewModel::onDeleteDismissed,
    )
}

@Composable
private fun AlarmEditorContent(
    uiState: AlarmEditorUiState,
    title: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onStationSelected: (String) -> Unit,
    onSpeciesToggled: (String) -> Unit,
    onMinSeveritySelected: (PollenSeverity) -> Unit,
    onDayToggled: (DayOfWeek) -> Unit,
    onTypeSelected: (AlarmType) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onWindowStartSelected: (LocalTime) -> Unit,
    onWindowEndSelected: (LocalTime) -> Unit,
    onSave: () -> Unit,
    onDiscardConfirmed: () -> Unit,
    onDiscardDismissed: () -> Unit,
    onDeleteRequested: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteDismissed: () -> Unit,
) {
    // The overline is the place the editor belongs to: it is opened from the Alarms tab.
    val header = @Composable { ScreenHeader(overline = stringResource(Res.string.nav_alarms), title = title) }
    ScreenFrame(
        onBack = onBack,
        actionBar = (uiState as? AlarmEditorUiState.Editing)?.let { state -> { SaveBar(state, onSave) } },
    ) { contentPadding ->
        when (uiState) {
            AlarmEditorUiState.Loading -> EditorColumn(contentPadding, header, Modifier.fillMaxSize()) {
                AlarmEditorSkeleton(Modifier.fillMaxWidth())
            }

            is AlarmEditorUiState.Error -> EditorColumn(contentPadding, header, Modifier.fillMaxSize()) {
                StatusCard(
                    title = stringResource(Res.string.alarm_editor_load_failed),
                    body = uiState.error.message(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PillButton(onClick = onRetry, style = PillButtonStyle.Primary, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.common_retry))
                    }
                }
            }

            is AlarmEditorUiState.Editing -> EditingForm(
                state = uiState,
                header = header,
                onStationSelected = onStationSelected,
                onSpeciesToggled = onSpeciesToggled,
                onMinSeveritySelected = onMinSeveritySelected,
                onDayToggled = onDayToggled,
                onTypeSelected = onTypeSelected,
                onTimeSelected = onTimeSelected,
                onWindowStartSelected = onWindowStartSelected,
                onWindowEndSelected = onWindowEndSelected,
                onDeleteRequested = onDeleteRequested,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            )
        }
    }

    if (uiState is AlarmEditorUiState.Editing) {
        if (uiState.showDiscardDialog) {
            ConfirmDialog(
                icon = Icons.Default.Edit,
                tone = DialogTone.Neutral,
                title = stringResource(Res.string.alarm_discard_title),
                text = stringResource(Res.string.alarm_discard_text),
                confirmLabel = stringResource(Res.string.alarm_discard_confirm),
                confirmStyle = PillButtonStyle.Danger,
                onConfirm = onDiscardConfirmed,
                dismissLabel = stringResource(Res.string.alarm_discard_keep),
                onDismiss = onDiscardDismissed,
            )
        }
        if (uiState.showDeleteDialog) {
            ConfirmDialog(
                icon = Icons.Default.Delete,
                tone = DialogTone.Destructive,
                title = stringResource(Res.string.alarm_delete_title),
                text = stringResource(Res.string.alarm_delete_text),
                confirmLabel = stringResource(Res.string.alarm_delete_confirm),
                confirmStyle = PillButtonStyle.Danger,
                onConfirm = onDeleteConfirmed,
                dismissLabel = stringResource(Res.string.common_cancel),
                onDismiss = onDeleteDismissed,
            )
        }
    }
}

/**
 * The action bar: why the last save or delete failed, directly above Save, and Save itself with its
 * progress while it runs.
 */
@Composable
private fun SaveBar(state: AlarmEditorUiState.Editing, onSave: () -> Unit) {
    val failure = state.saveError?.message(ErrorContext.SAVE_ALARM)
        ?: state.deleteError?.message(ErrorContext.DELETE_ALARM)
    failure?.let { text ->
        Row(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 14.dp)) {
            // Decorative: the red sentence says what went wrong.
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 1.dp, end = 10.dp).size(18.dp),
            )
            Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
    PillButton(
        onClick = onSave,
        style = PillButtonStyle.Primary,
        enabled = state.canSave,
        busy = state.isSaving,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(if (state.isSaving) Res.string.alarm_saving else Res.string.alarm_save))
    }
}

@Composable
private fun EditingForm(
    state: AlarmEditorUiState.Editing,
    onStationSelected: (String) -> Unit,
    onSpeciesToggled: (String) -> Unit,
    onMinSeveritySelected: (PollenSeverity) -> Unit,
    onDayToggled: (DayOfWeek) -> Unit,
    onTypeSelected: (AlarmType) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onWindowStartSelected: (LocalTime) -> Unit,
    onWindowEndSelected: (LocalTime) -> Unit,
    onDeleteRequested: () -> Unit,
    header: @Composable () -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    val form = state.form
    val enabled = !state.isBusy
    EditorColumn(contentPadding, header, modifier) {
        // Locked when editing: still shown, so the alarm's type is visible, but not changeable.
        TypeCards(selected = form.type, locked = form.typeLocked, enabled = enabled, onTypeSelected = onTypeSelected)
        Text(
            stringResource(
                when (form.type) {
                    AlarmType.DAILY -> Res.string.alarm_type_daily_description
                    AlarmType.THRESHOLD -> Res.string.alarm_type_threshold_description
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Section(stringResource(Res.string.alarm_section_station)) {
            StationDropdown(
                stations = state.stations,
                selectedAbbr = form.stationAbbr,
                enabled = enabled,
                onStationSelected = onStationSelected,
            )
        }

        Section(stringResource(Res.string.alarm_section_species)) {
            ChipRow {
                state.species.forEach { species ->
                    MeadowFilterChip(
                        selected = species.id in form.species,
                        onClick = { onSpeciesToggled(species.id) },
                        enabled = enabled,
                    ) { ChipLabel(speciesName(species.id, species.name)) }
                }
            }
            if (form.species.isEmpty()) ValidationHint(stringResource(Res.string.alarm_hint_no_species))
        }

        Section(
            stringResource(
                when (form.type) {
                    AlarmType.DAILY -> Res.string.alarm_section_minimum_daily
                    AlarmType.THRESHOLD -> Res.string.alarm_section_minimum_threshold
                },
            ),
        ) {
            ChipRow {
                form.severityOptions.forEach { severity ->
                    MeadowFilterChip(
                        selected = severity == form.minSeverity,
                        onClick = { onMinSeveritySelected(severity) },
                        enabled = enabled,
                    ) { ChipLabel(severity.minimumLabel()) }
                }
            }
            Note(
                when {
                    form.type == AlarmType.THRESHOLD ->
                        stringResource(Res.string.alarm_minimum_note_threshold, form.minSeverity.minimumLabel())
                    form.minSeverity == PollenSeverity.NONE -> stringResource(Res.string.alarm_minimum_note_any)
                    else -> stringResource(Res.string.alarm_minimum_note_daily, form.minSeverity.minimumLabel())
                },
            )
        }

        Section(stringResource(Res.string.alarm_section_days)) {
            DayToggles(days = form.days, enabled = enabled, dates = rememberDateWording(), onDayToggled = onDayToggled)
            if (form.days.isEmpty()) ValidationHint(stringResource(Res.string.alarm_hint_no_days))
        }

        when (val schedule = form.schedule) {
            is AlarmSchedule.Daily -> Section(stringResource(Res.string.alarm_section_time)) {
                TimeField(
                    label = stringResource(Res.string.alarm_report_time),
                    time = schedule.at,
                    enabled = enabled,
                    onTimeSelected = onTimeSelected,
                    modifier = Modifier.fillMaxWidth(),
                )
                Note(stringResource(Res.string.alarm_swiss_time))
            }
            is AlarmSchedule.Threshold -> Section(stringResource(Res.string.alarm_section_window)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeField(
                        label = stringResource(Res.string.alarm_window_start),
                        time = schedule.from,
                        enabled = enabled,
                        onTimeSelected = onWindowStartSelected,
                        modifier = Modifier.weight(1f),
                    )
                    // The end is the field that is wrong when the window is not valid.
                    TimeField(
                        label = stringResource(Res.string.alarm_window_end),
                        time = schedule.until,
                        enabled = enabled,
                        onTimeSelected = onWindowEndSelected,
                        modifier = Modifier.weight(1f),
                        isError = !form.isWindowValid,
                    )
                }
                if (!form.isWindowValid) ValidationHint(stringResource(Res.string.alarm_hint_window))
                Note(stringResource(Res.string.alarm_swiss_time))
            }
        }

        if (state.canDelete) {
            PillButton(
                onClick = onDeleteRequested,
                style = PillButtonStyle.Danger,
                enabled = !state.isBusy,
                busy = state.isDeleting,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text(stringResource(if (state.isDeleting) Res.string.alarm_deleting else Res.string.alarm_delete))
            }
        }
    }
}

/** The editor's scrolling column, clear of the back button and the action bar, with its margins. */
@Composable
private fun EditorColumn(
    contentPadding: PaddingValues,
    header: @Composable () -> Unit,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        header()
        content()
    }
}

/** A titled part of the form: its heading, then its controls and what is said about them. */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ValidationHint(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationDropdown(
    stations: List<Station>,
    selectedAbbr: String,
    enabled: Boolean,
    onStationSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
    ) {
        SelectField(
            label = stringResource(Res.string.alarm_section_station),
            value = stations.firstOrNull { it.abbr == selectedAbbr }?.name ?: selectedAbbr,
            trailingIcon = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            enabled = enabled,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            stations.forEach { station ->
                DropdownMenuItem(
                    text = { Text(station.name) },
                    onClick = {
                        onStationSelected(station.abbr)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
