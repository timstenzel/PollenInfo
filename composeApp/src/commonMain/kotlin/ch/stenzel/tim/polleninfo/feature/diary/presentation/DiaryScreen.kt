package ch.stenzel.tim.polleninfo.feature.diary.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.core.species.domain.model.Species
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.ui.species.speciesColor
import ch.stenzel.tim.polleninfo.feature.diary.chart.DiaryChart
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Diary tab: the user's answers and the daily pollen levels at a station — the home station
 * until another is chosen — over the last week, month or year. A period without answers shows a
 * hint over the chart pointing to Home. Below the chart, one checkbox per pollen type switches its
 * line on and off and doubles as the legend.
 *
 * Scrolls as a whole and has no pull-to-refresh. The note that the diary is no diagnosis is part of
 * every state with a graph, tied to the explanatory line by its asterisk.
 */
@Composable
fun DiaryScreen(viewModel: DiaryViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DiaryContent(
        uiState = uiState,
        actions = DiaryActions(
            onStationSelected = viewModel::onStationSelected,
            onRangeSelected = viewModel::onRangeSelected,
            onSpeciesToggled = viewModel::onSpeciesToggled,
        ),
        onRetry = viewModel::retry,
    )
}

private class DiaryActions(
    val onStationSelected: (String) -> Unit,
    val onRangeSelected: (HistoryRange) -> Unit,
    val onSpeciesToggled: (String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryContent(
    uiState: DiaryUiState,
    actions: DiaryActions,
    onRetry: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Diary") }) }) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (uiState) {
            is DiaryUiState.Loading -> CenteredBox(modifier) { CircularProgressIndicator() }
            is DiaryUiState.Content -> DiaryView(uiState, actions, modifier)
            is DiaryUiState.Error -> CenteredBox(modifier) { ErrorView(uiState.message, onRetry) }
        }
    }
}

@Composable
private fun DiaryView(content: DiaryUiState.Content, actions: DiaryActions, modifier: Modifier) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
        StationDropdown(stations = content.stations, selectedAbbr = content.stationAbbr, onSelected = actions.onStationSelected)
        Spacer(Modifier.height(16.dp))
        RangeSelector(selected = content.range, onSelected = actions.onRangeSelected)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Compare how you felt with the pollen levels at a station.*",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        // Reserved even when idle, so the chart does not jump when a load starts.
        Box(Modifier.fillMaxWidth().height(4.dp)) {
            if (content.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(8.dp))
        Box {
            DiaryChart(
                days = content.history.days,
                speciesIds = content.shownSpeciesIds,
                range = content.historyRange,
                entries = content.entries,
            )
            // Outside the chart's single semantics node, so a screen reader reads it on its own.
            if (content.hasNoEntries) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 48.dp, vertical = 16.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp,
                ) {
                    Text(
                        text = NO_ENTRIES_HINT,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Pollen types",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(4.dp))
        val notMeasured = content.notMeasured
        content.species.forEach { species ->
            SpeciesCheckboxRow(
                species = species,
                checked = species.id in content.checked,
                measured = species.id !in notMeasured,
                onToggled = { actions.onSpeciesToggled(species.id) },
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = DISCLAIMER,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Any station; choosing one here never changes the home station. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationDropdown(stations: List<Station>, selectedAbbr: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = stations.firstOrNull { it.abbr == selectedAbbr }?.name ?: selectedAbbr,
            onValueChange = {},
            readOnly = true,
            label = { Text("Station") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            stations.forEach { station ->
                DropdownMenuItem(
                    text = { Text(station.name) },
                    onClick = {
                        onSelected(station.abbr)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

/**
 * One pollen type's checkbox, with a sample of its line's colour so the rows are also the chart's
 * legend. A type the station does not measure in the period is disabled and shown unchecked with
 * "Not measured here" — no line is drawn for it, and a missing line must not read as "no pollen".
 * Its checked choice is kept, so it is drawn again at a station that measures it.
 *
 * The whole row is one toggleable focus stop; the colour sample is decoration.
 */
@Composable
private fun SpeciesCheckboxRow(species: Species, checked: Boolean, measured: Boolean, onToggled: () -> Unit) {
    val shownChecked = checked && measured
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = shownChecked, enabled = measured, role = Role.Checkbox, onValueChange = { onToggled() }),
    ) {
        Checkbox(checked = shownChecked, onCheckedChange = null, enabled = measured)
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .size(width = 20.dp, height = 4.dp)
                .alpha(if (measured) 1f else DISABLED_ALPHA)
                .background(speciesColor(species.id) ?: MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = species.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (measured) 1f else DISABLED_ALPHA),
            )
            if (!measured) {
                Text(
                    text = "Not measured here",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeSelector(selected: HistoryRange, onSelected: (HistoryRange) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        HistoryRange.entries.forEachIndexed { index, range ->
            SegmentedButton(
                selected = range == selected,
                onClick = { onSelected(range) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = HistoryRange.entries.size),
            ) {
                Text(range.label())
            }
        }
    }
}

private fun HistoryRange.label(): String = when (this) {
    HistoryRange.WEEK -> "Week"
    HistoryRange.MONTH -> "Month"
    HistoryRange.YEAR -> "Year"
}

@Composable
private fun CenteredBox(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "The pollen history could not be loaded.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

/** Material's opacity for disabled content. */
private const val DISABLED_ALPHA = 0.38f

private const val NO_ENTRIES_HINT = "Answer 'How do you feel today?' on Home to see your line here."

private const val DISCLAIMER =
    "* This is not a medical diagnosis. If you suspect a pollen allergy, please see a doctor."
