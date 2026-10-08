package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.notifications.NotificationPermissionState
import ch.stenzel.tim.polleninfo.core.preferences.NotificationPermissionPreferences
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.species.domain.repository.SpeciesRepository
import ch.stenzel.tim.polleninfo.core.station.domain.repository.StationRepository
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.toAlarmAppError
import ch.stenzel.tim.polleninfo.feature.alarms.domain.repository.AlarmRepository
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Never reads the permission itself: `AlarmsScreen` reads it through the platform controller on
 * every resume and hands the answer to [onPermissionState]. That is what keeps this testable without
 * a platform, as `OnboardingViewModel` does with location.
 *
 * The list and the permission are tracked separately. The list is loaded on the first `ENABLED`
 * state — never before, so a user who has not allowed notifications causes no backend contact and
 * no device registration — and is kept while the permission is revoked, so re-enabling shows the
 * same list again rather than a reload.
 */
class AlarmsViewModel(
    private val permissionPreferences: NotificationPermissionPreferences,
    private val alarmRepository: AlarmRepository,
    private val stationRepository: StationRepository,
    private val speciesRepository: SpeciesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlarmsUiState>(AlarmsUiState.CheckingPermission)
    val uiState: StateFlow<AlarmsUiState> = _uiState.asStateFlow()

    private val _events = Channel<AlarmsEvent>(Channel.BUFFERED)
    val events: Flow<AlarmsEvent> = _events.receiveAsFlow()

    /**
     * The flag the platform controller needs to classify the permission. `null` until it has been
     * read, so the screen does not classify against a guess and flash the wrong button.
     */
    val askedBefore: StateFlow<Boolean?> = permissionPreferences.askedBefore
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    private var permission: NotificationPermissionState? = null

    /** `Loading`, `Content` or `Error`; `null` until the first load starts. */
    private var listState: AlarmsUiState? = null

    /** As on Home: every load cancels the previous one, so an older response never wins. */
    private var loadJob: Job? = null

    /** Station names by abbreviation, kept once loaded; the station list does not change. */
    private var stationNames: Map<String, String>? = null

    /** Species names by id in display order, kept once loaded, like [stationNames]. */
    private var speciesNames: Map<String, String>? = null

    fun onPermissionState(state: NotificationPermissionState) {
        permission = state
        if (state == NotificationPermissionState.ENABLED && listState == null) {
            load(keepAlarms = false)
        } else {
            publish()
        }
    }

    /** The system prompt was answered, whatever the answer. */
    fun onPermissionRequested() {
        viewModelScope.launch {
            // A failed write is not surfaced. Its only effect is that a permanent denial keeps
            // offering "Allow notifications", which Android then answers with an immediate denial.
            permissionPreferences.markAsked()
        }
    }

    /** Pull-to-refresh: the alarms on screen stay there, flagged as refreshing, until replaced. */
    fun refresh() = load(keepAlarms = true)

    /** Leaves the error state by loading again from scratch. */
    fun retry() = load(keepAlarms = false)

    /**
     * The tab is visible again — back from the editor, from another tab or from another app. A list
     * already on screen is reloaded quietly, so a saved alarm shows up without a gesture: the rows
     * stay as they are with no indicator, and are replaced when the new list arrives. A failed quiet
     * reload keeps them; pull-to-refresh is the way to see the error.
     *
     * Nothing happens before the first list has loaded, while a load runs, or without permission —
     * [onPermissionState] owns the first load.
     */
    fun onResume() {
        val current = listState as? AlarmsUiState.Content ?: return
        if (permission != NotificationPermissionState.ENABLED || loadJob?.isActive == true) return

        loadJob = viewModelScope.launch {
            val result = loadItems()
            if (result is Result.Success) {
                listState = current.copy(alarms = result.data)
                publish()
            }
        }
    }

    /**
     * The row's switch. The row shows [enabled] at once and the alarm is stored with it through a
     * full update; if that fails, the switch goes back and [AlarmsEvent.ToggleFailed] says why.
     */
    fun onEnabledToggled(alarmId: String, enabled: Boolean) {
        val item = (listState as? AlarmsUiState.Content)?.alarms?.firstOrNull { it.alarm.id == alarmId } ?: return
        if (item.alarm.enabled == enabled) return
        val toggled = item.alarm.copy(enabled = enabled)
        setEnabled(alarmId, enabled)

        viewModelScope.launch {
            val result = alarmRepository.update(alarmId, toggled.toDraft())
            if (result is Result.Failure) {
                setEnabled(alarmId, !enabled)
                _events.send(AlarmsEvent.ToggleFailed(result.exception.toAlarmAppError(), enabling = enabled))
            }
        }
    }

    private fun setEnabled(alarmId: String, enabled: Boolean) {
        val current = listState as? AlarmsUiState.Content ?: return
        listState = current.copy(
            alarms = current.alarms.map { item ->
                if (item.alarm.id == alarmId) item.copy(alarm = item.alarm.copy(enabled = enabled)) else item
            },
        )
        publish()
    }

    private fun load(keepAlarms: Boolean) {
        loadJob?.cancel()
        val current = listState
        val refreshing = keepAlarms && current is AlarmsUiState.Content
        listState = if (refreshing) {
            (current as AlarmsUiState.Content).copy(isRefreshing = true)
        } else {
            AlarmsUiState.Loading
        }
        publish()

        loadJob = viewModelScope.launch {
            // Held for the same reason as on Home: a fast answer would flip `isRefreshing` and back
            // within one frame, and `PullToRefreshBox` would never retract its indicator.
            val minimumIndicator = if (refreshing) launch { delay(MIN_REFRESH_INDICATOR) } else null
            val result = loadItems()
            minimumIndicator?.join()
            listState = when (result) {
                is Result.Success -> AlarmsUiState.Content(result.data)
                is Result.Failure -> AlarmsUiState.Error(result.exception.toAlarmAppError())
            }
            publish()
        }
    }

    /**
     * Stations and species are fetched only once there is an alarm to describe, so a device without
     * push — which fails before any request — makes no network call at all. A name list that fails
     * falls back to the abbreviation or id rather than failing a list that did load.
     */
    private suspend fun loadItems(): Result<List<AlarmListItem>> {
        val alarms = when (val result = alarmRepository.alarms()) {
            is Result.Success -> result.data
            is Result.Failure -> return result
        }
        if (alarms.isEmpty()) return Result.Success(emptyList())
        val names = stationNames()
        val species = speciesNames()
        return Result.Success(
            alarms.map { alarm ->
                AlarmListItem(
                    alarm = alarm,
                    stationName = names[alarm.stationAbbr] ?: alarm.stationAbbr,
                    summary = summaryOf(alarm, species),
                )
            },
        )
    }

    private suspend fun speciesNames(): Map<String, String> {
        speciesNames?.let { return it }
        return when (val result = speciesRepository.getSpecies()) {
            is Result.Success -> result.data.associate { it.id to it.name }.also { speciesNames = it }
            is Result.Failure -> emptyMap()
        }
    }

    private suspend fun stationNames(): Map<String, String> {
        stationNames?.let { return it }
        return when (val result = stationRepository.getStations()) {
            is Result.Success -> result.data.associate { it.abbr to it.name }.also { stationNames = it }
            is Result.Failure -> emptyMap()
        }
    }

    private fun publish() {
        _uiState.value = when (val state = permission) {
            null -> AlarmsUiState.CheckingPermission
            NotificationPermissionState.ENABLED -> listState ?: AlarmsUiState.Loading
            NotificationPermissionState.CAN_REQUEST,
            NotificationPermissionState.MUST_OPEN_SETTINGS,
            -> AlarmsUiState.PermissionRequired(state)
        }
    }

    companion object {
        /** The shortest time a refresh shows its indicator; see [load] for why one is needed. */
        val MIN_REFRESH_INDICATOR = 500.milliseconds
    }
}
