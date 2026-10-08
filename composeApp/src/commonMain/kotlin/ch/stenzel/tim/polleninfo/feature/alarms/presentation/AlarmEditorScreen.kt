package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.ui.error.ErrorContext
import ch.stenzel.tim.polleninfo.core.ui.error.message
import ch.stenzel.tim.polleninfo.core.ui.format.formatTime
import ch.stenzel.tim.polleninfo.core.ui.format.rememberDateWording
import ch.stenzel.tim.polleninfo.core.ui.species.speciesName
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
import ch.stenzel.tim.polleninfo.resources.alarm_time_description
import ch.stenzel.tim.polleninfo.resources.alarm_time_picker_confirm
import ch.stenzel.tim.polleninfo.resources.alarm_type_daily
import ch.stenzel.tim.polleninfo.resources.alarm_type_daily_description
import ch.stenzel.tim.polleninfo.resources.alarm_type_threshold
import ch.stenzel.tim.polleninfo.resources.alarm_type_threshold_description
import ch.stenzel.tim.polleninfo.resources.alarm_window_end
import ch.stenzel.tim.polleninfo.resources.alarm_window_start
import ch.stenzel.tim.polleninfo.resources.common_back
import ch.stenzel.tim.polleninfo.resources.common_cancel
import ch.stenzel.tim.polleninfo.resources.common_retry
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
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AlarmEditorScreen(
    alarmId: String?,
    onDone: () -> Unit,
    viewModel: AlarmEditorViewModel = koinViewModel(parameters = { parametersOf(alarmId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // The system back asks about unsaved changes just as the top bar's arrow does.
    BackHandler(onBack = viewModel::onBack)

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

@OptIn(ExperimentalMaterial3Api::class)
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (uiState) {
            AlarmEditorUiState.Loading -> Box(modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            is AlarmEditorUiState.Error -> Column(
                modifier = modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(Res.string.alarm_editor_load_failed),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    uiState.error.message(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onRetry) { Text(stringResource(Res.string.common_retry)) }
            }

            is AlarmEditorUiState.Editing -> EditingForm(
                state = uiState,
                onStationSelected = onStationSelected,
                onSpeciesToggled = onSpeciesToggled,
                onMinSeveritySelected = onMinSeveritySelected,
                onDayToggled = onDayToggled,
                onTypeSelected = onTypeSelected,
                onTimeSelected = onTimeSelected,
                onWindowStartSelected = onWindowStartSelected,
                onWindowEndSelected = onWindowEndSelected,
                onSave = onSave,
                onDeleteRequested = onDeleteRequested,
                modifier = modifier,
            )
        }
    }

    if (uiState is AlarmEditorUiState.Editing) {
        if (uiState.showDiscardDialog) {
            AlertDialog(
                onDismissRequest = onDiscardDismissed,
                title = { Text(stringResource(Res.string.alarm_discard_title)) },
                text = { Text(stringResource(Res.string.alarm_discard_text)) },
                confirmButton = { TextButton(onClick = onDiscardConfirmed) { Text(stringResource(Res.string.alarm_discard_confirm)) } },
                dismissButton = { TextButton(onClick = onDiscardDismissed) { Text(stringResource(Res.string.alarm_discard_keep)) } },
            )
        }
        if (uiState.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = onDeleteDismissed,
                title = { Text(stringResource(Res.string.alarm_delete_title)) },
                text = { Text(stringResource(Res.string.alarm_delete_text)) },
                confirmButton = {
                    TextButton(
                        onClick = onDeleteConfirmed,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(Res.string.alarm_delete_confirm)) }
                },
                dismissButton = { TextButton(onClick = onDeleteDismissed) { Text(stringResource(Res.string.common_cancel)) } },
            )
        }
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
    onSave: () -> Unit,
    onDeleteRequested: () -> Unit,
    modifier: Modifier,
) {
    val form = state.form
    val enabled = !state.isBusy
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Locked when editing: still shown, so the alarm's type is visible, but not changeable.
        TypeToggle(selected = form.type, enabled = enabled && !form.typeLocked, onTypeSelected = onTypeSelected)
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

        SectionHeading(stringResource(Res.string.alarm_section_station))
        StationDropdown(
            stations = state.stations,
            selectedAbbr = form.stationAbbr,
            enabled = enabled,
            onStationSelected = onStationSelected,
        )

        SectionHeading(stringResource(Res.string.alarm_section_species))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.species.forEach { species ->
                FilterChip(
                    selected = species.id in form.species,
                    onClick = { onSpeciesToggled(species.id) },
                    label = { Text(speciesName(species.id, species.name)) },
                    enabled = enabled,
                )
            }
        }
        if (form.species.isEmpty()) ValidationHint(stringResource(Res.string.alarm_hint_no_species))

        SectionHeading(
            stringResource(
                when (form.type) {
                    AlarmType.DAILY -> Res.string.alarm_section_minimum_daily
                    AlarmType.THRESHOLD -> Res.string.alarm_section_minimum_threshold
                },
            ),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            form.severityOptions.forEach { severity ->
                FilterChip(
                    selected = severity == form.minSeverity,
                    onClick = { onMinSeveritySelected(severity) },
                    label = { Text(severity.minimumLabel()) },
                    enabled = enabled,
                )
            }
        }
        Text(
            when {
                form.type == AlarmType.THRESHOLD ->
                    stringResource(Res.string.alarm_minimum_note_threshold, form.minSeverity.minimumLabel())
                form.minSeverity == PollenSeverity.NONE -> stringResource(Res.string.alarm_minimum_note_any)
                else -> stringResource(Res.string.alarm_minimum_note_daily, form.minSeverity.minimumLabel())
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionHeading(stringResource(Res.string.alarm_section_days))
        val dates = rememberDateWording()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DayOfWeek.entries.forEach { day ->
                FilterChip(
                    selected = day in form.days,
                    onClick = { onDayToggled(day) },
                    // "Mon" would be read as a word; the full name is what a screen reader says. Set
                    // on the label, which the chip merges into its own node, so it stays one stop.
                    label = {
                        Text(dates.weekdayShort(day), modifier = Modifier.semantics { contentDescription = dates.weekdayFull(day) })
                    },
                    enabled = enabled,
                )
            }
        }
        if (form.days.isEmpty()) ValidationHint(stringResource(Res.string.alarm_hint_no_days))

        when (val schedule = form.schedule) {
            is AlarmSchedule.Daily -> {
                SectionHeading(stringResource(Res.string.alarm_section_time))
                TimeField(label = stringResource(Res.string.alarm_report_time), time = schedule.at, enabled = enabled, onTimeSelected = onTimeSelected)
            }
            is AlarmSchedule.Threshold -> {
                SectionHeading(stringResource(Res.string.alarm_section_window))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TimeField(
                        label = stringResource(Res.string.alarm_window_start),
                        time = schedule.from,
                        enabled = enabled,
                        onTimeSelected = onWindowStartSelected,
                    )
                    // Decorative: each button names its own end of the window.
                    Text("–", modifier = Modifier.clearAndSetSemantics {})
                    TimeField(
                        label = stringResource(Res.string.alarm_window_end),
                        time = schedule.until,
                        enabled = enabled,
                        onTimeSelected = onWindowEndSelected,
                    )
                }
                if (!form.isWindowValid) ValidationHint(stringResource(Res.string.alarm_hint_window))
            }
        }
        Text(
            stringResource(Res.string.alarm_swiss_time),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val failure = state.saveError?.message(ErrorContext.SAVE_ALARM)
            ?: state.deleteError?.message(ErrorContext.DELETE_ALARM)
        failure?.let { text ->
            Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = onSave,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
            if (state.isSaving) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(stringResource(Res.string.alarm_saving))
                }
            } else {
                Text(stringResource(Res.string.alarm_save))
            }
        }

        if (state.canDelete) {
            OutlinedButton(
                onClick = onDeleteRequested,
                enabled = !state.isBusy,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                if (state.isDeleting) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(stringResource(Res.string.alarm_deleting))
                    }
                } else {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(Res.string.alarm_delete), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

/** Daily report or threshold alert, as two segments; each announces its own selected state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeToggle(
    selected: AlarmType,
    enabled: Boolean,
    onTypeSelected: (AlarmType) -> Unit,
) {
    val options = listOf(AlarmType.DAILY to stringResource(Res.string.alarm_type_daily), AlarmType.THRESHOLD to stringResource(Res.string.alarm_type_threshold))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (type, label) ->
            SegmentedButton(
                selected = type == selected,
                onClick = { onTypeSelected(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                enabled = enabled,
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
    )
}

@Composable
private fun ValidationHint(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
        OutlinedTextField(
            value = stations.firstOrNull { it.abbr == selectedAbbr }?.name ?: selectedAbbr,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(stringResource(Res.string.alarm_section_station)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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

/** A button showing [time] that opens a 24-hour time picker in a dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(
    label: String,
    time: LocalTime,
    enabled: Boolean,
    onTimeSelected: (LocalTime) -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val description = stringResource(Res.string.alarm_time_description, label, formatTime(time))
    OutlinedButton(
        onClick = { showPicker = true },
        enabled = enabled,
        // Read as "Report time 08:00" rather than a bare time.
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(formatTime(time), modifier = Modifier.padding(start = 8.dp))
    }

    if (showPicker) {
        val pickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(label) },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeSelected(LocalTime(pickerState.hour, pickerState.minute))
                        showPicker = false
                    },
                ) { Text(stringResource(Res.string.alarm_time_picker_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(Res.string.common_cancel)) }
            },
        )
    }
}

