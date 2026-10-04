package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStationRepository
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.species.domain.repository.SpeciesRepository
import ch.stenzel.tim.polleninfo.core.station.domain.repository.StationRepository
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmFormState
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmType
import ch.stenzel.tim.polleninfo.feature.alarms.domain.repository.AlarmRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

/**
 * Creates an alarm. The stations and pollen types are loaded together, and the form opens on the
 * defaults of [AlarmFormState.newDailyReport] with the home station preselected; the type toggle
 * turns it into a threshold alert.
 *
 * Every rule about the fields lives in [AlarmFormState]; this class only applies the user's changes
 * to it and runs the save.
 */
class AlarmEditorViewModel(
    private val stationRepository: StationRepository,
    private val speciesRepository: SpeciesRepository,
    private val selectedStationRepository: SelectedStationRepository,
    private val alarmRepository: AlarmRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlarmEditorUiState>(AlarmEditorUiState.Loading)
    val uiState: StateFlow<AlarmEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<AlarmEditorEvent>(Channel.BUFFERED)
    val events: Flow<AlarmEditorEvent> = _events.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    fun onStationSelected(abbr: String) = editForm { withStation(abbr) }

    fun onSpeciesToggled(id: String) = editForm { toggleSpecies(id) }

    fun onMinSeveritySelected(severity: PollenSeverity) = editForm { withMinSeverity(severity) }

    fun onDayToggled(day: DayOfWeek) = editForm { toggleDay(day) }

    fun onTypeSelected(type: AlarmType) = editForm { withType(type) }

    fun onTimeSelected(time: LocalTime) = editForm { withTime(time) }

    fun onWindowStartSelected(time: LocalTime) = editForm { withWindowStart(time) }

    fun onWindowEndSelected(time: LocalTime) = editForm { withWindowEnd(time) }

    /** Ignored unless the form can be saved, so a double tap cannot create the alarm twice. */
    fun save() {
        val editing = _uiState.value as? AlarmEditorUiState.Editing ?: return
        if (!editing.canSave) return
        _uiState.value = editing.copy(isSaving = true, saveError = null)

        viewModelScope.launch {
            when (val result = alarmRepository.create(editing.form.toDraft())) {
                is Result.Success -> _events.send(AlarmEditorEvent.Done)
                is Result.Failure -> updateEditing {
                    copy(isSaving = false, saveError = result.exception.message ?: DEFAULT_SAVE_ERROR)
                }
            }
        }
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.value = AlarmEditorUiState.Loading

        loadJob = viewModelScope.launch {
            val stations = async { stationRepository.getStations() }
            val species = async { speciesRepository.getSpecies() }
            val home = selectedStationRepository.selectedStation.first()

            val stationList = when (val result = stations.await()) {
                is Result.Success -> result.data
                is Result.Failure -> return@launch fail(result.exception)
            }
            val speciesList = when (val result = species.await()) {
                is Result.Success -> result.data
                is Result.Failure -> return@launch fail(result.exception)
            }
            // Past the startup gate a home station always exists; the fallback covers a stored
            // station the list no longer contains rather than leaving the dropdown empty.
            val station = stationList.firstOrNull { it.abbr == home?.abbr } ?: stationList.firstOrNull()
                ?: return@launch fail(IllegalStateException("No stations available"))

            _uiState.value = AlarmEditorUiState.Editing(
                form = AlarmFormState.newDailyReport(station.abbr, speciesList.map { it.id }),
                stations = stationList,
                species = speciesList,
            )
        }
    }

    private fun fail(exception: Exception) {
        _uiState.value = AlarmEditorUiState.Error(exception.message ?: DEFAULT_LOAD_ERROR)
    }

    /**
     * Ignored while a save runs: the save sends the form as it was when tapped, and a change made
     * meanwhile would be lost silently when the editor closes.
     */
    private fun editForm(change: AlarmFormState.() -> AlarmFormState) = updateEditing {
        if (isSaving) this else copy(form = form.change())
    }

    private fun updateEditing(change: AlarmEditorUiState.Editing.() -> AlarmEditorUiState.Editing) {
        _uiState.update { state -> if (state is AlarmEditorUiState.Editing) state.change() else state }
    }

    private companion object {
        const val DEFAULT_LOAD_ERROR = "An unexpected error occurred"
        const val DEFAULT_SAVE_ERROR = "The alarm could not be saved"
    }
}
