package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import ch.stenzel.tim.polleninfo.core.notifications.NotificationPermissionState
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.Alarm

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
    ) : AlarmsUiState

    /**
     * The list could not be loaded. [pushUnavailable] means it never can be on this device, so the
     * screen explains that instead of offering a retry.
     */
    data class Error(
        val message: String,
        val pushUnavailable: Boolean = false,
    ) : AlarmsUiState
}

/** One row of the list: the alarm, its station's display name and a one-line summary. */
data class AlarmListItem(
    val alarm: Alarm,
    val stationName: String,
    val summary: String,
)
