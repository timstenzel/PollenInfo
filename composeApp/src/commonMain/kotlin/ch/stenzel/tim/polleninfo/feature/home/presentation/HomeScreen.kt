package ch.stenzel.tim.polleninfo.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import ch.stenzel.tim.polleninfo.core.ui.layout.LocalBottomBarPadding
import ch.stenzel.tim.polleninfo.core.ui.layout.ScreenHeader
import ch.stenzel.tim.polleninfo.core.ui.layout.TabCenteredContent
import ch.stenzel.tim.polleninfo.core.ui.layout.TabPullToRefreshBox
import ch.stenzel.tim.polleninfo.core.ui.layout.TabScreenFrame
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
import ch.stenzel.tim.polleninfo.resources.home_overline
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The pollen situation at the station the user chose.
 *
 * There is deliberately no way to change the station from here — the pin before the title is
 * decorative. Station changes belong to Settings. [onOpenAccount] opens the login, when a feeling
 * is tapped while nobody is logged in.
 */
@Composable
fun HomeScreen(onOpenAccount: () -> Unit, viewModel: HomeViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                HomeEvent.OpenAccount -> onOpenAccount()
            }
        }
    }

    HomeContent(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onFeelingSelected = viewModel::onFeelingSelected,
        onFeelingPromptDismissed = viewModel::onFeelingPromptDismissed,
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onFeelingSelected: (Feeling) -> Unit,
    onFeelingPromptDismissed: () -> Unit,
) {
    TabScreenFrame { contentPadding ->
        // On every state: the station name is in the header, so the screen never goes blank and is
        // identifiable while the readings are still loading.
        val header = @Composable {
            ScreenHeader(
                overline = stringResource(Res.string.home_overline),
                title = uiState.stationName,
                icon = Icons.Default.LocationOn,
            )
        }
        when (uiState) {
            is HomeUiState.Loading -> TabCenteredContent(contentPadding, header) { CircularProgressIndicator() }

            // The readings stay composed while a refresh runs; only the indicator is added.
            is HomeUiState.Content -> TabPullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                topInset = contentPadding.calculateTopPadding(),
                modifier = Modifier.fillMaxSize(),
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
                ReadingView(
                    content = uiState,
                    contentPadding = contentPadding,
                    bottomInset = promptInset,
                    header = header,
                    modifier = Modifier.fillMaxSize(),
                )
                if (uiState.showFeelingPrompt) {
                    FeelingPrompt(
                        saveError = uiState.feelingSaveError,
                        onFeelingSelected = onFeelingSelected,
                        onDismiss = onFeelingPromptDismissed,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            // Above the glass tab bar, which the screen extends behind.
                            .padding(bottom = LocalBottomBarPadding.current)
                            .padding(PROMPT_MARGIN)
                            .fillMaxWidth()
                            .onSizeChanged { promptHeightPx = it.height },
                    )
                }
            }

            is HomeUiState.Error -> TabCenteredContent(contentPadding, header) {
                ErrorView(uiState.error.message(), onRetry)
            }
        }
    }
}

/** The gap around the floating feeling card. */
private val PROMPT_MARGIN = 16.dp

@Composable
private fun ReadingView(
    content: HomeUiState.Content,
    contentPadding: PaddingValues,
    bottomInset: Dp,
    header: @Composable () -> Unit,
    modifier: Modifier,
) {
    // Scrollable even when it fits, so the pull gesture has something to drag.
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp + bottomInset),
    ) {
        header()
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
