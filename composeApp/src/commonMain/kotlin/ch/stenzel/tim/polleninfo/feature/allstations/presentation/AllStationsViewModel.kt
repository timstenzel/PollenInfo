package ch.stenzel.tim.polleninfo.feature.allstations.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.station.domain.repository.StationRepository
import ch.stenzel.tim.polleninfo.feature.allstations.domain.usecase.GetAllStationReadingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AllStationsViewModel(
    private val stationRepository: StationRepository,
    private val getAllStationReadings: GetAllStationReadingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AllStationsUiState>(AllStationsUiState.Loading)
    val uiState: StateFlow<AllStationsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /**
     * The full-screen spinner lasts only until the station list arrives. From then on the screen is
     * [AllStationsUiState.Content] with every station pending, and each row fills in on its own — one
     * slow station must not keep fourteen answered ones off screen.
     */
    private fun load() {
        _uiState.value = AllStationsUiState.Loading
        viewModelScope.launch {
            when (val result = stationRepository.getStations()) {
                is Result.Failure -> _uiState.value = AllStationsUiState.Error(
                    result.exception.message ?: DEFAULT_ERROR_MESSAGE,
                )

                // The use case's first emission is every station pending, so Content appears the
                // moment the list is known; later emissions replace the rows as readings resolve.
                is Result.Success -> getAllStationReadings(result.data).collect { readings ->
                    _uiState.update { state ->
                        (state as? AllStationsUiState.Content)?.copy(stations = readings)
                            ?: AllStationsUiState.Content(stations = readings)
                    }
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_ERROR_MESSAGE = "An unexpected error occurred"
    }
}
