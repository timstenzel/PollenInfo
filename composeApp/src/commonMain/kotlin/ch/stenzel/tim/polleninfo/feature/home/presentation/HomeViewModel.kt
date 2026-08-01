package ch.stenzel.tim.polleninfo.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStationRepository
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.home.domain.usecase.GetStationMeasurementUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class HomeViewModel(
    selectedStationRepository: SelectedStationRepository,
    private val getStationMeasurement: GetStationMeasurementUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading(stationName = ""))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Observed, not read once. `StartupViewModel` takes only the first value because
            // rebuilding the navigation graph mid-session would yank the user out of the navigation
            // they just triggered; that reason does not transfer here, where a station change is
            // exactly what should reload the readings. Nothing can change the selection today, so
            // the two behave identically — this one stays correct when the settings screen lands.
            //
            // `collectLatest` cancels a load still in flight when the selection changes, so a
            // response for the old station can never overwrite the new one's.
            selectedStationRepository.selectedStation
                .distinctUntilChanged()
                .collectLatest(::load)
        }
    }

    private suspend fun load(selected: SelectedStation?) {
        // Unreachable by design: the startup gate only routes here once a station is stored. It
        // resolves to an error with a message in the refresh-and-recovery slice, which is where the
        // retry that message needs also arrives.
        if (selected == null) return

        _uiState.value = HomeUiState.Loading(selected.name)
        _uiState.value = when (val result = getStationMeasurement(selected.abbr)) {
            is Result.Success -> HomeUiState.Content(
                stationName = selected.name,
                overallSeverity = result.data.overallSeverity,
            )

            is Result.Failure -> HomeUiState.Error(
                stationName = selected.name,
                message = result.exception.message ?: DEFAULT_ERROR_MESSAGE,
            )
        }
    }

    private companion object {
        const val DEFAULT_ERROR_MESSAGE = "An unexpected error occurred"
    }
}
