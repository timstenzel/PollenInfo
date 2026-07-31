package ch.stenzel.tim.polleninfo.core.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Resolves, once per launch, whether onboarding is still needed.
 *
 * Lives in `core/startup` rather than in the onboarding slice because this is app-level
 * composition — it decides which slice the app opens on, so it cannot belong to either of them.
 *
 * It takes only the **first** value of the stored selection, not an ongoing subscription: this
 * answers "where does the app start", which is a question asked once. Following the flow would
 * rebuild the navigation graph the moment onboarding writes its result, yanking the user out of the
 * navigation the write just triggered.
 */
class StartupViewModel(
    selectedStationRepository: SelectedStationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<StartupState>(StartupState.Loading)
    val state: StateFlow<StartupState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = when (selectedStationRepository.selectedStation.first()) {
                null -> StartupState.NeedsOnboarding
                else -> StartupState.Ready
            }
        }
    }
}
