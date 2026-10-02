package ch.stenzel.tim.polleninfo.feature.allstations.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.ReadingAge
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.readingAgeOf
import ch.stenzel.tim.polleninfo.core.ui.severity.ReadingAgeView
import ch.stenzel.tim.polleninfo.core.ui.severity.SeverityBar
import ch.stenzel.tim.polleninfo.core.ui.severity.SeverityBarSize
import ch.stenzel.tim.polleninfo.core.ui.severity.SpeciesListHeading
import ch.stenzel.tim.polleninfo.core.ui.severity.SpeciesRow
import ch.stenzel.tim.polleninfo.core.ui.severity.label
import ch.stenzel.tim.polleninfo.core.ui.severity.refreshedLabel
import ch.stenzel.tim.polleninfo.feature.allstations.domain.model.StationReading
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.compose.viewmodel.koinViewModel

/**
 * Every station's current reading, one row each, alphabetically.
 *
 * Browsing here never touches the station chosen during onboarding — Home keeps showing that one.
 */
@Composable
fun AllStationsScreen(viewModel: AllStationsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Hoisted here so the event collector below can drive it.
    val listState = rememberLazyListState()

    // One-shot events, not state: a selection scrolls the list exactly once, and never again on
    // recomposition or on returning to the tab with the selection still in place.
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AllStationsEvent.ScrollToStation -> {
                    val index = (viewModel.uiState.value as? AllStationsUiState.Content)
                        ?.stations?.indexOfFirst { it.station.abbr == event.abbr }
                        ?.takeIf { it >= 0 }
                    if (index != null) listState.revealExpandedItem(index)
                }
            }
        }
    }

    AllStationsContent(
        uiState = uiState,
        listState = listState,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onStationClick = viewModel::onStationClicked,
    )
}

/**
 * Scrolls as little as needed for the item at [index] to be fully visible once its expansion has
 * settled. An item taller than the list is aligned to its top instead, so its header is what shows.
 */
private suspend fun LazyListState.revealExpandedItem(index: Int) {
    // The row's height is only final once it has finished expanding (and the previous selection has
    // finished collapsing, which moves everything below it).
    delay(EXPAND_DURATION_MILLIS.milliseconds)
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (item == null) {
        animateScrollToItem(index)
        return
    }
    val top = item.offset - layoutInfo.viewportStartOffset
    val bottom = item.offset + item.size - layoutInfo.viewportEndOffset
    val distance = when {
        top < 0 -> top
        bottom > 0 -> minOf(bottom, top)
        else -> return
    }
    animateScrollBy(distance.toFloat())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllStationsContent(
    uiState: AllStationsUiState,
    listState: LazyListState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onStationClick: (String) -> Unit,
) {
    Scaffold(
        // The title matches the tab's accessibility name, so what is announced and what is shown
        // cannot disagree.
        topBar = { TopAppBar(title = { Text("All stations") }) },
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (uiState) {
            // Only until the station list is known; readings never hold the screen up.
            is AllStationsUiState.Loading -> CenteredBox(modifier) { CircularProgressIndicator() }

            is AllStationsUiState.Content -> ContentView(uiState, listState, onRefresh, onStationClick, modifier)

            is AllStationsUiState.Error -> CenteredBox(modifier) { ErrorView(uiState.message, onRetry) }
        }
    }
}

@Composable
private fun CenteredBox(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentView(
    content: AllStationsUiState.Content,
    listState: LazyListState,
    onRefresh: () -> Unit,
    onStationClick: (String) -> Unit,
    modifier: Modifier,
) {
    // Sampled at composition and deliberately not remembered, as on Home: staleness is a fact
    // about now, so a later recomposition must be free to escalate a row to stale.
    val now = Clock.System.now()
    BoxWithConstraints(modifier) {
        // The column's height, i.e. the screen between the top bar and the bottom bar — so the cap
        // is a share of what this screen actually has, in portrait, landscape and on small devices.
        val mapMaxHeight = maxHeight * MAP_MAX_HEIGHT_FRACTION
        Column(Modifier.fillMaxSize()) {
            // Outside the list and outside pull-to-refresh: the map stays put while the list scrolls.
            SwissMap(
                stations = content.stations,
                selectedAbbr = content.selectedAbbr,
                maxHeight = mapMaxHeight,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            // Moves on every completed round even when the backend's cache returns the same readings —
            // that is how the user sees that a refresh happened. Absent until the first round is in.
            content.refreshedAt?.let { refreshedAt ->
                Text(
                    text = refreshedLabel(refreshedAt, now),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
            // Only the list is inside pull-to-refresh; the rows stay composed while it runs.
            PullToRefreshBox(
                isRefreshing = content.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                StationList(
                    stations = content.stations,
                    selectedAbbr = content.selectedAbbr,
                    now = now,
                    listState = listState,
                    onStationClick = onStationClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun StationList(
    stations: List<StationReading>,
    selectedAbbr: String?,
    now: Instant,
    listState: LazyListState,
    onStationClick: (String) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(state = listState, modifier = modifier, contentPadding = PaddingValues(vertical = 8.dp)) {
        items(stations, key = { it.station.abbr }) { reading ->
            val abbr = reading.station.abbr
            val expanded = abbr == selectedAbbr
            StationRow(reading, expanded, now, onClick = { onStationClick(abbr) })
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(EXPAND_DURATION_MILLIS)) + fadeIn(tween(EXPAND_DURATION_MILLIS)),
                exit = shrinkVertically(tween(EXPAND_DURATION_MILLIS)) + fadeOut(tween(EXPAND_DURATION_MILLIS)),
            ) {
                StationDetail(reading, now)
            }
            HorizontalDivider(Modifier.padding(horizontal = 24.dp))
        }
    }
}

/**
 * Two lines: the station name, then the bar filled to its worst severity beside the word.
 *
 * One focus stop that toggles the station, announced with its name, severity and whether it is
 * expanded. The detail below it is deliberately outside this node, so a screen reader does not read
 * seven taxa as part of the row's name.
 */
@Composable
private fun StationRow(reading: StationReading, expanded: Boolean, now: Instant, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                stateDescription = if (expanded) "Expanded" else "Collapsed"
            }
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(text = reading.station.name, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            when (reading) {
                is StationReading.Pending -> {
                    PendingBar(Modifier.weight(1f))
                    Spacer(Modifier.width(16.dp))
                    SeverityWord("Loading…", muted = true)
                }

                is StationReading.Available -> {
                    val overview = reading.overview
                    SeverityBar(overview.overallSeverity, SeverityBarSize.Compact, Modifier.weight(1f))
                    Spacer(Modifier.width(16.dp))
                    SeverityWord(overview.overallSeverity.label())
                }

                // The empty muted track is the bar's own way of saying "no reading" — never None.
                is StationReading.Unavailable -> {
                    SeverityBar(null, SeverityBarSize.Compact, Modifier.weight(1f))
                    Spacer(Modifier.width(16.dp))
                    SeverityWord("No reading")
                }
            }
            // The collapsed row has no room for Home's full warning, but an old reading must still
            // never pass for a current one — the backend serves its last good reading through an
            // outage with no maximum age. The slot is reserved on every row so the bars line up.
            Box(Modifier.size(STALE_ICON_SIZE), contentAlignment = Alignment.Center) {
                if (reading is StationReading.Available &&
                    readingAgeOf(reading.overview.measuredAt, now) is ReadingAge.Stale
                ) {
                    StaleIcon()
                }
            }
        }
    }
}

/**
 * The selected station's reading in full: how current it is, then every taxon worst first, with the
 * same wording and styling as Home.
 */
@Composable
private fun StationDetail(reading: StationReading, now: Instant) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 16.dp)) {
        when (reading) {
            is StationReading.Available -> {
                val overview = reading.overview
                ReadingAgeView(readingAgeOf(overview.measuredAt, now))
                Spacer(Modifier.height(16.dp))
                SpeciesListHeading(overview.unit)
                overview.species.forEach { species ->
                    Spacer(Modifier.height(16.dp))
                    SpeciesRow(species)
                }
            }

            is StationReading.Pending -> DetailMessage("Loading this station's reading…")

            is StationReading.Unavailable -> DetailMessage("No reading available for this station right now.")
        }
    }
}

@Composable
private fun DetailMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Stands in for the bar while the reading is on its way: fainter than the empty "no reading" track,
 * so a station still loading is not mistaken for one that failed.
 */
@Composable
private fun PendingBar(modifier: Modifier) {
    Box(
        modifier
            .height(SeverityBarSize.Compact.height)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = PENDING_TRACK_ALPHA)),
    )
}

@Composable
private fun SeverityWord(text: String, muted: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.let { if (muted) it.copy(alpha = 0.6f) else it },
        modifier = Modifier.width(SEVERITY_WORD_WIDTH),
    )
}

@Composable
private fun StaleIcon() {
    Icon(
        imageVector = Icons.Default.Warning,
        // Announced, not decorative: a screen-reader user needs the same warning as a sighted one.
        contentDescription = "Reading not current",
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(STALE_ICON_SIZE),
    )
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "The stations could not be loaded.",
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

/** Wide enough for "Very high" and "No reading", so the bars all end at the same x. */
private val SEVERITY_WORD_WIDTH = 88.dp

private val STALE_ICON_SIZE = 20.dp

/** The map's share of the screen's content height at most, so the list keeps room below it. */
private const val MAP_MAX_HEIGHT_FRACTION = 0.4f

private const val PENDING_TRACK_ALPHA = 0.2f

/** Shared by the expand animation and the scroll that waits for it to settle. */
private const val EXPAND_DURATION_MILLIS = 250
