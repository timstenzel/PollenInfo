package ch.stenzel.tim.polleninfo.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.location.CoarseLocationProvider
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStationRepository
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.station.domain.repository.StationRepository
import ch.stenzel.tim.polleninfo.core.stationpicker.domain.usecase.FindNearestStationUseCase
import ch.stenzel.tim.polleninfo.core.stationpicker.presentation.StationPicker
import ch.stenzel.tim.polleninfo.core.stationpicker.presentation.selected
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * First launch: the shared [StationPicker] starting on nothing, and Continue, which stores the pick
 * and completes onboarding. All the choosing — the list, the location shortcut and its timeout —
 * is the picker's; this ViewModel only saves.
 */
class OnboardingViewModel(
    stationRepository: StationRepository,
    private val selectedStationRepository: SelectedStationRepository,
    coarseLocationProvider: CoarseLocationProvider,
    findNearestStation: FindNearestStationUseCase,
) : ViewModel() {

    private val picker = StationPicker(
        stationRepository,
        coarseLocationProvider,
        findNearestStation,
        viewModelScope,
    )

    private val saveError = MutableStateFlow(false)

    val uiState: StateFlow<OnboardingUiState> =
        combine(picker.state, saveError, ::OnboardingUiState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, OnboardingUiState(picker.state.value))

    /** One-shot events — see [OnboardingEvent] for why completion is not a flag on the UI state. */
    private val _events = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events: Flow<OnboardingEvent> = _events.receiveAsFlow()

    fun retry() = picker.retry()

    /** Records the user's pick — see [StationPicker.onStationSelected]. Also clears a save error. */
    fun onStationSelected(station: Station) {
        saveError.value = false
        picker.onStationSelected(station)
    }

    fun onPermissionResult(granted: Boolean) = picker.onPermissionResult(granted)

    /**
     * Persists the current selection and, only on success, announces completion. A failed write
     * leaves the user on this screen with [OnboardingUiState.saveError] set and emits nothing, so
     * navigation never runs ahead of the data it depends on.
     */
    fun onConfirm() {
        val station = picker.state.value.selected ?: return

        viewModelScope.launch {
            val result = selectedStationRepository.select(
                SelectedStation(abbr = station.abbr, name = station.name),
            )
            when (result) {
                is Result.Success -> _events.send(OnboardingEvent.Completed)
                is Result.Failure -> saveError.value = true
            }
        }
    }
}
