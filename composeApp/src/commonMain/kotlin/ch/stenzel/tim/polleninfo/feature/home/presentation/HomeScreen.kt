package ch.stenzel.tim.polleninfo.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.feature.home.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.feature.home.domain.model.SpeciesReading
import org.koin.compose.viewmodel.koinViewModel

/**
 * The pollen situation at the station the user chose.
 *
 * There is deliberately no way to change the station from here — the pin in the top bar is
 * decorative. Station changes belong to the planned settings feature; until then, clearing the
 * app's data is the only route back to onboarding.
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(uiState = uiState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(uiState: HomeUiState) {
    Scaffold(
        topBar = {
            // Outside the `when`: the station name is on every state, so the bar never goes blank
            // and the screen is identifiable while the readings are still loading.
            TopAppBar(title = { StationTitle(uiState.stationName) })
        },
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)
        when (uiState) {
            is HomeUiState.Loading -> CenteredBox(modifier) { CircularProgressIndicator() }
            is HomeUiState.Content -> ReadingView(uiState, modifier)
            is HomeUiState.Error -> CenteredBox(modifier) { ErrorView(uiState.message) }
        }
    }
}

@Composable
private fun CenteredBox(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun ReadingView(content: HomeUiState.Content, modifier: Modifier) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        OverallSeverityView(content.overallSeverity, content.drivenBy)
        HorizontalDivider(Modifier.padding(vertical = 24.dp))
        SpeciesListHeading(content.unit)
        content.species.forEach { reading ->
            Spacer(Modifier.height(16.dp))
            SpeciesRow(reading)
        }
    }
}

@Composable
private fun StationTitle(stationName: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            // Decorative: the name beside it already says what this is, and the pin is not
            // actionable, so announcing it would only add noise to a screen reader.
            contentDescription = null,
        )
        Spacer(Modifier.width(8.dp))
        Text(stationName)
    }
}

@Composable
private fun OverallSeverityView(severity: PollenSeverity, drivenBy: String?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = severity.label(), style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        SeverityBar(severity, SeverityBarSize.Large, Modifier.fillMaxWidth())
        // The overall severity is an aggregate with no concentration of its own; without naming
        // its source, the user would have to scan the list to learn what is high.
        if (drivenBy != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Driven by $drivenBy",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The unit is stated once here, so the numbers on the rows can stay bare and scannable. */
@Composable
private fun SpeciesListHeading(unit: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(
            text = "All species",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "concentration in $unit",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Two lines: the name, then the bar, the severity word and the concentration.
 *
 * A taxon the station does not report says "No data" with a dash, never "None" with a 0 — those
 * are a measurement of clean air, and a user who reacts to this taxon must not mistake one for the
 * other.
 */
@Composable
private fun SpeciesRow(reading: SpeciesReading) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = reading.name, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SeverityBar(reading.severity, SeverityBarSize.Compact, Modifier.weight(1f))
            Spacer(Modifier.width(16.dp))
            // Fixed widths, so the bars all end at the same x and the words and numbers each form
            // a column down the list.
            Text(
                text = reading.severity?.label() ?: "No data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(SEVERITY_WORD_WIDTH),
            )
            Text(
                text = reading.concentration?.toString() ?: "–",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.width(CONCENTRATION_WIDTH),
            )
        }
    }
}

@Composable
private fun ErrorView(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "The pollen readings could not be loaded.",
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
    }
}

private val SEVERITY_WORD_WIDTH = 88.dp
private val CONCENTRATION_WIDTH = 56.dp

/**
 * The single home of the severity wording. Every severity is spelled out as a word — the screen
 * must stay fully readable to someone who cannot tell its colours apart.
 */
private fun PollenSeverity.label(): String = when (this) {
    PollenSeverity.NONE -> "None"
    PollenSeverity.LOW -> "Low"
    PollenSeverity.MODERATE -> "Moderate"
    PollenSeverity.HIGH -> "High"
    PollenSeverity.VERY_HIGH -> "Very high"
}
