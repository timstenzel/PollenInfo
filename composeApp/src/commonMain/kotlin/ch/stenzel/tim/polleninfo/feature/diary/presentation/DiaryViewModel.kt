package ch.stenzel.tim.polleninfo.feature.diary.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.model.swissToday
import ch.stenzel.tim.polleninfo.core.diary.domain.repository.DiaryRepository
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.core.history.domain.repository.StationHistoryRepository
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStationRepository
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.species.domain.model.Species
import ch.stenzel.tim.polleninfo.core.species.domain.repository.SpeciesRepository
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.station.domain.repository.StationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * The Diary tab: the user's answers and the daily pollen levels at a station over a period ending
 * yesterday.
 *
 * Opens on the stored home station (the first listed station if it is no longer listed) over
 * [HistoryRange.MONTH], with every pollen type checked. The home station is **read**, once, and
 * never written: choosing another station here must not change what Home shows.
 *
 * The chosen station, range and checked types are fields of this ViewModel, not only of `Content`:
 * a failed load is [DiaryUiState.Error], and its Retry resumes all three. Nothing is persisted — the
 * choices live as long as the ViewModel, which survives tab switches but not an app restart.
 *
 * Choosing another station or range keeps the current graph on screen, with `isLoading`, until the
 * new history arrives. Toggling a pollen type only changes `checked`; it never reloads.
 *
 * The station list and the pollen types are fetched on the first load and kept; a Retry fetches
 * only what has not arrived yet.
 *
 * The answers are observed: one recorded while the Diary is open joins the graph at once if its day
 * is on it, with no history reload. Today's answer never is — "today" is [swissToday] of [clock].
 */
class DiaryViewModel(
    private val selectedStationRepository: SelectedStationRepository,
    private val stationRepository: StationRepository,
    private val speciesRepository: SpeciesRepository,
    private val stationHistoryRepository: StationHistoryRepository,
    private val diaryRepository: DiaryRepository,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiaryUiState>(DiaryUiState.Loading)
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    /** The chosen station; `null` until the stored home station has been read. */
    private var stationAbbr: String? = null

    /** The selected period. */
    private var range = HistoryRange.MONTH

    /** The checked pollen type ids; `null` until the types have arrived, then all of them. */
    private var checked: Set<String>? = null

    private var stations: List<Station>? = null
    private var species: List<Species>? = null

    /** Every load starts by cancelling this, so an older answer can never replace a newer one. */
    private var loadJob: Job? = null

    /** Every answer the user has given, as last read; filtered per history in [withEntries]. */
    private var allEntries: List<DiaryEntry> = emptyList()

    init {
        viewModelScope.launch {
            diaryRepository.entries.collect { entries ->
                allEntries = entries
                val current = _uiState.value as? DiaryUiState.Content ?: return@collect
                _uiState.value = withEntries(current)
            }
        }
        load()
    }

    /** Leaves [DiaryUiState.Error] by loading the chosen station and range again. */
    fun retry() = load()

    /** Loads [range]'s history; the selected range again does nothing. */
    fun onRangeSelected(range: HistoryRange) {
        if (range == this.range) return
        this.range = range
        load()
    }

    /** Loads the history of the station [abbr]; the station already chosen does nothing. */
    fun onStationSelected(abbr: String) {
        if (abbr == stationAbbr) return
        stationAbbr = abbr
        load()
    }

    /** Checks or unchecks one pollen type. Only changes what is drawn; nothing is requested. */
    fun onSpeciesToggled(id: String) {
        val current = checked ?: return
        val toggled = if (id in current) current - id else current + id
        checked = toggled
        val content = _uiState.value as? DiaryUiState.Content ?: return
        _uiState.value = content.copy(checked = toggled)
    }

    /** [content] with the answers on the days its history covers, today excluded. */
    private fun withEntries(content: DiaryUiState.Content): DiaryUiState.Content {
        val today = swissToday(clock)
        val window = content.history.from..content.history.until
        return content.copy(entries = allEntries.filter { it.date in window && it.date < today })
    }

    private fun load() {
        loadJob?.cancel()
        val range = range
        val previous = _uiState.value as? DiaryUiState.Content
        _uiState.value = previous?.copy(stationAbbr = stationAbbr ?: previous.stationAbbr, range = range, isLoading = true)
            ?: DiaryUiState.Loading
        loadJob = viewModelScope.launch {
            val home = if (stationAbbr == null) {
                // Unreachable past the startup gate, as on Home; an error rather than a spinner.
                selectedStationRepository.selectedStation.first()
                    ?: return@launch fail(NO_STATION_MESSAGE)
            } else {
                null
            }

            val stationsRequest = if (stations == null) async { stationRepository.getStations() } else null
            val speciesRequest = if (species == null) async { speciesRepository.getSpecies() } else null
            stationsRequest?.await()?.let { result ->
                when (result) {
                    is Result.Success -> stations = result.data
                    is Result.Failure -> return@launch fail(result.exception)
                }
            }
            speciesRequest?.await()?.let { result ->
                when (result) {
                    is Result.Success -> species = result.data
                    is Result.Failure -> return@launch fail(result.exception)
                }
            }
            val stations = stations.orEmpty()
            val species = species.orEmpty()
            val checked = checked ?: species.map { it.id }.toSet().also { checked = it }

            val abbr = stationAbbr
                ?: (stations.firstOrNull { it.abbr == home?.abbr } ?: stations.firstOrNull())?.abbr
                    ?.also { stationAbbr = it }
                ?: return@launch fail(NO_STATIONS_MESSAGE)

            _uiState.value = when (val result = stationHistoryRepository.history(abbr, range)) {
                is Result.Success -> withEntries(
                    DiaryUiState.Content(
                        stations = stations,
                        stationAbbr = abbr,
                        range = range,
                        historyRange = range,
                        history = result.data,
                        species = species,
                        checked = checked,
                    ),
                )

                is Result.Failure -> DiaryUiState.Error(result.exception.message ?: DEFAULT_ERROR_MESSAGE)
            }
        }
    }

    private fun fail(exception: Exception) = fail(exception.message ?: DEFAULT_ERROR_MESSAGE)

    private fun fail(message: String) {
        _uiState.value = DiaryUiState.Error(message)
    }

    companion object {
        private const val DEFAULT_ERROR_MESSAGE = "An unexpected error occurred"
        private const val NO_STATION_MESSAGE = "No measuring station is selected."
        private const val NO_STATIONS_MESSAGE = "No measuring stations are available."
    }
}
