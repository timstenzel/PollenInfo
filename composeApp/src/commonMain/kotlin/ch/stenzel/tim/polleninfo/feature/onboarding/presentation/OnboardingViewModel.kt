package ch.stenzel.tim.polleninfo.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStationRepository
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.onboarding.domain.model.Station
import ch.stenzel.tim.polleninfo.feature.onboarding.domain.repository.StationRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val stationRepository: StationRepository,
    private val selectedStationRepository: SelectedStationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Loading)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** One-shot events — see [OnboardingEvent] for why completion is not a flag on the UI state. */
    private val _events = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events: Flow<OnboardingEvent> = _events.receiveAsFlow()

    init {
        loadStations()
    }

    fun loadStations() {
        _uiState.value = OnboardingUiState.Loading

        viewModelScope.launch {
            _uiState.value = when (val result = stationRepository.getStations()) {
                is Result.Success -> OnboardingUiState.Content(result.data)
                is Result.Failure -> OnboardingUiState.Error(result.exception)
            }
        }
    }

    fun retry() = loadStations()

    /** Records the user's pick. Nothing is persisted until they confirm it. */
    fun onStationSelected(station: Station) {
        _uiState.update { state ->
            if (state is OnboardingUiState.Content) {
                state.copy(selected = station, saveError = false)
            } else {
                state
            }
        }
    }

    /**
     * Persists the current selection and, only on success, announces completion. A failed write
     * leaves the user on this screen with [OnboardingUiState.Content.saveError] set and emits
     * nothing, so navigation never runs ahead of the data it depends on.
     */
    fun onConfirm() {
        val state = _uiState.value
        val station = (state as? OnboardingUiState.Content)?.selected ?: return

        viewModelScope.launch {
            val result = selectedStationRepository.select(
                SelectedStation(abbr = station.abbr, name = station.name),
            )
            when (result) {
                is Result.Success -> _events.send(OnboardingEvent.Completed)
                is Result.Failure -> _uiState.update {
                    if (it is OnboardingUiState.Content) it.copy(saveError = true) else it
                }
            }
        }
    }
}
