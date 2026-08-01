package ch.stenzel.tim.polleninfo.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
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
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                is HomeUiState.Loading -> CircularProgressIndicator()
                is HomeUiState.Content -> OverallSeverityView(uiState.overallSeverity)
                is HomeUiState.Error -> ErrorView(uiState.message)
            }
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
private fun OverallSeverityView(severity: PollenSeverity) {
    Text(text = severity.label(), style = MaterialTheme.typography.displaySmall)
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
