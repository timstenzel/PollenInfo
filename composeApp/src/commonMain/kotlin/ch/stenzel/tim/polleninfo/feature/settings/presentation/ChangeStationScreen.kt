package ch.stenzel.tim.polleninfo.feature.settings.presentation

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
import ch.stenzel.tim.polleninfo.core.ui.button.PillButton
import ch.stenzel.tim.polleninfo.core.ui.button.PillButtonStyle
import ch.stenzel.tim.polleninfo.core.ui.layout.ScreenFrame
import ch.stenzel.tim.polleninfo.core.ui.layout.ScreenHeader
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.change_station_intro
import ch.stenzel.tim.polleninfo.resources.change_station_save
import ch.stenzel.tim.polleninfo.resources.change_station_title
import ch.stenzel.tim.polleninfo.resources.nav_settings
import ch.stenzel.tim.polleninfo.resources.station_picker_save_error
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Changes the default station: the shared picker on a card under a "Default station" header, Save
 * in the glass action bar. Not a tab, so the bottom bar is hidden. [onDone] closes the screen — after
 * a save, or from the back button; the system back gesture leaves it without saving as well.
 */
@Composable
fun ChangeStationScreen(
    onDone: () -> Unit,
    viewModel: ChangeStationViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ChangeStationEvent.Done -> onDone()
            }
        }
    }

    ScreenFrame(
        onBack = onDone,
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
                onClick = viewModel::save,
                style = PillButtonStyle.Primary,
                // Disabled while the pick is still the stored station: there is nothing to save.
                enabled = uiState.canSave,
                busy = uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.change_station_save))
            }
        },
    ) { contentPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            // The overline is the place this screen belongs to: it is opened from Settings.
            ScreenHeader(
                overline = stringResource(Res.string.nav_settings),
                title = stringResource(Res.string.change_station_title),
            )
            Text(
                text = stringResource(Res.string.change_station_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            StationPickerContent(
                state = uiState.picker,
                onPermissionResult = viewModel::onPermissionResult,
                onStationSelected = viewModel::onStationSelected,
                onRetry = viewModel::retry,
                inCard = true,
            )
        }
    }
}
