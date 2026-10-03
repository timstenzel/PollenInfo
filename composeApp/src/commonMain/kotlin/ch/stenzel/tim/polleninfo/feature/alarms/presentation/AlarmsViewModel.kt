package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.notifications.NotificationPermissionState
import ch.stenzel.tim.polleninfo.core.preferences.NotificationPermissionPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Never reads the permission itself: `AlarmsScreen` reads it through the platform controller on
 * every resume and hands the answer to [onPermissionState]. That is what keeps this testable without
 * a platform, as `OnboardingViewModel` does with location.
 */
class AlarmsViewModel(
    private val permissionPreferences: NotificationPermissionPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlarmsUiState>(AlarmsUiState.Loading)
    val uiState: StateFlow<AlarmsUiState> = _uiState.asStateFlow()

    /**
     * The flag the platform controller needs to classify the permission. `null` until it has been
     * read, so the screen does not classify against a guess and flash the wrong button.
     */
    val askedBefore: StateFlow<Boolean?> = permissionPreferences.askedBefore
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    fun onPermissionState(state: NotificationPermissionState) {
        _uiState.value = when (state) {
            NotificationPermissionState.ENABLED -> AlarmsUiState.Content
            NotificationPermissionState.CAN_REQUEST,
            NotificationPermissionState.MUST_OPEN_SETTINGS,
            -> AlarmsUiState.PermissionRequired(state)
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
}
