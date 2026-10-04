package ch.stenzel.tim.polleninfo.feature.diary.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
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
import ch.stenzel.tim.polleninfo.feature.diary.chart.DiaryChart
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Diary tab: the daily pollen levels at the home station over the last 30 days.
 *
 * Scrolls as a whole and has no pull-to-refresh. The note that the diary is no diagnosis is part of
 * every state with a graph, tied to the explanatory line by its asterisk.
 */
@Composable
fun DiaryScreen(viewModel: DiaryViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DiaryContent(uiState = uiState, onRetry = viewModel::retry)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryContent(uiState: DiaryUiState, onRetry: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Diary") }) }) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (uiState) {
            is DiaryUiState.Loading -> CenteredBox(modifier) { CircularProgressIndicator() }
            is DiaryUiState.Content -> DiaryView(uiState, modifier)
            is DiaryUiState.Error -> CenteredBox(modifier) { ErrorView(uiState.message, onRetry) }
        }
    }
}

@Composable
private fun DiaryView(content: DiaryUiState.Content, modifier: Modifier) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text(text = content.stationName, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
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
        DiaryChart(
            days = content.history.days,
            speciesIds = content.speciesIds,
            range = content.range,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = DISCLAIMER,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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

private const val DISCLAIMER =
    "* This is not a medical diagnosis. If you suspect a pollen allergy, please see a doctor."
