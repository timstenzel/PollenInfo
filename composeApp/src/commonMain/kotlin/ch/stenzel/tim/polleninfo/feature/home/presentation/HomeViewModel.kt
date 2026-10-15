package ch.stenzel.tim.polleninfo.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.diary.domain.model.swissToday
import ch.stenzel.tim.polleninfo.core.diary.domain.repository.DiaryRepository
import ch.stenzel.tim.polleninfo.core.diary.domain.repository.FeelingPromptPreferences
import ch.stenzel.tim.polleninfo.core.measurement.domain.usecase.GetStationMeasurementUseCase
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStationRepository
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.toAppError
import ch.stenzel.tim.polleninfo.core.session.domain.model.Session
import ch.stenzel.tim.polleninfo.core.session.domain.model.SignedOutException
import ch.stenzel.tim.polleninfo.core.session.domain.model.toSessionAppError
import ch.stenzel.tim.polleninfo.core.session.domain.repository.SessionRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** What Home asks its navigation to do, once. */
sealed interface HomeEvent {
    /** A feeling was tapped while nobody is logged in: open the login, nothing was saved. */
    data object OpenAccount : HomeEvent
}

/**
 * Home: the stored station's reading, and once a Swiss day the feeling prompt.
 *
 * **The prompt follows the session.** Logged out it is shown unless today was dismissed on this
 * phone, and a tap only asks for the login ([HomeEvent.OpenAccount]) — nothing is kept. Logged in it
 * stays hidden until the backend has said whether today is answered ([DiaryRepository.today]) —
 * asked again on every load, refresh and session change, so an answer given on another phone hides
 * it here — and a tap sends the answer to the account. A failed check keeps it hidden until the
 * next one; a dismissal hides it either way.
 */
class HomeViewModel(
    selectedStationRepository: SelectedStationRepository,
    private val getStationMeasurement: GetStationMeasurementUseCase,
    sessionRepository: SessionRepository,
    private val diaryRepository: DiaryRepository,
    private val feelingPromptPreferences: FeelingPromptPreferences,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading(stationName = ""))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    /** The latest stored selection — what [refresh] and [retry] reload. */
    private var selected: SelectedStation? = null

    /**
     * Every load goes through this one job and starts by cancelling it, so a response for an
     * earlier request — the old station, or a refresh overtaken by another — can never overwrite a
     * newer one.
     */
    private var loadJob: Job? = null

    /**
     * Whether anyone is logged in; `null` until the stored session has been read, so the prompt
     * never flashes up before it is known which rules apply.
     */
    private var signedIn: Boolean? = null

    /** The last dismissal as read, valid once [dismissalRead]; until then the prompt stays hidden. */
    private var dismissedOn: LocalDate? = null
    private var dismissalRead = false

    /** What the backend last said about today, for the logged-in prompt. */
    private var today: TodayAnswer = TodayAnswer.Unknown

    /** The check of [today] in flight; a newer one — a session change, a reload — replaces it. */
    private var todayJob: Job? = null

    /** An answer being sent; further taps are ignored until it is done. */
    private var recordJob: Job? = null

    /** Why the last answer was not saved; cleared by the next attempt. */
    private var feelingSaveError: AppError? = null

    init {
        viewModelScope.launch {
            feelingPromptPreferences.dismissedOn.collect { date ->
                dismissedOn = date
                dismissalRead = true
                // A dismissal hides the prompt at once, with no reload.
                refreshPrompt()
            }
        }
        viewModelScope.launch {
            sessionRepository.session
                .distinctUntilChanged()
                .collect { session -> onSession(session) }
        }
        viewModelScope.launch {
            // Observed, not read once. `StartupViewModel` takes only the first value because
            // rebuilding the navigation graph mid-session would yank the user out of the navigation
            // they just triggered; that reason does not transfer here, where a station change is
            // exactly what should reload the readings.
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
     * Logging in or out — here, on the Diary, in Settings, or by the backend refusing the session —
     * starts the prompt over: what the previous account had answered says nothing about this one.
     */
    private fun onSession(session: Session?) {
        signedIn = session != null
        recordJob?.cancel()
        feelingSaveError = null
        today = TodayAnswer.Unknown
        if (session != null) checkToday() else todayJob?.cancel()
        refreshPrompt()
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

    /**
     * Logged in, sends today's answer; the prompt hides once the account holds one. Logged out,
     * asks for the login instead and keeps nothing — the user taps again once logged in.
     */
    fun onFeelingSelected(feeling: Feeling) {
        when (signedIn) {
            null -> return
            false -> {
                feelingSaveError = null
                refreshPrompt()
                _events.trySend(HomeEvent.OpenAccount)
            }
            true -> {
                if (recordJob?.isActive == true) return
                feelingSaveError = null
                refreshPrompt()
                recordJob = viewModelScope.launch {
                    when (val result = diaryRepository.record(feeling)) {
                        // The stored answer, whichever phone gave it, is today's.
                        is Result.Success -> today = TodayAnswer.Known(result.data.date, answered = true)
                        is Result.Failure -> {
                            // A refused session has been cleared, and the session change shows the
                            // logged-out prompt, whose next tap opens the login; no error to add.
                            if (result.exception !is SignedOutException) {
                                feelingSaveError = result.exception.toSessionAppError()
                            }
                        }
                    }
                    refreshPrompt()
                }
            }
        }
    }

    /** Closes the prompt for today without recording an answer, on this phone only. */
    fun onFeelingPromptDismissed() {
        viewModelScope.launch {
            feelingSaveError = null
            // A failed dismissal simply leaves the prompt up to be closed again.
            feelingPromptPreferences.dismiss(swissToday(clock))
            refreshPrompt()
        }
    }

    /**
     * Asks the backend whether today is answered. The previous answer stays in force until the new
     * one arrives, so a refresh does not make a shown prompt blink; a failure hides the prompt
     * until the next check, since a prompt for a day that may be answered must not invite a tap.
     */
    private fun checkToday() {
        todayJob?.cancel()
        todayJob = viewModelScope.launch {
            val date = swissToday(clock)
            today = when (val result = diaryRepository.today()) {
                is Result.Success -> TodayAnswer.Known(date, answered = result.data != null)
                is Result.Failure -> TodayAnswer.Unknown
            }
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
     * first load, refresh or prompt change after Swiss midnight; a logged-in answer known for an
     * earlier day counts as not known.
     */
    private fun withPrompt(content: HomeUiState.Content): HomeUiState.Content {
        val date = swissToday(clock)
        val notDismissed = dismissalRead && dismissedOn != date
        val show = notDismissed && when (signedIn) {
            null -> false
            false -> true
            true -> (today as? TodayAnswer.Known)?.let { it.date == date && !it.answered } ?: false
        }
        return content.copy(
            showFeelingPrompt = show,
            feelingSaveError = feelingSaveError.takeIf { show },
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
            current.copy(isRefreshing = true)
        } else {
            HomeUiState.Loading(station.name)
        }

        // Every load and refresh asks again whether today is answered, so an answer given on
        // another phone, or a new Swiss day, is picked up.
        if (signedIn == true) checkToday()

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
                    drivenBy = result.data.drivenBy,
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

    /** What is known about today's answer in the account. */
    private sealed interface TodayAnswer {
        /** Not asked yet, being asked for the first time, or the last check failed. */
        data object Unknown : TodayAnswer

        /** Whether the Swiss day [date] has an answer. */
        data class Known(val date: LocalDate, val answered: Boolean) : TodayAnswer
    }
}
