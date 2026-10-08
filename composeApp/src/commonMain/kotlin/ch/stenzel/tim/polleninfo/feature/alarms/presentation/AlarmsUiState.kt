package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import ch.stenzel.tim.polleninfo.core.notifications.NotificationPermissionState
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.Alarm
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.MAX_ALARMS

/** What the Alarms tab shows. Alarms are only reachable once notifications are allowed. */
sealed interface AlarmsUiState {

    /** The permission has not been read yet. Lasts a moment, so the screen shows nothing. */
    data object CheckingPermission : AlarmsUiState

    /** [state] is `CAN_REQUEST` or `MUST_OPEN_SETTINGS`, and picks the button the screen offers. */
    data class PermissionRequired(val state: NotificationPermissionState) : AlarmsUiState

    /** Notifications are allowed and the list is loading: the first time, or again after an error. */
    data object Loading : AlarmsUiState

    /** [isRefreshing] keeps [alarms] on screen while a pull-to-refresh runs. */
    data class Content(
        val alarms: List<AlarmListItem>,
        val isRefreshing: Boolean = false,
    ) : AlarmsUiState {
        /** The device holds [MAX_ALARMS]: "Create alarm" is disabled and a hint says why. */
        val limitReached: Boolean get() = alarms.size >= MAX_ALARMS
    }

    /**
     * The list could not be loaded. [AppError.PushUnavailable] means it never can be on this device,
     * so the screen explains that instead of offering a retry.
     */
    data class Error(val error: AppError) : AlarmsUiState
}

/**
 * One row of the list: the alarm and its station's display name. [speciesNames] are the backend's
 * pollen-type names by id in display order (empty if they could not be loaded); the screen words
 * the row's summary from them in the app's language with `summaryOf`.
 */
data class AlarmListItem(
    val alarm: Alarm,
    val stationName: String,
    val speciesNames: Map<String, String>,
)
