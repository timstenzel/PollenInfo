package ch.stenzel.tim.polleninfo.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.SpeciesReading
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.readingAgeOf
import ch.stenzel.tim.polleninfo.core.ui.error.message
import ch.stenzel.tim.polleninfo.core.ui.format.rememberDateWording
import ch.stenzel.tim.polleninfo.core.ui.format.resolve
import ch.stenzel.tim.polleninfo.core.ui.severity.ReadingAgeView
import ch.stenzel.tim.polleninfo.core.ui.severity.SeverityBar
import ch.stenzel.tim.polleninfo.core.ui.severity.SeverityBarSize
import ch.stenzel.tim.polleninfo.core.ui.severity.SpeciesListHeading
import ch.stenzel.tim.polleninfo.core.ui.severity.SpeciesRow
import ch.stenzel.tim.polleninfo.core.ui.severity.label
import ch.stenzel.tim.polleninfo.core.ui.severity.refreshedLabel
import ch.stenzel.tim.polleninfo.core.ui.species.speciesName
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.common_retry
import ch.stenzel.tim.polleninfo.resources.home_driven_by
import ch.stenzel.tim.polleninfo.resources.home_load_failed
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The pollen situation at the station the user chose.
 *
 * There is deliberately no way to change the station from here — the pin in the top bar is
 * decorative. Station changes belong to Settings.
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onFeelingSelected = viewModel::onFeelingSelected,
        onFeelingPromptDismissed = viewModel::onFeelingPromptDismissed,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onFeelingSelected: (Feeling) -> Unit,
    onFeelingPromptDismissed: () -> Unit,
) {
    Scaffold(
        topBar = {
            // Outside the `when`: the station name is on every state, so the bar never goes blank
            // and the screen is identifiable while the readings are still loading.
            TopAppBar(title = { StationTitle(uiState.stationName) })
        },
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (uiState) {
            is HomeUiState.Loading -> CenteredBox(modifier) { CircularProgressIndicator() }

            // The readings stay composed while a refresh runs; only the indicator is added.
            is HomeUiState.Content -> PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = modifier,
            ) {
                // The card floats over the list rather than taking space from it; while it is up,
                // the list's end grows by the card's height so the last row can still be scrolled
                // fully above it.
                var promptHeightPx by remember { mutableIntStateOf(0) }
                val promptInset = if (uiState.showFeelingPrompt) {
                    with(LocalDensity.current) { promptHeightPx.toDp() } + PROMPT_MARGIN
                } else {
                    0.dp
                }
                ReadingView(uiState, bottomInset = promptInset, modifier = Modifier.fillMaxSize())
                if (uiState.showFeelingPrompt) {
                    FeelingPrompt(
                        saveError = uiState.feelingSaveError,
                        onFeelingSelected = onFeelingSelected,
                        onDismiss = onFeelingPromptDismissed,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(PROMPT_MARGIN)
                            .fillMaxWidth()
                            .onSizeChanged { promptHeightPx = it.height },
                    )
                }
            }

            is HomeUiState.Error -> CenteredBox(modifier) { ErrorView(uiState.error.message(), onRetry) }
        }
    }
}

/** The gap around the floating feeling card. */
private val PROMPT_MARGIN = 16.dp

@Composable
private fun CenteredBox(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun ReadingView(content: HomeUiState.Content, bottomInset: Dp, modifier: Modifier) {
    // Scrollable even when it fits, so the pull gesture has something to drag.
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 24.dp + bottomInset),
    ) {
        // Classified against the clock at composition, deliberately not remembered: the age is a
        // fact about now, so a later recomposition must be free to escalate it to stale.
        val now = Clock.System.now()
        ReadingAgeView(readingAgeOf(content.measuredAt, now))
        Spacer(Modifier.height(8.dp))
        Text(
            text = refreshedLabel(content.refreshedAt, now, rememberDateWording()).resolve(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
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
private fun OverallSeverityView(severity: PollenSeverity, drivenBy: SpeciesReading?) {
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
                text = stringResource(Res.string.home_driven_by, speciesName(drivenBy.id, drivenBy.name)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.home_load_failed),
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
        Button(onClick = onRetry) { Text(stringResource(Res.string.common_retry)) }
    }
}
