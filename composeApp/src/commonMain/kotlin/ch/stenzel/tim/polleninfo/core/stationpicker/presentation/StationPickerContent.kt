package ch.stenzel.tim.polleninfo.core.stationpicker.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.location.rememberCoarseLocationPermissionRequester
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.ui.error.message
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.common_retry
import ch.stenzel.tim.polleninfo.resources.station_picker_label
import ch.stenzel.tim.polleninfo.resources.station_picker_load_failed
import ch.stenzel.tim.polleninfo.resources.station_picker_location_denied
import ch.stenzel.tim.polleninfo.resources.station_picker_location_unavailable
import ch.stenzel.tim.polleninfo.resources.station_picker_use_my_location
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The whole picker as a screen body: a spinner while the list loads, the error with Retry when it
 * cannot, and otherwise [header], the location shortcut, the dropdown and [footer] — where each
 * screen puts its own confirm button.
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
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    header: @Composable ColumnScope.() -> Unit = {},
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val requestLocationPermission = rememberCoarseLocationPermissionRequester(onResult = onPermissionResult)

    when (state) {
        is StationPickerState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is StationPickerState.Error -> StationListError(state.error, onRetry, modifier)

        is StationPickerState.Content -> Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = verticalArrangement,
        ) {
            header()
            // The shortcut comes first: it is the fastest path, so it is the most prominent one.
            UseMyLocationButton(isLocating = state.isLocating, onClick = requestLocationPermission)
            if (state.locationError != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(state.locationError.text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(24.dp))
            // Neither the dropdown nor the screen's confirm button takes `isLocating` into account,
            // on purpose: a slow lookup must never block the manual path. Picking calls it off.
            StationDropdown(
                stations = state.stations,
                selected = state.selected,
                onStationSelected = onStationSelected,
            )
            footer()
        }
    }
}

@Composable
private fun StationListError(error: AppError, onRetry: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.station_picker_load_failed),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = error.message(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text(stringResource(Res.string.common_retry)) }
    }
}

@Composable
private fun UseMyLocationButton(isLocating: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        // Disabled only while a lookup is in flight, so impatient taps cannot stack up requests. A
        // refused permission deliberately leaves it enabled — the device may still allow a prompt.
        enabled = !isLocating,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isLocating) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
            Spacer(Modifier.width(8.dp))
        }
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
        OutlinedTextField(
            value = selected?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.station_picker_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
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
                )
            }
        }
    }
}
