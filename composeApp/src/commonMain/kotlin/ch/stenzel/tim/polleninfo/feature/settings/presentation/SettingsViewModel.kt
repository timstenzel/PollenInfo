package ch.stenzel.tim.polleninfo.feature.settings.presentation

import androidx.lifecycle.ViewModel
import ch.stenzel.tim.polleninfo.core.appinfo.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The Settings tab. Holds no text: the Impressum and the data source are fixed wording that the
 * screen takes from string resources, so only what the platform reports passes through here.
 */
class SettingsViewModel(
    appInfo: AppInfo,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(version = appInfo.version))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
}
