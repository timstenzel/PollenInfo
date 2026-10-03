package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import ch.stenzel.tim.polleninfo.core.notifications.NotificationPermissionState

/** What the Alarms tab shows. Alarms are only reachable once notifications are allowed. */
sealed interface AlarmsUiState {

    /** The permission has not been read yet. Lasts a moment, so the screen shows nothing. */
    data object Loading : AlarmsUiState

    /** [state] is `CAN_REQUEST` or `MUST_OPEN_SETTINGS`, and picks the button the screen offers. */
    data class PermissionRequired(val state: NotificationPermissionState) : AlarmsUiState

    /**
     * Notifications are allowed. For now always the empty list: the alarms loaded from the backend
     * replace this placeholder once the backend can store them.
     */
    data object Content : AlarmsUiState
}
