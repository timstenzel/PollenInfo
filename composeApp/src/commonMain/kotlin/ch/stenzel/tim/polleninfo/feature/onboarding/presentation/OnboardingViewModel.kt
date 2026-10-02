package ch.stenzel.tim.polleninfo.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.location.CoarseLocationProvider
import ch.stenzel.tim.polleninfo.core.location.CoarseLocationResult
import ch.stenzel.tim.polleninfo.core.location.LOCATION_TIMEOUT
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStation
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStationRepository
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.station.domain.repository.StationRepository
import ch.stenzel.tim.polleninfo.feature.onboarding.domain.usecase.FindNearestStationUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class OnboardingViewModel(
    private val stationRepository: StationRepository,
    private val selectedStationRepository: SelectedStationRepository,
    private val coarseLocationProvider: CoarseLocationProvider,
    private val findNearestStation: FindNearestStationUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Loading)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** One-shot events — see [OnboardingEvent] for why completion is not a flag on the UI state. */
    private val _events = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events: Flow<OnboardingEvent> = _events.receiveAsFlow()

    /** The lookup currently in flight, if any. Held only so [onStationSelected] can call it off. */
    private var locationJob: Job? = null

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

    /**
     * Records the user's pick. Nothing is persisted until they confirm it.
     *
     * A pick also calls off any lookup still running. Cancelling rather than filtering the late
     * result is what makes "a fix that lands after the user has chosen must not silently replace
     * their choice" structural instead of guarded: there is no result left to apply. It also
     * reaches the platform — both actuals bridge through a cancellable continuation — so the
     * device stops looking for a position nobody is waiting for.
     *
     * Both error flags clear here as well: neither outlives the pick that answers it.
     */
    fun onStationSelected(station: Station) {
        locationJob?.cancel()
        locationJob = null

        _uiState.update { state ->
            if (state is OnboardingUiState.Content) {
                state.copy(
                    selected = station,
                    isLocating = false,
                    locationError = null,
                    saveError = false,
                )
            } else {
                state
            }
        }
    }

    /**
     * The answer to the permission prompt the screen showed.
     *
     * The prompt itself lives in the composition — Android's launcher is activity-scoped — so the
     * ViewModel learns of it only as this boolean and stays free of platform APIs.
     *
     * A refusal is recorded and nothing else: it leaves the shortcut usable, so a user who changes
     * their mind while the device still permits a prompt can grant it without leaving the app.
     */
    fun onPermissionResult(granted: Boolean) {
        val content = _uiState.value as? OnboardingUiState.Content ?: return

        if (!granted) {
            _uiState.value = content.copy(
                isLocating = false,
                locationError = LocationError.PERMISSION_DENIED,
            )
            return
        }

        // Retrying clears the previous complaint the moment the attempt starts, rather than only
        // once it succeeds — a message that outlives its cause reads as the new attempt failing.
        _uiState.value = content.copy(isLocating = true, locationError = null)
        locationJob?.cancel()
        locationJob = viewModelScope.launch { resolveNearestStation() }
    }

    /**
     * Fills in the dropdown with the nearest station — it never confirms. The shortcut proposes; the
     * user still commits, so a surprising proposal is visible and overridable.
     *
     * The timeout is applied here rather than in either platform actual: one [LOCATION_TIMEOUT]
     * constant that cannot drift between platforms, and one testable under virtual time.
     */
    private suspend fun resolveNearestStation() {
        val result = withTimeoutOrNull(LOCATION_TIMEOUT) { coarseLocationProvider.currentLocation() }
            ?: CoarseLocationResult.Unavailable

        _uiState.update { state ->
            if (state !is OnboardingUiState.Content) return@update state

            when (result) {
                is CoarseLocationResult.Success -> {
                    val nearest =
                        findNearestStation(result.latitude, result.longitude, state.stations)
                    if (nearest != null) {
                        state.copy(selected = nearest, isLocating = false, locationError = null)
                    } else {
                        // Only reachable with an empty station list. Reported rather than shrugged
                        // off: a lookup that ends with no visible outcome looks like a hang.
                        state.copy(isLocating = false, locationError = LocationError.UNAVAILABLE)
                    }
                }

                CoarseLocationResult.PermissionDenied -> state.copy(
                    isLocating = false,
                    locationError = LocationError.PERMISSION_DENIED,
                )

                // Includes the timeout above: from the screen's point of view "nothing arrived in
                // ten seconds" and "no provider" are the same problem with the same answer.
                CoarseLocationResult.Unavailable -> state.copy(
                    isLocating = false,
                    locationError = LocationError.UNAVAILABLE,
                )
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
