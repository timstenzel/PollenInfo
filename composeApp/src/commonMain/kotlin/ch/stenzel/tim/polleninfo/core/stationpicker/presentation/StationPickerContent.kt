package ch.stenzel.tim.polleninfo.core.stationpicker.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.location.rememberCoarseLocationPermissionRequester
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.ui.button.PillButton
import ch.stenzel.tim.polleninfo.core.ui.button.PillButtonStyle
import ch.stenzel.tim.polleninfo.core.ui.error.message
import ch.stenzel.tim.polleninfo.core.ui.field.SelectField
import ch.stenzel.tim.polleninfo.core.ui.layout.MeadowCard
import ch.stenzel.tim.polleninfo.core.ui.status.Banner
import ch.stenzel.tim.polleninfo.core.ui.status.ShimmerBox
import ch.stenzel.tim.polleninfo.core.ui.status.SkeletonCanvas
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.common_retry
import ch.stenzel.tim.polleninfo.resources.station_picker_label
import ch.stenzel.tim.polleninfo.resources.station_picker_load_failed
import ch.stenzel.tim.polleninfo.resources.station_picker_location_denied
import ch.stenzel.tim.polleninfo.resources.station_picker_location_unavailable
import ch.stenzel.tim.polleninfo.resources.station_picker_use_my_location
import ch.stenzel.tim.polleninfo.theme.meadowColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The picker as a block of a screen's scrolling content (Figma "TextField" + "Use my location"): the
 * station dropdown and the tonal location shortcut, a location failure as a [Banner] below them, a
 * shimmer while the list loads and, when it cannot, an inline error card with Retry in their place.
 * The screen's confirm button lives in its own action bar.
 *
 * [inCard] puts the dropdown and the shortcut on a card of their own (Change station); onboarding
 * shows them straight on the background, under its illustration.
 *
 * The permission prompt is requested here, from the composition: Android's launcher is scoped to
 * the activity, so no injected class can own it. The state holder only ever sees the boolean
 * handed to [onPermissionResult].
 */
@Composable
fun StationPickerContent(
    state: StationPickerState,
    onPermissionResult: (granted: Boolean) -> Unit,
    onStationSelected: (Station) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    inCard: Boolean = false,
) {
    val requestLocationPermission = rememberCoarseLocationPermissionRequester(onResult = onPermissionResult)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (inCard) 20.dp else 16.dp)) {
        when (state) {
            is StationPickerState.Loading -> Controls(inCard) {
                ShimmerBox(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(controlSpacing(inCard))) {
                        SkeletonControl(height = 64.dp, shape = MaterialTheme.shapes.medium)
                        SkeletonControl(height = 52.dp, shape = CircleShape)
                    }
                }
            }

            is StationPickerState.Error -> StationListError(state.error, onRetry)

            is StationPickerState.Content -> {
                Controls(inCard) {
                    // Neither the dropdown nor the screen's confirm button takes `isLocating` into
                    // account, on purpose: a slow lookup must never block the manual path. Picking
                    // calls it off.
                    StationDropdown(
                        stations = state.stations,
                        selected = state.selected,
                        onStationSelected = onStationSelected,
                    )
                    UseMyLocationButton(isLocating = state.isLocating, onClick = requestLocationPermission)
                }
                if (state.locationError != null) {
                    Banner(stringResource(state.locationError.text), Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** The dropdown and the shortcut, on their card when [inCard]. */
@Composable
private fun Controls(inCard: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val column = @Composable { modifier: Modifier ->
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(controlSpacing(inCard)), content = content)
    }
    if (inCard) {
        MeadowCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
            column(Modifier.padding(16.dp))
        }
    } else {
        column(Modifier)
    }
}

private fun controlSpacing(inCard: Boolean) = if (inCard) 14.dp else 16.dp

@Composable
private fun SkeletonControl(height: Dp, shape: Shape) {
    val sunken = MaterialTheme.colorScheme.surfaceContainerHighest
    SkeletonCanvas(Modifier.fillMaxWidth().height(height)) {
        drawOutline(shape.createOutline(size, layoutDirection, this), sunken)
    }
}

/**
 * The list could not be loaded (Figma "PickerError"): a warning tile, the existing wording and the
 * reason, Retry below — in place of the dropdown, so the screen around it stays as it is.
 */
@Composable
private fun StationListError(error: AppError, onRetry: () -> Unit) {
    val meadow = MaterialTheme.meadowColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(40.dp).background(meadow.warningContainer, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                // Decorative: the text beside it says what went wrong.
                Icon(Icons.Default.CloudOff, contentDescription = null, tint = meadow.onWarningContainer, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(Res.string.station_picker_load_failed),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = error.message(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        PillButton(onClick = onRetry, style = PillButtonStyle.Tonal, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.common_retry))
        }
    }
}

@Composable
private fun UseMyLocationButton(isLocating: Boolean, onClick: () -> Unit) {
    PillButton(
        onClick = onClick,
        style = PillButtonStyle.Tonal,
        // Disabled only while a lookup is in flight, so impatient taps cannot stack up requests. A
        // refused permission deliberately leaves it enabled — the device may still allow a prompt.
        enabled = !isLocating,
        icon = Icons.Default.MyLocation,
        busy = isLocating,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(Res.string.station_picker_use_my_location))
    }
}

/**
 * The single home of the two location failure messages. Wording that exists once cannot drift
 * between copies, which is why this is a mapping rather than a resource at each use site.
 */
private val LocationError.text: StringResource
    get() = when (this) {
        LocationError.PERMISSION_DENIED -> Res.string.station_picker_location_denied
        LocationError.UNAVAILABLE -> Res.string.station_picker_location_unavailable
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationDropdown(
    stations: List<Station>,
    selected: Station?,
    onStationSelected: (Station) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        SelectField(
            label = stringResource(Res.string.station_picker_label),
            value = selected?.name.orEmpty(),
            trailingIcon = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            stations.forEach { station ->
                // Name only — canton, coordinates and altitude would only slow the list down.
                DropdownMenuItem(
                    text = { Text(station.name) },
                    onClick = {
                        onStationSelected(station)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
