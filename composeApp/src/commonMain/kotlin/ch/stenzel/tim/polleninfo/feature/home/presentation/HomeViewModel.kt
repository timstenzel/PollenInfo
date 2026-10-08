package ch.stenzel.tim.polleninfo.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.diary.domain.model.swissToday
import ch.stenzel.tim.polleninfo.core.diary.domain.repository.DiaryRepository
import ch.stenzel.tim.polleninfo.core.measurement.domain.usecase.GetStationMeasurementUseCase
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStationRepository
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.toAppError
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

class HomeViewModel(
    selectedStationRepository: SelectedStationRepository,
    private val getStationMeasurement: GetStationMeasurementUseCase,
    private val diaryRepository: DiaryRepository,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading(stationName = ""))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** The latest stored selection — what [refresh] and [retry] reload. */
    private var selected: SelectedStation? = null

    /**
     * Every load goes through this one job and starts by cancelling it, so a response for an
     * earlier request — the old station, or a refresh overtaken by another — can never overwrite a
     * newer one.
     */
    private var loadJob: Job? = null

    /** The diary as last read; `null` until it has been, so the prompt never flashes up unasked. */
    private var diary: DiarySnapshot? = null

    /** The last answer was not stored; cleared by the next attempt. */
    private var feelingSaveError = false

    init {
        viewModelScope.launch {
            combine(diaryRepository.entries, diaryRepository.dismissedOn, ::DiarySnapshot)
                .collect { snapshot ->
                    diary = snapshot
                    // An answer or a dismissal hides the prompt at once, with no reload.
                    refreshPrompt()
                }
        }
        viewModelScope.launch {
            // Observed, not read once. `StartupViewModel` takes only the first value because
            // rebuilding the navigation graph mid-session would yank the user out of the navigation
            // they just triggered; that reason does not transfer here, where a station change is
            // exactly what should reload the readings. Nothing can change the selection today, so
            // the two behave identically — this one stays correct when the settings screen lands.
            selectedStationRepository.selectedStation
                .distinctUntilChanged()
                .collect { station ->
                    selected = station
                    // A different station's readings must not stay on screen under the new name,
                    // so a change of station is a fresh load, never a refresh.
                    load(keepReadings = false)
                }
        }
    }

    /**
     * Pull-to-refresh. The readings already on screen stay there, flagged as refreshing, until the
     * new ones replace them.
     *
     * Within the backend's cache period this legitimately returns the same reading, and its age
     * does not move: the backend does not re-contact MeteoSwiss on demand, by design.
     */
    fun refresh() = load(keepReadings = true)

    /** Leaves the error state by loading again from scratch. */
    fun retry() = load(keepReadings = false)

    /** Stores today's answer; the prompt hides when the diary reports it stored. */
    fun onFeelingSelected(feeling: Feeling) {
        viewModelScope.launch {
            feelingSaveError = false
            val result = diaryRepository.record(swissToday(clock), feeling)
            feelingSaveError = result is Result.Failure
            refreshPrompt()
        }
    }

    /** Closes the prompt for today without recording an answer. */
    fun onFeelingPromptDismissed() {
        viewModelScope.launch {
            feelingSaveError = false
            // A failed dismissal simply leaves the prompt up to be closed again.
            diaryRepository.dismiss(swissToday(clock))
            refreshPrompt()
        }
    }

    /** Re-derives the prompt on the Content shown, if any; Loading and Error never carry it. */
    private fun refreshPrompt() {
        val current = _uiState.value as? HomeUiState.Content ?: return
        _uiState.value = withPrompt(current)
    }

    /**
     * "Today" is taken from the clock each time Content is built, so the prompt returns on the
     * first load, refresh or diary change after Swiss midnight.
     */
    private fun withPrompt(content: HomeUiState.Content): HomeUiState.Content {
        val today = swissToday(clock)
        val show = diary?.let { snapshot ->
            snapshot.dismissedOn != today && snapshot.entries.none { it.date == today }
        } ?: false
        return content.copy(
            showFeelingPrompt = show,
            feelingSaveError = show && feelingSaveError,
        )
    }

    private fun load(keepReadings: Boolean) {
        loadJob?.cancel()

        // Unreachable by design: the startup gate only routes here once a station is stored. It is
        // still an error rather than an endless spinner, so a future change that
        // made it reachable would be diagnosable.
        val station = selected ?: run {
            _uiState.value = HomeUiState.Error(stationName = "", error = AppError.NoStationSelected)
            return
        }

        val current = _uiState.value
        val refreshing = keepReadings && current is HomeUiState.Content
        _uiState.value = if (refreshing) {
            (current as HomeUiState.Content).copy(isRefreshing = true)
        } else {
            HomeUiState.Loading(station.name)
        }

        loadJob = viewModelScope.launch {
            // A refresh served from the backend's cache answers within milliseconds — before the
            // next frame — and usually with an equal reading, so `isRefreshing` would go true and
            // back to false without composition ever reading `true`. `PullToRefreshBox` only
            // retracts its indicator when it sees `isRefreshing` change, so the spinner would stay
            // stuck. Holding the flag for a minimum time guarantees the change is observed.
            val minimumIndicator = if (refreshing) launch { delay(MIN_REFRESH_INDICATOR) } else null
            val result = getStationMeasurement(station.abbr)
            minimumIndicator?.join()
            _uiState.value = when (result) {
                is Result.Success -> withPrompt(HomeUiState.Content(
                    stationName = station.name,
                    measuredAt = result.data.measuredAt,
                    overallSeverity = result.data.overallSeverity,
                    drivenBy = result.data.drivenBy?.name,
                    unit = result.data.unit,
                    species = result.data.species,
                    refreshedAt = clock.now(),
                ))

                is Result.Failure -> HomeUiState.Error(
                    stationName = station.name,
                    error = result.exception.toAppError(),
                )
            }
        }
    }

    companion object {
        /** The shortest time a refresh shows its indicator; see [load] for why one is needed. */
        val MIN_REFRESH_INDICATOR = 500.milliseconds
    }

    private data class DiarySnapshot(val entries: List<DiaryEntry>, val dismissedOn: LocalDate?)
}
