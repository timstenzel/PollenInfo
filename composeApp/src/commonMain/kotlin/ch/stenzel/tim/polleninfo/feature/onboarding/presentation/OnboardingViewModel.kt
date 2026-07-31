package ch.stenzel.tim.polleninfo.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.onboarding.domain.model.Station
import ch.stenzel.tim.polleninfo.feature.onboarding.domain.repository.StationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val stationRepository: StationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Loading)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

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
            if (state is OnboardingUiState.Content) state.copy(selected = station) else state
        }
    }
}
