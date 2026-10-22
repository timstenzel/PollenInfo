package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.ui.format.DateWording
import ch.stenzel.tim.polleninfo.core.ui.layout.MeadowCard
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmSchedule
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.alarm_edit_action
import ch.stenzel.tim.polleninfo.resources.alarm_paused_label
import ch.stenzel.tim.polleninfo.resources.alarm_switch_description
import ch.stenzel.tim.polleninfo.theme.meadowColors
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.stringResource

/**
 * What an [AlarmCard] says, already worded: [type] from `typeSummary`, [days] from `daysSummary`
 * (spoken for the weekday circles), [species] from `speciesSummary` and [minimum], the minimum's tag.
 */
internal class AlarmCardText(
    val type: String,
    val days: String,
    val species: String,
    val minimum: String,
)

/**
 * One alarm as a card (Figma "AlarmCard"): its type's icon tile, the type and time, the station,
 * the weekday circles, the pollen types and the minimum as tags, and the switch. A paused alarm is
 * dimmed, marked "Paused" and its circles lose their green. Only the card's fill and type tile are
 * dimmed, not its text: Figma's dimming of the whole card would take the text below 4.5:1.
 *
 * Tapping the card opens it in the editor and is one focus stop reading the card's words in order;
 * the switch is a focus stop of its own, named after the station so a screen reader says what it
 * switches.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AlarmCard(
    item: AlarmListItem,
    text: AlarmCardText,
    dates: DateWording,
    onEdit: () -> Unit,
    onEnabledToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = item.alarm.enabled
    val colors = MaterialTheme.colorScheme
    val switchDescription = stringResource(Res.string.alarm_switch_description, item.stationName)
    val cardColor = if (enabled) {
        colors.surfaceContainerLowest
    } else {
        colors.surfaceContainerLowest.copy(alpha = PAUSED_ALPHA).compositeOver(colors.background)
    }
    MeadowCard(modifier, shape = MaterialTheme.shapes.large, color = cardColor) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = stringResource(Res.string.alarm_edit_action), onClick = onEdit)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeTile(item.alarm.schedule, Modifier.alpha(if (enabled) 1f else PAUSED_ALPHA))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (!enabled) {
                        Text(
                            text = stringResource(Res.string.alarm_paused_label).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = text.type,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (enabled) colors.onSurface else colors.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Place,
                            // Decorative: the station's name beside it says what it is.
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = item.stationName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledToggled,
                    modifier = Modifier.semantics { contentDescription = switchDescription },
                )
            }
            DayCircles(item.alarm.days, enabled, dates, text.days)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Tag(text.species, colors.onSurface)
                Tag(text.minimum, colors.onSurfaceVariant)
            }
        }
    }
}

/** The type's icon on its tile: a sun on sage for a daily report, a rising line on warm for an alert. */
@Composable
private fun TypeTile(schedule: AlarmSchedule, modifier: Modifier) {
    val (icon, tile, ink) = when (schedule) {
        is AlarmSchedule.Daily -> Triple(
            Icons.Default.WbSunny,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        is AlarmSchedule.Threshold -> Triple(
            Icons.AutoMirrored.Filled.TrendingUp,
            MaterialTheme.meadowColors.warningContainer,
            MaterialTheme.meadowColors.onWarningContainer,
        )
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(46.dp).background(tile, MaterialTheme.shapes.small),
    ) {
        // Decorative: the type's words beside it say the same.
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(24.dp))
    }
}

/**
 * The week, Monday first, one circle per day; the chosen days are filled — green while the alarm
 * runs, grey while it is paused. Spoken as [description] ("Mon–Fri"), not day by day.
 */
@Composable
private fun DayCircles(days: Set<DayOfWeek>, enabled: Boolean, dates: DateWording, description: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        DayOfWeek.entries.forEach { day ->
            val chosen = day in days
            val (fill, ink) = when {
                !chosen -> Color.Transparent to colors.onSurfaceVariant
                enabled -> colors.primary to colors.onPrimary
                else -> colors.outlineVariant to colors.onSurface
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .widthIn(max = DAY_SIZE)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(fill)
                        .then(if (chosen) Modifier else Modifier.border(1.dp, colors.outlineVariant, CircleShape)),
                ) {
                    DayLabel(text = dates.weekdayShort(day), style = MaterialTheme.typography.labelMedium, color = ink)
                }
            }
        }
    }
}

/** A small pill on the sunken colour. */
@Composable
private fun Tag(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** How opaque a paused card's fill is over the screen; the contrast test holds its text at 4.5:1. */
internal const val PAUSED_ALPHA = 0.82f

private val DAY_SIZE = 34.dp
