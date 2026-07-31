package ch.stenzel.tim.polleninfo.feature.onboarding.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.location.rememberCoarseLocationPermissionRequester
import ch.stenzel.tim.polleninfo.feature.onboarding.domain.model.Station
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // One-shot events, not state: collecting a Channel here means the navigation below runs exactly
    // once per completion, rather than on every recomposition that reads a flag.
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is OnboardingEvent.Completed -> onOnboardingComplete()
            }
        }
    }

    // The permission prompt has to be requested from the composition: Android's launcher is scoped
    // to the activity, so no injected class can own it. The ViewModel only ever sees the boolean.
    val requestLocationPermission =
        rememberCoarseLocationPermissionRequester(onResult = viewModel::onPermissionResult)

    OnboardingContent(
        uiState = uiState,
        onUseMyLocation = requestLocationPermission,
        onStationSelected = viewModel::onStationSelected,
        onConfirm = viewModel::onConfirm,
        onRetry = viewModel::retry,
    )
}

/**
 * Deliberately no `TopAppBar`: this screen is a one-time setup step, not a place in the app the
 * user navigates around, so a title bar would suggest somewhere to go back to.
 */
@Composable
private fun OnboardingContent(
    uiState: OnboardingUiState,
    onUseMyLocation: () -> Unit,
    onStationSelected: (Station) -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is OnboardingUiState.Loading -> LoadingView()

            is OnboardingUiState.Error -> ErrorView(message = uiState.message, onRetry = onRetry)

            is OnboardingUiState.Content -> StationPickerView(
                stations = uiState.stations,
                selected = uiState.selected,
                isLocating = uiState.isLocating,
                locationError = uiState.locationError,
                saveError = uiState.saveError,
                onUseMyLocation = onUseMyLocation,
                onStationSelected = onStationSelected,
                onConfirm = onConfirm,
            )
        }
    }
}

@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "The station list could not be loaded.",
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
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun StationPickerView(
    stations: List<Station>,
    selected: Station?,
    isLocating: Boolean,
    locationError: LocationError?,
    saveError: Boolean,
    onUseMyLocation: () -> Unit,
    onStationSelected: (Station) -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Welcome to PollenInfo", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Choose the measuring station you want pollen levels for.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        // The shortcut comes first: it is the fastest path, so it is the most prominent one.
        UseMyLocationButton(isLocating = isLocating, onClick = onUseMyLocation)
        if (locationError != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = locationError.message(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(24.dp))
        StationDropdown(
            stations = stations,
            selected = selected,
            onStationSelected = onStationSelected,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onConfirm,
            // Nothing to confirm until a station is picked, so a mis-tap cannot commit anything.
            enabled = selected != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue")
        }
        if (saveError) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Could not save your selection. Please try again.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
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
        Text("Use my location")
    }
}

/**
 * The single home of the two location failure messages. Wording that exists once cannot drift
 * between copies, which is why this is a function rather than a literal at each use site.
 */
private fun LocationError.message(): String = when (this) {
    LocationError.PERMISSION_DENIED ->
        "Location permission is off. Enable it in Settings to find your nearest station."

    LocationError.UNAVAILABLE ->
        "Your location could not be determined. Please pick a station manually."
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
            label = { Text("Station") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
