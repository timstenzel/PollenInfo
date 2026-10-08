package ch.stenzel.tim.polleninfo.feature.settings.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.stationpicker.presentation.StationPickerContent
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.change_station_intro
import ch.stenzel.tim.polleninfo.resources.change_station_save
import ch.stenzel.tim.polleninfo.resources.change_station_title
import ch.stenzel.tim.polleninfo.resources.common_back
import ch.stenzel.tim.polleninfo.resources.station_picker_save_error
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Changes the default station: the shared picker under a "Default station" top bar, and Save.
 * Not a tab, so the bottom bar is hidden. [onDone] closes the screen — after a save, or from the
 * back arrow; the system back gesture leaves it without saving as well.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.change_station_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.common_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        StationPickerContent(
            state = uiState.picker,
            onPermissionResult = viewModel::onPermissionResult,
            onStationSelected = viewModel::onStationSelected,
            onRetry = viewModel::retry,
            modifier = Modifier.padding(padding),
            header = {
                Text(
                    text = stringResource(Res.string.change_station_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
            },
            footer = {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = viewModel::save,
                    // Disabled while the pick is still the stored station: there is nothing to save.
                    enabled = uiState.canSave,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = LocalContentColor.current,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(stringResource(Res.string.change_station_save))
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
