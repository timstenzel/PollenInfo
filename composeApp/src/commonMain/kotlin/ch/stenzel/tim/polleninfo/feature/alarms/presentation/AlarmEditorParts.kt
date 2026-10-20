package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.ui.button.PillButton
import ch.stenzel.tim.polleninfo.core.ui.button.PillButtonStyle
import ch.stenzel.tim.polleninfo.core.ui.field.SelectField
import ch.stenzel.tim.polleninfo.core.ui.format.DateWording
import ch.stenzel.tim.polleninfo.core.ui.format.formatTime
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmType
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.alarm_time_description
import ch.stenzel.tim.polleninfo.resources.alarm_time_picker_confirm
import ch.stenzel.tim.polleninfo.resources.alarm_type_daily
import ch.stenzel.tim.polleninfo.resources.alarm_type_threshold
import ch.stenzel.tim.polleninfo.resources.common_cancel
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource

/**
 * Daily report or threshold alert as two large cards (Figma "TypeSelector"), one radio group. The
 * chosen card is green with a check. When the type is [locked] (an existing alarm) neither card can
 * be chosen, and the other one is dimmed with a lock: it shows a type the alarm cannot become.
 */
@Composable
internal fun TypeCards(
    selected: AlarmType,
    locked: Boolean,
    enabled: Boolean,
    onTypeSelected: (AlarmType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AlarmType.entries.forEach { type ->
            TypeCard(
                type = type,
                selected = type == selected,
                locked = locked,
                enabled = enabled && !locked,
                onClick = { onTypeSelected(type) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TypeCard(
    type: AlarmType,
    selected: Boolean,
    locked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (icon, label) = when (type) {
        AlarmType.DAILY -> Icons.Default.WbSunny to stringResource(Res.string.alarm_type_daily)
        AlarmType.THRESHOLD -> Icons.AutoMirrored.Filled.TrendingUp to stringResource(Res.string.alarm_type_threshold)
    }
    val ink = if (selected) colors.onPrimary else colors.onSurface
    val (tile, tileInk) = if (selected) {
        selectedTile(colors.onPrimary, colors.primary) to colors.onPrimary
    } else {
        colors.primaryContainer to colors.onPrimaryContainer
    }
    // The locked card is dimmed as a whole: like any disabled control, it need not reach 4.5:1.
    Surface(
        modifier = modifier
            .alpha(if (locked && !selected) LOCKED_ALPHA else 1f)
            .heightIn(min = 104.dp)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (selected) colors.primary else colors.surfaceContainerLowest,
        border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp).background(tile, MaterialTheme.shapes.small),
                ) {
                    // Decorative: the card's words name the type.
                    Icon(icon, contentDescription = null, tint = tileInk, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.weight(1f))
                // Decorative: the selected state and the disabled state are announced by the card.
                when {
                    selected -> Icon(Icons.Default.Check, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
                    locked -> Icon(Icons.Default.Lock, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = ink)
        }
    }
}

/** The selected card's icon tile: its icon colour, faintly, over the card's green. */
internal fun selectedTile(icon: Color, card: Color): Color = icon.copy(alpha = SELECTED_TILE_ALPHA).compositeOver(card)

/**
 * The week, Monday first, as circles that each switch one day on or off: filled green when chosen.
 * Each circle says its full day name ("Mon" would be read as a word) and whether it is on.
 */
@Composable
internal fun DayToggles(
    days: Set<DayOfWeek>,
    enabled: Boolean,
    dates: DateWording,
    onDayToggled: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DayOfWeek.entries.forEach { day ->
            val chosen = day in days
            val dayName = dates.weekdayFull(day)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .minimumInteractiveComponentSize()
                    .sizeIn(minWidth = DAY_SIZE, minHeight = DAY_SIZE)
                    .alpha(if (enabled) 1f else DISABLED_ALPHA)
                    .clip(CircleShape)
                    .background(if (chosen) colors.primary else colors.surfaceContainerLowest)
                    .then(if (chosen) Modifier else Modifier.border(1.dp, colors.outlineVariant, CircleShape))
                    .toggleable(value = chosen, enabled = enabled, role = Role.Checkbox, onValueChange = { onDayToggled(day) })
                    .semantics { contentDescription = dayName },
            ) {
                Text(
                    text = dates.weekdayShort(day),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (chosen) colors.onPrimary else colors.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}

/**
 * A field showing [time] (Figma "TextField" with a clock) that opens the Meadow time picker: [label]
 * as its overline, large hour and minute boxes, the 24-hour dial, Cancel and OK. Read as "Report
 * time, 08:00" rather than a bare time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeField(
    label: String,
    time: LocalTime,
    enabled: Boolean,
    onTimeSelected: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val description = stringResource(Res.string.alarm_time_description, label, formatTime(time))
    SelectField(
        label = label,
        value = formatTime(time),
        trailingIcon = Icons.Default.Schedule,
        isError = isError,
        enabled = enabled,
        onClick = { showPicker = true },
        modifier = modifier.semantics { contentDescription = description },
    )

    if (showPicker) {
        val pickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true,
        )
        val colors = MaterialTheme.colorScheme
        BasicAlertDialog(onDismissRequest = { showPicker = false }) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = colors.surfaceContainerLowest) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(20.dp))
                    TimePicker(
                        state = pickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = colors.surfaceContainerHighest,
                            clockDialSelectedContentColor = colors.onPrimary,
                            clockDialUnselectedContentColor = colors.onSurface,
                            selectorColor = colors.primary,
                            containerColor = colors.surfaceContainerLowest,
                            timeSelectorSelectedContainerColor = colors.primaryContainer,
                            timeSelectorUnselectedContainerColor = colors.surfaceContainerHighest,
                            timeSelectorSelectedContentColor = colors.onPrimaryContainer,
                            timeSelectorUnselectedContentColor = colors.onSurface,
                        ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)) {
                        PillButton(onClick = { showPicker = false }, style = PillButtonStyle.Text) {
                            Text(stringResource(Res.string.common_cancel))
                        }
                        PillButton(
                            onClick = {
                                onTimeSelected(LocalTime(pickerState.hour, pickerState.minute))
                                showPicker = false
                            },
                            style = PillButtonStyle.Text,
                        ) { Text(stringResource(Res.string.alarm_time_picker_confirm)) }
                    }
                }
            }
        }
    }
}

private val DAY_SIZE = 42.dp
private const val LOCKED_ALPHA = 0.5f
private const val SELECTED_TILE_ALPHA = 0.16f
private const val DISABLED_ALPHA = 0.38f
