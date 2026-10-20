package ch.stenzel.tim.polleninfo.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.ReadingAge
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.SpeciesReading
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.readingAgeOf
import ch.stenzel.tim.polleninfo.core.ui.button.PillButton
import ch.stenzel.tim.polleninfo.core.ui.button.PillButtonStyle
import ch.stenzel.tim.polleninfo.core.ui.error.message
import ch.stenzel.tim.polleninfo.core.ui.format.rememberDateWording
import ch.stenzel.tim.polleninfo.core.ui.format.resolve
import ch.stenzel.tim.polleninfo.core.ui.layout.MeadowCard
import ch.stenzel.tim.polleninfo.core.ui.layout.ScreenHeader
import ch.stenzel.tim.polleninfo.core.ui.layout.TabPullToRefreshBox
import ch.stenzel.tim.polleninfo.core.ui.layout.TabScreenFrame
import ch.stenzel.tim.polleninfo.core.ui.severity.ReadingAgePill
import ch.stenzel.tim.polleninfo.core.ui.severity.SeverityGauge
import ch.stenzel.tim.polleninfo.core.ui.severity.SpeciesListHeading
import ch.stenzel.tim.polleninfo.core.ui.severity.SpeciesTile
import ch.stenzel.tim.polleninfo.core.ui.severity.refreshedLabel
import ch.stenzel.tim.polleninfo.core.ui.species.speciesName
import ch.stenzel.tim.polleninfo.core.ui.status.Banner
import ch.stenzel.tim.polleninfo.core.ui.status.StatusCard
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.common_retry
import ch.stenzel.tim.polleninfo.resources.home_driven_by
import ch.stenzel.tim.polleninfo.resources.home_load_failed
import ch.stenzel.tim.polleninfo.resources.home_overline
import ch.stenzel.tim.polleninfo.resources.reading_not_current
import kotlin.time.Clock
import kotlin.time.Instant
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
            is HomeUiState.Loading -> HomeColumn(contentPadding, Modifier.fillMaxSize()) {
                header()
                HomeSkeleton()
            }

            // The readings stay composed while a refresh runs; only the indicator is added.
            is HomeUiState.Content -> TabPullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                topInset = contentPadding.calculateTopPadding(),
                modifier = Modifier.fillMaxSize(),
            ) {
                // Scrollable even when it fits, so the pull gesture has something to drag.
                HomeColumn(contentPadding, Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    header()
                    ReadingView(
                        content = uiState,
                        onFeelingSelected = onFeelingSelected,
                        onFeelingPromptDismissed = onFeelingPromptDismissed,
                    )
                }
            }

            is HomeUiState.Error -> HomeColumn(contentPadding, Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                header()
                StatusCard(
                    title = stringResource(Res.string.home_load_failed),
                    body = uiState.error.message(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PillButton(onClick = onRetry, style = PillButtonStyle.Primary, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.common_retry))
                    }
                }
            }
        }
    }
}

/** Home's column inside the frame's [contentPadding], with the screen's side margins. */
@Composable
private fun HomeColumn(contentPadding: PaddingValues, modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier.padding(contentPadding).padding(start = SIDE_MARGIN, end = SIDE_MARGIN, bottom = 24.dp)) {
        content()
    }
}

private val SIDE_MARGIN = 20.dp

/** The vertical gap between Home's cards. */
private val SECTION_GAP = 20.dp

/**
 * From the top: the outdated-reading banner if it applies, the gauge card, the feeling card when it
 * is due, and the grid of species tiles, worst first.
 */
@Composable
private fun ReadingView(
    content: HomeUiState.Content,
    onFeelingSelected: (Feeling) -> Unit,
    onFeelingPromptDismissed: () -> Unit,
) {
    // Classified against the clock at composition, deliberately not remembered: the age is a
    // fact about now, so a later recomposition must be free to escalate it to stale.
    val now = Clock.System.now()
    val age = readingAgeOf(content.measuredAt, now)
    if (age is ReadingAge.Stale) {
        Banner(text = stringResource(Res.string.reading_not_current), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(SECTION_GAP))
    }
    GaugeCard(content, age, now)
    if (content.showFeelingPrompt) {
        Spacer(Modifier.height(SECTION_GAP))
        FeelingPrompt(
            saveError = content.feelingSaveError,
            onFeelingSelected = onFeelingSelected,
            onDismiss = onFeelingPromptDismissed,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(Modifier.height(SECTION_GAP))
    SpeciesListHeading(content.unit, titleStyle = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(SECTION_GAP))
    SpeciesGrid(content.species, content.unit)
}

/** The hero: the gauge with the worst severity, what drives it, and when the reading is from. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GaugeCard(content: HomeUiState.Content, age: ReadingAge, now: Instant) {
    MeadowCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SeverityGauge(content.overallSeverity)
            // The overall severity is an aggregate with no concentration of its own; without naming
            // its source, the user would have to scan the tiles to learn what is high.
            val drivenBy = content.drivenBy
            if (drivenBy != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.home_driven_by, speciesName(drivenBy.id, drivenBy.name)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(16.dp))
            // Side by side when they fit, stacked when a long language or a large font needs it.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                ReadingAgePill(age)
                Text(
                    text = refreshedLabel(content.refreshedAt, now, rememberDateWording()).resolve(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The species as tiles two to a row, the pair in a row as tall as the taller of the two. */
@Composable
private fun SpeciesGrid(species: List<SpeciesReading>, unit: String) {
    Column(verticalArrangement = Arrangement.spacedBy(TILE_GAP)) {
        species.chunked(2).forEach { pair ->
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
                pair.forEach { reading ->
                    SpeciesTile(reading, unit, Modifier.weight(1f).fillMaxHeight())
                }
                // An odd last tile keeps the width of a column.
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
