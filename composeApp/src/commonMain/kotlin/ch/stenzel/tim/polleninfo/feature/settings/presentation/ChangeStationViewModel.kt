package ch.stenzel.tim.polleninfo.feature.settings.presentation

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
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Changing the default station from Settings: the shared [StationPicker], started on the stored
 * station, and Save, which stores a different one.
 *
 * The stored station is read **once**, when the screen opens — it is what the user is changing
 * from, and what Save compares against. Saving only rewrites `SelectedStationRepository`: Home
 * follows it, while existing alarms and the Diary's current choice keep their own stations.
 */
class ChangeStationViewModel(
    private val selectedStationRepository: SelectedStationRepository,
    stationRepository: StationRepository,
    coarseLocationProvider: CoarseLocationProvider,
    findNearestStation: FindNearestStationUseCase,
) : ViewModel() {

    private val stored = viewModelScope.async { selectedStationRepository.selectedStation.first() }

    private val picker = StationPicker(
        stationRepository,
        coarseLocationProvider,
        findNearestStation,
        viewModelScope,
        initialAbbr = { stored.await()?.abbr },
    )

    /** Everything but the picker: the stored abbreviation once read, the save and its failure. */
    private val local = MutableStateFlow(ChangeStationUiState())

    val uiState: StateFlow<ChangeStationUiState> =
        combine(picker.state, local) { picker, local -> local.copy(picker = picker) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, ChangeStationUiState(picker.state.value))

    private val _events = Channel<ChangeStationEvent>(Channel.BUFFERED)
    val events: Flow<ChangeStationEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val abbr = stored.await()?.abbr
            local.update { it.copy(storedAbbr = abbr) }
        }
    }

    fun retry() = picker.retry()

    /** Ignored while a save runs: the save stores the station as it was when Save was tapped. */
    fun onStationSelected(station: Station) {
        if (local.value.isSaving) return
        local.update { it.copy(saveError = false) }
        picker.onStationSelected(station)
    }

    fun onPermissionResult(granted: Boolean) {
        if (local.value.isSaving) return
        picker.onPermissionResult(granted)
    }

    /**
     * Stores the pick and, only on success, sends [ChangeStationEvent.Done]. A failed write keeps
     * the user here with [ChangeStationUiState.saveError] set, so they know nothing changed. After
     * a success `isSaving` stays set, so the screen cannot save again on its way out.
     */
    fun save() {
        // Read from the sources rather than from uiState, which follows them a dispatch later.
        val state = local.value.copy(picker = picker.state.value)
        val station = state.picker.selected
        if (!state.canSave || station == null) return

        local.update { it.copy(isSaving = true, saveError = false) }
        viewModelScope.launch {
            val result = selectedStationRepository.select(
                SelectedStation(abbr = station.abbr, name = station.name),
            )
            when (result) {
                is Result.Success -> _events.send(ChangeStationEvent.Done)
                is Result.Failure -> local.update { it.copy(isSaving = false, saveError = true) }
            }
        }
    }
}
