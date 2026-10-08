package ch.stenzel.tim.polleninfo.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.appinfo.AppInfo
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStationRepository
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.station.domain.repository.StationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The Settings tab. Holds no text: the Impressum and the data source are fixed wording that the
 * screen takes from string resources, so only the default station and what the platform reports
 * pass through here.
 *
 * The station is **observed**, so the row shows a new default as soon as the change-station screen
 * has stored it. Its name comes from the station list, fetched once; until that arrives, or if it
 * fails, the name stored with the selection stands in — never an error for the whole screen.
 */
class SettingsViewModel(
    selectedStationRepository: SelectedStationRepository,
    private val stationRepository: StationRepository,
    appInfo: AppInfo,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(version = appInfo.version))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /** `null` until the list has arrived, and for good if it failed. */
    private val stations = MutableStateFlow<List<Station>?>(null)

    init {
        viewModelScope.launch {
            val result = stationRepository.getStations()
            if (result is Result.Success) stations.value = result.data
        }
        viewModelScope.launch {
            combine(selectedStationRepository.selectedStation, stations) { selected, stations ->
                selected?.let { stored -> stations?.firstOrNull { it.abbr == stored.abbr }?.name ?: stored.name }
            }.collect { name -> _uiState.update { it.copy(stationName = name) } }
        }
    }
}
