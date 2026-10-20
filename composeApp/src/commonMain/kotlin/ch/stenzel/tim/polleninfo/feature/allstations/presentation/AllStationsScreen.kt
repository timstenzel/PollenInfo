package ch.stenzel.tim.polleninfo.feature.allstations.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.ReadingAge
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.readingAgeOf
import ch.stenzel.tim.polleninfo.core.ui.button.PillButton
import ch.stenzel.tim.polleninfo.core.ui.button.PillButtonStyle
import ch.stenzel.tim.polleninfo.core.ui.error.message
import ch.stenzel.tim.polleninfo.core.ui.format.rememberDateWording
import ch.stenzel.tim.polleninfo.core.ui.format.resolve
import ch.stenzel.tim.polleninfo.core.ui.layout.MeadowCard
import ch.stenzel.tim.polleninfo.core.ui.layout.ScreenHeader
import ch.stenzel.tim.polleninfo.core.ui.layout.TabScreenFrame
import ch.stenzel.tim.polleninfo.core.ui.severity.ReadingAgePill
import ch.stenzel.tim.polleninfo.core.ui.severity.SpeciesListHeading
import ch.stenzel.tim.polleninfo.core.ui.severity.SpeciesTileGrid
import ch.stenzel.tim.polleninfo.core.ui.severity.color
import ch.stenzel.tim.polleninfo.core.ui.severity.label
import ch.stenzel.tim.polleninfo.core.ui.severity.refreshedLabel
import ch.stenzel.tim.polleninfo.core.ui.species.speciesName
import ch.stenzel.tim.polleninfo.core.ui.status.StatusCard
import ch.stenzel.tim.polleninfo.feature.allstations.domain.model.StationReading
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.all_stations_collapsed
import ch.stenzel.tim.polleninfo.resources.all_stations_detail_loading
import ch.stenzel.tim.polleninfo.resources.all_stations_detail_unavailable
import ch.stenzel.tim.polleninfo.resources.all_stations_expanded
import ch.stenzel.tim.polleninfo.resources.all_stations_list_heading
import ch.stenzel.tim.polleninfo.resources.all_stations_load_failed
import ch.stenzel.tim.polleninfo.resources.all_stations_overline
import ch.stenzel.tim.polleninfo.resources.all_stations_row_loading
import ch.stenzel.tim.polleninfo.resources.all_stations_row_no_reading
import ch.stenzel.tim.polleninfo.resources.all_stations_stale
import ch.stenzel.tim.polleninfo.resources.common_retry
import ch.stenzel.tim.polleninfo.resources.nav_all_stations
import ch.stenzel.tim.polleninfo.theme.meadowColors
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
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
                    if (index != null) listState.revealExpandedItem(LIST_HEADING_ITEMS + index)
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
    // The list extends behind the glass tab bar; only what is above its end padding counts as visible.
    val bottom = item.offset + item.size - (layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding)
    val distance = when {
        top < 0 -> top
        bottom > 0 -> minOf(bottom, top)
        else -> return
    }
    animateScrollBy(distance.toFloat())
}

@Composable
private fun AllStationsContent(
    uiState: AllStationsUiState,
    listState: LazyListState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onStationClick: (String) -> Unit,
) {
    TabScreenFrame { contentPadding ->
        val header = @Composable {
            ScreenHeader(
                // The count is only known once the station list is; until then the line stays empty.
                overline = (uiState as? AllStationsUiState.Content)?.stations?.size?.let { count ->
                    pluralStringResource(Res.plurals.all_stations_overline, count, count)
                },
                // The tab's own name, so what is announced and what is shown cannot disagree.
                title = stringResource(Res.string.nav_all_stations),
            )
        }
        when (uiState) {
            // Only until the station list is known; readings never hold the screen up.
            is AllStationsUiState.Loading -> Column(Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = SIDE_MARGIN)) {
                header()
                AllStationsSkeleton()
            }

            is AllStationsUiState.Content -> ContentView(
                content = uiState,
                listState = listState,
                contentPadding = contentPadding,
                header = header,
                onRefresh = onRefresh,
                onStationClick = onStationClick,
            )

            is AllStationsUiState.Error -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding)
                    .padding(start = SIDE_MARGIN, end = SIDE_MARGIN, bottom = 24.dp),
            ) {
                header()
                StatusCard(
                    title = stringResource(Res.string.all_stations_load_failed),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentView(
    content: AllStationsUiState.Content,
    listState: LazyListState,
    contentPadding: PaddingValues,
    header: @Composable () -> Unit,
    onRefresh: () -> Unit,
    onStationClick: (String) -> Unit,
) {
    // Sampled at composition and deliberately not remembered, as on Home: staleness is a fact
    // about now, so a later recomposition must be free to escalate a row to stale.
    val now = Clock.System.now()
    // Only the top and the sides: the list runs on behind the glass tab bar and pads its own end.
    val layoutDirection = LocalLayoutDirection.current
    val framePadding = PaddingValues(
        start = contentPadding.calculateStartPadding(layoutDirection),
        top = contentPadding.calculateTopPadding(),
        end = contentPadding.calculateEndPadding(layoutDirection),
    )
    BoxWithConstraints(Modifier.fillMaxSize().padding(framePadding)) {
        // The column's height, i.e. the screen below the status bar — so the cap is a share of what
        // this screen actually has, in portrait, landscape and on small devices.
        val mapMaxHeight = maxHeight * MAP_MAX_HEIGHT_FRACTION
        Column(Modifier.fillMaxSize()) {
            // Fixed with the map: the map stays put while the list scrolls.
            Box(Modifier.padding(horizontal = SIDE_MARGIN)) { header() }
            // Outside the list and outside pull-to-refresh: the map stays put while the list scrolls.
            MapCard(
                content = content,
                mapMaxHeight = mapMaxHeight,
                onStationClick = onStationClick,
                modifier = Modifier.padding(horizontal = SIDE_MARGIN).fillMaxWidth(),
            )
            // Only the list is inside pull-to-refresh; the rows stay composed while it runs.
            PullToRefreshBox(
                isRefreshing = content.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                StationList(
                    content = content,
                    now = now,
                    listState = listState,
                    bottomPadding = contentPadding.calculateBottomPadding(),
                    onStationClick = onStationClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** The map with its legend in a card (Figma "MapCard"). */
@Composable
private fun MapCard(
    content: AllStationsUiState.Content,
    mapMaxHeight: Dp,
    onStationClick: (String) -> Unit,
    modifier: Modifier,
) {
    val selected = content.stations.firstOrNull { it.station.abbr == content.selectedAbbr }
    val selectedLabel = selected?.let { "${it.station.name}$SEPARATOR${stateWord(it)}" }
    MeadowCard(modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SwissMap(
                stations = content.stations,
                selectedAbbr = content.selectedAbbr,
                selectedLabel = selectedLabel,
                maxHeight = mapMaxHeight,
                onStationClick = onStationClick,
            )
            Legend(Modifier.fillMaxWidth())
        }
    }
}

/**
 * Every severity's dot beside its word, so the map's colours can be read without the list.
 *
 * Silent to a screen reader: it explains colours, and the map is announced as one description and
 * every row says its level in words.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(modifier: Modifier) {
    FlowRow(
        modifier = modifier.clearAndSetSemantics {},
        // Spread across the card as drawn; a long language or a large font wraps to a second line.
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        PollenSeverity.entries.forEach { severity ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(LEGEND_DOT_SIZE).background(severity.color(), CircleShape))
                Spacer(Modifier.width(5.dp))
                Text(
                    text = severity.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StationList(
    content: AllStationsUiState.Content,
    now: Instant,
    listState: LazyListState,
    bottomPadding: Dp,
    onStationClick: (String) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(start = SIDE_MARGIN, end = SIDE_MARGIN, bottom = 24.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(ROW_GAP),
    ) {
        // Counted in LIST_HEADING_ITEMS, so a station's index in the list is known for the scroll.
        item(key = LIST_HEADING_KEY) {
            ListHeading(content.refreshedAt, now)
        }
        items(content.stations, key = { it.station.abbr }) { reading ->
            val abbr = reading.station.abbr
            StationCard(reading, expanded = abbr == content.selectedAbbr, now, onClick = { onStationClick(abbr) })
        }
    }
}

/**
 * "Stations" with, at the end, when the last round of readings came in. That time moves on every
 * completed round even when the backend's cache returns the same readings — that is how the user
 * sees that a refresh happened. Absent until the first round is in.
 */
@Composable
private fun ListHeading(refreshedAt: Instant?, now: Instant) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = stringResource(Res.string.all_stations_list_heading),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (refreshedAt != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = refreshedLabel(refreshedAt, now, rememberDateWording()).resolve(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

/**
 * One station as a rounded card: the severity dot, the name over "level · driving type", the slot
 * for the outdated-reading icon and the expand chevron. The selected station's card is sage with a
 * green edge and shows its reading in full below.
 *
 * The header is one focus stop that toggles the station, announced with its name, its line and
 * whether it is expanded. The detail is deliberately outside that node, so a screen reader does not
 * read seven taxa as part of the row's name.
 */
@Composable
private fun StationCard(reading: StationReading, expanded: Boolean, now: Instant, onClick: () -> Unit) {
    // Resolved here: the semantics block is not composable.
    val expandedWord = stringResource(Res.string.all_stations_expanded)
    val collapsedWord = stringResource(Res.string.all_stations_collapsed)
    val shape = MaterialTheme.shapes.medium
    val stale = reading is StationReading.Available && readingAgeOf(reading.overview.measuredAt, now) is ReadingAge.Stale
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (expanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest)
            .then(if (expanded) Modifier.border(SELECTED_EDGE, MaterialTheme.colorScheme.primary, shape) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .semantics(mergeDescendants = true) {
                    stateDescription = if (expanded) expandedWord else collapsedWord
                }
                .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SeverityDot(reading)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = reading.station.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = rowLine(reading, stale),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(14.dp))
            // The collapsed row has no room for Home's banner, but an old reading must still never
            // pass for a current one — the backend serves its last good reading through an outage
            // with no maximum age. The slot is reserved on every row so the chevrons line up.
            Box(Modifier.size(STALE_SLOT_SIZE), contentAlignment = Alignment.Center) {
                if (stale) StaleIcon()
            }
            Spacer(Modifier.width(14.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                // Decorative: the row's state description says whether it is expanded.
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(CHEVRON_SIZE),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(EXPAND_DURATION_MILLIS)) + fadeIn(tween(EXPAND_DURATION_MILLIS)),
            exit = shrinkVertically(tween(EXPAND_DURATION_MILLIS)) + fadeOut(tween(EXPAND_DURATION_MILLIS)),
        ) {
            StationDetail(reading, now)
        }
    }
}

/**
 * The row's second line: the level and what drives it, or — for an outdated reading — the level
 * and that it is not current, said in words beside the icon. A station without a reading says
 * whether it is still loading or has none.
 */
@Composable
private fun rowLine(reading: StationReading, stale: Boolean): String = when (reading) {
    is StationReading.Available -> {
        val overview = reading.overview
        val second = when {
            stale -> stringResource(Res.string.all_stations_stale)
            else -> overview.drivenBy?.let { speciesName(it.id, it.name) }
        }
        listOfNotNull(overview.overallSeverity.label(), second).joinToString(SEPARATOR)
    }

    else -> stateWord(reading)
}

/** The level, or the word that stands in for it while there is none. */
@Composable
private fun stateWord(reading: StationReading): String = when (reading) {
    is StationReading.Available -> reading.overview.overallSeverity.label()
    is StationReading.Pending -> stringResource(Res.string.all_stations_row_loading)
    is StationReading.Unavailable -> stringResource(Res.string.all_stations_row_no_reading)
}

/**
 * The severity colour, the same function as the map's dot. Without a level — still loading or no
 * reading — a grey ring, never a colour; the words beside it tell the two apart.
 */
@Composable
private fun SeverityDot(reading: StationReading) {
    val modifier = Modifier.size(ROW_DOT_SIZE)
    when (reading) {
        is StationReading.Available -> Box(modifier.background(reading.overview.overallSeverity.color(), CircleShape))

        else -> Box(
            modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        )
    }
}

/**
 * The selected station's reading in full: how current it is, then every taxon worst first as tiles,
 * with the same wording and styling as Home.
 */
@Composable
private fun StationDetail(reading: StationReading, now: Instant) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
        when (reading) {
            is StationReading.Available -> {
                val overview = reading.overview
                Box(Modifier.padding(start = 4.dp)) { ReadingAgePill(readingAgeOf(overview.measuredAt, now)) }
                Spacer(Modifier.height(16.dp))
                Box(Modifier.padding(horizontal = 4.dp)) { SpeciesListHeading(overview.unit) }
                Spacer(Modifier.height(12.dp))
                SpeciesTileGrid(overview.species, overview.unit)
            }

            is StationReading.Pending -> DetailMessage(stringResource(Res.string.all_stations_detail_loading))

            is StationReading.Unavailable -> DetailMessage(stringResource(Res.string.all_stations_detail_unavailable))
        }
    }
}

@Composable
private fun DetailMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun StaleIcon() {
    Icon(
        imageVector = Icons.Default.Warning,
        // Decorative: the row's line says "Reading not current" in words.
        contentDescription = null,
        tint = MaterialTheme.meadowColors.onWarningContainer,
        modifier = Modifier.size(STALE_ICON_SIZE),
    )
}

/** Joins a name or level to what follows it: "Zürich · High", "High · Birch". */
private const val SEPARATOR = " · "

private val SIDE_MARGIN = 20.dp
private val ROW_GAP = 8.dp
private val ROW_DOT_SIZE = 14.dp
private val LEGEND_DOT_SIZE = 9.dp
private val SELECTED_EDGE = 2.dp
private val STALE_SLOT_SIZE = 24.dp
private val STALE_ICON_SIZE = 18.dp
private val CHEVRON_SIZE = 22.dp

/** The items above the first station in the list: the heading. */
private const val LIST_HEADING_ITEMS = 1
private const val LIST_HEADING_KEY = "heading"

/** The map's share of the screen's content height at most, so the list keeps room below it. */
private const val MAP_MAX_HEIGHT_FRACTION = 0.4f

/** Shared by the expand animation and the scroll that waits for it to settle. */
private const val EXPAND_DURATION_MILLIS = 250
