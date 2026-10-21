package ch.stenzel.tim.polleninfo.feature.onboarding.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.stationpicker.presentation.StationPickerContent
import ch.stenzel.tim.polleninfo.core.stationpicker.presentation.StationPickerState
import ch.stenzel.tim.polleninfo.core.stationpicker.presentation.selected
import ch.stenzel.tim.polleninfo.core.ui.button.PillButton
import ch.stenzel.tim.polleninfo.core.ui.button.PillButtonStyle
import ch.stenzel.tim.polleninfo.core.ui.layout.ScreenFrame
import ch.stenzel.tim.polleninfo.core.ui.layout.ScreenHeader
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.onboarding_continue
import ch.stenzel.tim.polleninfo.resources.onboarding_intro
import ch.stenzel.tim.polleninfo.resources.onboarding_title
import ch.stenzel.tim.polleninfo.resources.station_picker_save_error
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The first-launch setup: the Swiss map illustration, the welcome header and the shared picker,
 * Continue in the glass action bar. Deliberately no back button: this is a one-time step, not a place
 * in the app the user navigates around, so there is nowhere to go back to.
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

    ScreenFrame(
        onBack = null,
        actionBar = {
            if (uiState.saveError) {
                Text(
                    text = stringResource(Res.string.station_picker_save_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            PillButton(
                onClick = viewModel::onConfirm,
                style = PillButtonStyle.Primary,
                // Nothing to confirm until a station is picked, so a mis-tap cannot commit anything.
                enabled = uiState.picker.selected != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.onboarding_continue))
            }
        },
        washes = { OnboardingWashes(it) },
    ) { contentPadding ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding)) {
            val picker = uiState.picker
            OnboardingIllustration(
                stations = (picker as? StationPickerState.Content)?.stations.orEmpty(),
                selectedAbbr = picker.selected?.abbr,
            )
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                ScreenHeader(overline = APP_NAME, title = stringResource(Res.string.onboarding_title))
                Text(
                    text = stringResource(Res.string.onboarding_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                StationPickerContent(
                    state = picker,
                    onPermissionResult = viewModel::onPermissionResult,
                    onStationSelected = viewModel::onStationSelected,
                    onRetry = viewModel::retry,
                )
            }
        }
    }
}

/** The overline above the welcome: the app's name, which is never translated. */
private const val APP_NAME = "PollenInfo"
