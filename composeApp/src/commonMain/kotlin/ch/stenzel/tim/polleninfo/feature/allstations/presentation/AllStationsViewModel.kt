package ch.stenzel.tim.polleninfo.feature.allstations.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.station.domain.repository.StationRepository
import ch.stenzel.tim.polleninfo.feature.allstations.domain.model.StationReading
import ch.stenzel.tim.polleninfo.feature.allstations.domain.usecase.GetAllStationReadingsUseCase
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class AllStationsViewModel(
    private val stationRepository: StationRepository,
    private val getAllStationReadings: GetAllStationReadingsUseCase,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AllStationsUiState>(AllStationsUiState.Loading)
    val uiState: StateFlow<AllStationsUiState> = _uiState.asStateFlow()

    /** The station list once it has loaded — what [refresh] reads again without re-fetching it. */
    private var stations: List<Station>? = null

    /**
     * Every load goes through this one job and starts by cancelling it, so an overtaken round can
     * never overwrite a newer one.
     */
    private var loadJob: Job? = null

    init {
        load()
    }

    /**
     * Pull-to-refresh: reads every station again. The rows on screen stay as they are — not reset to
     * pending — until the round completes and its final result replaces them all at once.
     *
     * The station list is not fetched again; it is static metadata.
     */
    fun refresh() {
        val current = _uiState.value as? AllStationsUiState.Content ?: return
        val stations = stations ?: return
        loadJob?.cancel()
        _uiState.value = current.copy(isRefreshing = true)
        loadJob = viewModelScope.launch {
            // As on Home: a round answered from the backend's cache can land before the next frame,
            // and PullToRefreshBox only retracts its indicator once it has seen the flag change.
            val minimumIndicator = launch { delay(MIN_REFRESH_INDICATOR) }
            val readings = getAllStationReadings(stations).last()
            minimumIndicator.join()
            completeRound(readings)
        }
    }

    /** Leaves the error state by loading again from scratch, station list included. */
    fun retry() = load()

    /**
     * The full-screen spinner lasts only until the station list arrives. From then on the screen is
     * [AllStationsUiState.Content] with every station pending, and each row fills in on its own — one
     * slow station must not keep fourteen answered ones off screen.
     */
    private fun load() {
        loadJob?.cancel()
        _uiState.value = AllStationsUiState.Loading
        loadJob = viewModelScope.launch {
            val stations = stations ?: when (val result = stationRepository.getStations()) {
                is Result.Failure -> {
                    _uiState.value = AllStationsUiState.Error(
                        result.exception.message ?: DEFAULT_ERROR_MESSAGE,
                    )
                    return@launch
                }

                is Result.Success -> result.data.also { stations = it }
            }

            // The use case's first emission is every station pending, so Content appears the
            // moment the list is known; later emissions replace the rows as readings resolve.
            var readings = emptyList<StationReading>()
            getAllStationReadings(stations).collect { emitted ->
                readings = emitted
                _uiState.update { state ->
                    (state as? AllStationsUiState.Content)?.copy(stations = emitted)
                        ?: AllStationsUiState.Content(stations = emitted)
                }
            }
            completeRound(readings)
        }
    }

    /**
     * A round in which no station answered is not fifteen "No reading" rows but an unreachable
     * service, and says so. Any single answer keeps the list: per-station failures stay per station.
     */
    private fun completeRound(readings: List<StationReading>) {
        if (readings.all { it is StationReading.Unavailable }) {
            _uiState.value = AllStationsUiState.Error(NO_READINGS_MESSAGE)
            return
        }
        _uiState.update { state ->
            val content = state as? AllStationsUiState.Content ?: AllStationsUiState.Content(readings)
            content.copy(stations = readings, refreshedAt = clock.now(), isRefreshing = false)
        }
    }

    companion object {
        /** The shortest time a refresh shows its indicator; see [refresh] for why one is needed. */
        val MIN_REFRESH_INDICATOR = 500.milliseconds

        private const val DEFAULT_ERROR_MESSAGE = "An unexpected error occurred"
        private const val NO_READINGS_MESSAGE = "No station's reading could be loaded."
    }
}
