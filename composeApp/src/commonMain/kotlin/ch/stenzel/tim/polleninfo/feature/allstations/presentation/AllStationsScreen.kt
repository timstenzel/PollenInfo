package ch.stenzel.tim.polleninfo.feature.allstations.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.ReadingAge
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.readingAgeOf
import ch.stenzel.tim.polleninfo.core.ui.severity.SeverityBar
import ch.stenzel.tim.polleninfo.core.ui.severity.SeverityBarSize
import ch.stenzel.tim.polleninfo.core.ui.severity.label
import ch.stenzel.tim.polleninfo.core.ui.severity.refreshedLabel
import ch.stenzel.tim.polleninfo.feature.allstations.domain.model.StationReading
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

    AllStationsContent(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllStationsContent(
    uiState: AllStationsUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
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

            is AllStationsUiState.Content -> ContentView(uiState, onRefresh, modifier)

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
private fun ContentView(content: AllStationsUiState.Content, onRefresh: () -> Unit, modifier: Modifier) {
    // Sampled at composition and deliberately not remembered, as on Home: staleness is a fact
    // about now, so a later recomposition must be free to escalate a row to stale.
    val now = Clock.System.now()
    Column(modifier) {
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
            StationList(content.stations, now, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun StationList(stations: List<StationReading>, now: Instant, modifier: Modifier) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(vertical = 8.dp)) {
        items(stations, key = { it.station.abbr }) { reading ->
            StationRow(reading, now)
            HorizontalDivider(Modifier.padding(horizontal = 24.dp))
        }
    }
}

/** Two lines: the station name, then the bar filled to its worst severity beside the word. */
@Composable
private fun StationRow(reading: StationReading, now: Instant) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
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

private const val PENDING_TRACK_ALPHA = 0.2f
