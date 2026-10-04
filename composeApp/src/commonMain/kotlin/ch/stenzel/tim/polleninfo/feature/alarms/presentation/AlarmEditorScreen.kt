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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmFormState
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmSchedule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.koin.compose.viewmodel.koinViewModel

/**
 * Creates a daily report. Shown without the bottom bar — it is a task with a clear way back, not a
 * tab. [onDone] leaves the editor, after a save or when the user goes back.
 */
@Composable
fun AlarmEditorScreen(
    onDone: () -> Unit,
    viewModel: AlarmEditorViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AlarmEditorEvent.Done -> onDone()
            }
        }
    }

    AlarmEditorContent(
        uiState = uiState,
        onBack = onDone,
        onRetry = viewModel::retry,
        onStationSelected = viewModel::onStationSelected,
        onSpeciesToggled = viewModel::onSpeciesToggled,
        onMinSeveritySelected = viewModel::onMinSeveritySelected,
        onDayToggled = viewModel::onDayToggled,
        onTimeSelected = viewModel::onTimeSelected,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditorContent(
    uiState: AlarmEditorUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onStationSelected: (String) -> Unit,
    onSpeciesToggled: (String) -> Unit,
    onMinSeveritySelected: (PollenSeverity) -> Unit,
    onDayToggled: (DayOfWeek) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New alarm") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    "The alarm editor could not be loaded.",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    uiState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onRetry) { Text("Retry") }
            }

            is AlarmEditorUiState.Editing -> EditingForm(
                state = uiState,
                onStationSelected = onStationSelected,
                onSpeciesToggled = onSpeciesToggled,
                onMinSeveritySelected = onMinSeveritySelected,
                onDayToggled = onDayToggled,
                onTimeSelected = onTimeSelected,
                onSave = onSave,
                modifier = modifier,
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
    onTimeSelected: (LocalTime) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier,
) {
    val form = state.form
    val enabled = !state.isSaving
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Daily report", style = MaterialTheme.typography.titleMedium)
        Text(
            "A summary of the selected pollen types at one time on the chosen days.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionHeading("Station")
        StationDropdown(
            stations = state.stations,
            selectedAbbr = form.stationAbbr,
            enabled = enabled,
            onStationSelected = onStationSelected,
        )

        SectionHeading("Pollen types")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.species.forEach { species ->
                FilterChip(
                    selected = species.id in form.species,
                    onClick = { onSpeciesToggled(species.id) },
                    label = { Text(species.name) },
                    enabled = enabled,
                )
            }
        }
        if (form.species.isEmpty()) ValidationHint("Select at least one pollen type.")

        SectionHeading("Send only from")
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
            if (form.minSeverity == PollenSeverity.NONE) {
                "Sent every chosen day."
            } else {
                "Sent only when a selected type is at least ${form.minSeverity.minimumLabel()}."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionHeading("Days")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DayOfWeek.entries.forEach { day ->
                FilterChip(
                    selected = day in form.days,
                    onClick = { onDayToggled(day) },
                    // "Mon" would be read as a word; the full name is what a screen reader says. Set
                    // on the label, which the chip merges into its own node, so it stays one stop.
                    label = {
                        Text(day.shortName(), modifier = Modifier.semantics { contentDescription = day.fullName() })
                    },
                    enabled = enabled,
                )
            }
        }
        if (form.days.isEmpty()) ValidationHint("Select at least one day.")

        SectionHeading("Time")
        val at = (form.schedule as? AlarmSchedule.Daily)?.at ?: AlarmFormState.DEFAULT_DAILY_TIME
        TimeField(label = "Report time", time = at, enabled = enabled, onTimeSelected = onTimeSelected)
        Text(
            "Times are Swiss time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.saveError?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = onSave,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
            if (state.isSaving) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Saving…")
                }
            } else {
                Text("Save")
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
            label = { Text("Station") },
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
    OutlinedButton(
        onClick = { showPicker = true },
        enabled = enabled,
        // Read as "Report time 08:00" rather than a bare time.
        modifier = Modifier.semantics { contentDescription = "$label ${formatTime(time)}" },
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
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        )
    }
}

private fun DayOfWeek.fullName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
