package ch.stenzel.tim.polleninfo.feature.onboarding.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.stationpicker.presentation.StationPickerContent
import ch.stenzel.tim.polleninfo.core.stationpicker.presentation.selected
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.onboarding_continue
import ch.stenzel.tim.polleninfo.resources.onboarding_intro
import ch.stenzel.tim.polleninfo.resources.onboarding_title
import ch.stenzel.tim.polleninfo.resources.station_picker_save_error
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Deliberately no `TopAppBar`: this screen is a one-time setup step, not a place in the app the
 * user navigates around, so a title bar would suggest somewhere to go back to.
 */
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

    Surface(modifier = Modifier.fillMaxSize()) {
        StationPickerContent(
            state = uiState.picker,
            onPermissionResult = viewModel::onPermissionResult,
            onStationSelected = viewModel::onStationSelected,
            onRetry = viewModel::retry,
            verticalArrangement = Arrangement.Center,
            header = {
                Text(
                    text = stringResource(Res.string.onboarding_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.onboarding_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(32.dp))
            },
            footer = {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = viewModel::onConfirm,
                    // Nothing to confirm until a station is picked, so a mis-tap cannot commit anything.
                    enabled = uiState.picker.selected != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.onboarding_continue))
                }
                if (uiState.saveError) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.station_picker_save_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
        )
    }
}
