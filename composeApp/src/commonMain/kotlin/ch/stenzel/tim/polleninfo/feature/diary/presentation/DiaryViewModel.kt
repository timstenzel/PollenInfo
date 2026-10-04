package ch.stenzel.tim.polleninfo.feature.diary.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.core.history.domain.repository.StationHistoryRepository
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStationRepository
import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The Diary tab: the daily pollen levels at a station over a period ending yesterday.
 *
 * Opens on the stored home station over [HistoryRange.MONTH]. The home station is **read**, once,
 * and never written: whatever the diary shows later must not change what Home shows.
 */
class DiaryViewModel(
    private val selectedStationRepository: SelectedStationRepository,
    private val stationHistoryRepository: StationHistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiaryUiState>(DiaryUiState.Loading)
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    /** The station on screen; `null` until the stored home station has been read. */
    private var station: SelectedStation? = null

    private val range = HistoryRange.MONTH

    /** Every load starts by cancelling this, so an older answer can never replace a newer one. */
    private var loadJob: Job? = null

    init {
        load()
    }

    /** Leaves [DiaryUiState.Error] by loading again from scratch. */
    fun retry() = load()

    private fun load() {
        loadJob?.cancel()
        _uiState.value = DiaryUiState.Loading
        loadJob = viewModelScope.launch {
            val station = station ?: selectedStationRepository.selectedStation.first()?.also { station = it }
            if (station == null) {
                // Unreachable past the startup gate, as on Home; an error rather than a spinner.
                _uiState.value = DiaryUiState.Error(NO_STATION_MESSAGE)
                return@launch
            }
            _uiState.value = when (val result = stationHistoryRepository.history(station.abbr, range)) {
                is Result.Success -> DiaryUiState.Content(
                    stationAbbr = station.abbr,
                    stationName = station.name,
                    range = range,
                    history = result.data,
                    speciesIds = result.data.days.firstOrNull()?.levels?.keys?.toList().orEmpty(),
                )

                is Result.Failure -> DiaryUiState.Error(result.exception.message ?: DEFAULT_ERROR_MESSAGE)
            }
        }
    }

    companion object {
        private const val DEFAULT_ERROR_MESSAGE = "An unexpected error occurred"
        private const val NO_STATION_MESSAGE = "No measuring station is selected."
    }
}
