package ch.stenzel.tim.polleninfo.feature.diary.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.core.species.domain.model.Species
import ch.stenzel.tim.polleninfo.core.ui.button.PillButton
import ch.stenzel.tim.polleninfo.core.ui.button.PillButtonStyle
import ch.stenzel.tim.polleninfo.core.ui.error.message
import ch.stenzel.tim.polleninfo.core.ui.format.rememberDateWording
import ch.stenzel.tim.polleninfo.core.ui.layout.MeadowCard
import ch.stenzel.tim.polleninfo.core.ui.layout.ScreenHeader
import ch.stenzel.tim.polleninfo.core.ui.layout.TabScreenFrame
import ch.stenzel.tim.polleninfo.core.ui.selection.DropdownPill
import ch.stenzel.tim.polleninfo.core.ui.selection.SegmentedToggle
import ch.stenzel.tim.polleninfo.core.ui.species.speciesColor
import ch.stenzel.tim.polleninfo.core.ui.species.speciesName
import ch.stenzel.tim.polleninfo.core.ui.status.StatusCard
import ch.stenzel.tim.polleninfo.core.ui.status.StatusTone
import ch.stenzel.tim.polleninfo.feature.diary.chart.DiaryChart
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.account_title_login
import ch.stenzel.tim.polleninfo.resources.account_title_register
import ch.stenzel.tim.polleninfo.resources.common_retry
import ch.stenzel.tim.polleninfo.resources.diary_disclaimer
import ch.stenzel.tim.polleninfo.resources.diary_feeling_legend
import ch.stenzel.tim.polleninfo.resources.diary_gate_intro
import ch.stenzel.tim.polleninfo.resources.diary_gate_point_devices
import ch.stenzel.tim.polleninfo.resources.diary_gate_point_email
import ch.stenzel.tim.polleninfo.resources.diary_gate_point_health
import ch.stenzel.tim.polleninfo.resources.diary_gate_point_phone
import ch.stenzel.tim.polleninfo.resources.diary_gate_title
import ch.stenzel.tim.polleninfo.resources.diary_intro
import ch.stenzel.tim.polleninfo.resources.diary_load_failed
import ch.stenzel.tim.polleninfo.resources.diary_no_entries_hint
import ch.stenzel.tim.polleninfo.resources.diary_not_measured
import ch.stenzel.tim.polleninfo.resources.diary_overline
import ch.stenzel.tim.polleninfo.resources.diary_range_month
import ch.stenzel.tim.polleninfo.resources.diary_range_week
import ch.stenzel.tim.polleninfo.resources.diary_range_year
import ch.stenzel.tim.polleninfo.resources.diary_species_heading
import ch.stenzel.tim.polleninfo.resources.diary_station_label
import ch.stenzel.tim.polleninfo.resources.home_feeling_question
import ch.stenzel.tim.polleninfo.resources.nav_diary
import ch.stenzel.tim.polleninfo.resources.nav_home
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Diary tab: the user's answers and the daily pollen levels at a station — the home station
 * until another is chosen — over the last week, month or year. A period without answers shows a
 * hint over the chart pointing to Home. Below the chart, one tick chip per pollen type, in its
 * line's colour, switches its line on and off and doubles as the legend.
 *
 * Scrolls as a whole and has no pull-to-refresh. The note that the diary is no diagnosis is part of
 * every state with a graph, tied to the explanatory line by its asterisk.
 *
 * The diary needs an account: logged out, the tab is the gate — why an account is needed, and
 * [onLogIn] / [onCreateAccount], which open the account screen in either mode.
 */
@Composable
fun DiaryScreen(
    onLogIn: () -> Unit,
    onCreateAccount: () -> Unit,
    viewModel: DiaryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DiaryContent(
        uiState = uiState,
        actions = DiaryActions(
            onStationSelected = viewModel::onStationSelected,
            onRangeSelected = viewModel::onRangeSelected,
            onSpeciesToggled = viewModel::onSpeciesToggled,
            onLogIn = onLogIn,
            onCreateAccount = onCreateAccount,
        ),
        onRetry = viewModel::retry,
    )
}

private class DiaryActions(
    val onStationSelected: (String) -> Unit,
    val onRangeSelected: (HistoryRange) -> Unit,
    val onSpeciesToggled: (String) -> Unit,
    val onLogIn: () -> Unit,
    val onCreateAccount: () -> Unit,
)

@Composable
private fun DiaryContent(
    uiState: DiaryUiState,
    actions: DiaryActions,
    onRetry: () -> Unit,
) {
    TabScreenFrame { contentPadding ->
        val header = @Composable {
            ScreenHeader(
                overline = stringResource(Res.string.diary_overline),
                title = stringResource(Res.string.nav_diary),
            )
        }
        when (uiState) {
            is DiaryUiState.Loading -> DiaryColumn(contentPadding, header) { DiarySkeleton() }
            is DiaryUiState.SignedOut -> DiaryColumn(contentPadding, header) { DiaryGate(actions) }
            is DiaryUiState.Content -> DiaryColumn(contentPadding, header) { DiaryView(uiState, actions) }
            is DiaryUiState.Error -> DiaryColumn(contentPadding, header) {
                StatusCard(
                    title = stringResource(Res.string.diary_load_failed),
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

/** The Diary's scrolling column, clear of the status bar and the tab bar, with the screen's margins. */
@Composable
private fun DiaryColumn(
    contentPadding: PaddingValues,
    header: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(start = SIDE_MARGIN, end = SIDE_MARGIN, bottom = 24.dp),
    ) {
        header()
        content()
    }
}

private val SIDE_MARGIN = 20.dp

/** The vertical gap between the Diary's sections. */
internal val DIARY_SECTION_GAP = 20.dp

/**
 * From the top: the explanatory line, the station and the range side by side (stacked when they do
 * not fit), the chart card, the pollen-type chips and the disclaimer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.DiaryView(content: DiaryUiState.Content, actions: DiaryActions) {
    Text(
        text = stringResource(Res.string.diary_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(DIARY_SECTION_GAP))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        // Any station; choosing one here never changes the home station.
        DropdownPill(
            label = stringResource(Res.string.diary_station_label),
            value = content.stations.firstOrNull { it.abbr == content.stationAbbr }?.name ?: content.stationAbbr,
            options = content.stations,
            optionText = { it.name },
            onSelected = { actions.onStationSelected(it.abbr) },
        )
        SegmentedToggle(
            options = HistoryRange.entries,
            selected = content.range,
            onSelected = actions.onRangeSelected,
            label = { stringResource(it.labelResource()) },
        )
    }
    // Reserved even when idle, so the chart does not jump when a load starts.
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(4.dp)) {
        if (content.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
    ChartCard(content)
    Spacer(Modifier.height(DIARY_SECTION_GAP))
    Text(
        text = stringResource(Res.string.diary_species_heading),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(DIARY_SECTION_GAP))
    val notMeasured = content.notMeasured
    FlowRow(horizontalArrangement = Arrangement.spacedBy(CHIP_GAP), verticalArrangement = Arrangement.spacedBy(CHIP_GAP)) {
        content.species.forEach { species ->
            SpeciesChip(
                species = species,
                checked = species.id in content.checked,
                measured = species.id !in notMeasured,
                onToggled = { actions.onSpeciesToggled(species.id) },
            )
        }
    }
}

/**
 * The chart in its card, titled with the period it shows and a legend for the dashed feeling line.
 * The card is `surface`, not the raised card colour: every species line reaches 3.5:1 only on it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChartCard(content: DiaryUiState.Content) {
    val cardColor = MaterialTheme.colorScheme.surface
    MeadowCard(Modifier.fillMaxWidth(), color = cardColor) {
        Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 16.dp, top = 18.dp, bottom = 14.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = rememberDateWording().dateRange(content.history.from, content.history.until),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FeelingLegend()
            }
            Spacer(Modifier.height(12.dp))
            Box {
                DiaryChart(
                    days = content.history.days,
                    speciesIds = content.shownSpeciesIds,
                    range = content.historyRange,
                    entries = content.entries,
                    cardColor = cardColor,
                )
                // Outside the chart's single semantics node, so a screen reader reads it on its own.
                if (content.hasNoEntries) {
                    Text(
                        text = stringResource(
                            Res.string.diary_no_entries_hint,
                            stringResource(Res.string.home_feeling_question),
                            stringResource(Res.string.nav_home),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 40.dp, vertical = 16.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

/** A sample of the dashed feeling line and its name. The chart itself is described as a whole. */
@Composable
private fun FeelingLegend() {
    val color = MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(width = 18.dp, height = 3.dp).clearAndSetSemantics { }) {
            drawLine(
                color,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = size.height,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(Res.string.diary_feeling_legend),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The Diary without an account, as a gate card: why the diary needs one — the answers are health
 * information kept in the account, survive a lost or replaced phone and appear on every phone logged
 * in, and the email is only used to log in — each point beside a check badge, and the two ways in.
 * No chart.
 */
@Composable
private fun DiaryGate(actions: DiaryActions) {
    StatusCard(
        title = stringResource(Res.string.diary_gate_title),
        body = stringResource(Res.string.diary_gate_intro),
        modifier = Modifier.fillMaxWidth(),
        tone = StatusTone.Neutral,
        icon = Icons.AutoMirrored.Filled.MenuBook,
        centered = false,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(
                Res.string.diary_gate_point_health,
                Res.string.diary_gate_point_phone,
                Res.string.diary_gate_point_devices,
                Res.string.diary_gate_point_email,
            ).forEach { point -> GatePoint(stringResource(point)) }
        }
        Spacer(Modifier.height(22.dp))
        PillButton(onClick = actions.onCreateAccount, style = PillButtonStyle.Primary, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.account_title_register))
        }
        Spacer(Modifier.height(8.dp))
        PillButton(onClick = actions.onLogIn, style = PillButtonStyle.Outline, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.account_title_login))
        }
    }
}

/** One point of the gate. The check badge is decoration: each point is its own sentence. */
@Composable
private fun GatePoint(text: String) {
    Row(Modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * One pollen type's tick chip, in its line's colour so the chips are also the chart's legend:
 * checked, a filled box with a tick and an edge in that colour; unchecked, an empty box in it. A
 * type the station does not measure in the period is disabled and shown unchecked and dimmed with
 * "Not measured here" — no line is drawn for it, and a missing line must not read as "no pollen".
 * Its checked choice is kept, so it is drawn again at a station that measures it.
 *
 * The whole chip is one toggleable focus stop. It is drawn at least 40 dp tall, so a row of chips
 * with [CHIP_GAP] between them is 48 dp apart; Compose extends the touch area of the smaller chip to
 * 48 dp.
 */
@Composable
private fun SpeciesChip(species: Species, checked: Boolean, measured: Boolean, onToggled: () -> Unit) {
    val shownChecked = checked && measured
    val lineColor = speciesColor(species.id) ?: MaterialTheme.colorScheme.outline
    val raised = MaterialTheme.colorScheme.surfaceContainerLowest
    val textColor = if (measured) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .heightIn(min = CHIP_HEIGHT)
            .clip(CircleShape)
            .background(if (shownChecked) raised else Color.Transparent)
            .border(
                if (shownChecked) BorderStroke(1.5.dp, lineColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                CircleShape,
            )
            .toggleable(value = shownChecked, enabled = measured, role = Role.Checkbox, onValueChange = { onToggled() })
            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
    ) {
        val box = RoundedCornerShape(6.dp)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(18.dp)
                .alpha(if (measured) 1f else DISABLED_ALPHA)
                .then(if (shownChecked) Modifier.background(lineColor, box) else Modifier.border(2.dp, lineColor, box)),
        ) {
            if (shownChecked) {
                Icon(Icons.Default.Check, contentDescription = null, tint = raised, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(text = speciesName(species.id, species.name), style = MaterialTheme.typography.labelLarge, color = textColor)
            if (!measured) {
                Text(
                    text = stringResource(Res.string.diary_not_measured),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun HistoryRange.labelResource(): StringResource = when (this) {
    HistoryRange.WEEK -> Res.string.diary_range_week
    HistoryRange.MONTH -> Res.string.diary_range_month
    HistoryRange.YEAR -> Res.string.diary_range_year
}

private val CHIP_HEIGHT = 40.dp
private val CHIP_GAP = 8.dp

/** Material's opacity for disabled content. */
private const val DISABLED_ALPHA = 0.38f
