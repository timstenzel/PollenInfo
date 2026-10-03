package ch.stenzel.tim.polleninfo.core.notifications

import androidx.compose.runtime.Composable

/**
 * The platform's notification permission: read it, prompt for it, or open the settings for it.
 *
 * A composable for the same reason as `rememberCoarseLocationPermissionRequester`: Android's prompt
 * needs an activity-scoped `ActivityResultLauncher`, which only a composition can provide. The screen
 * reads the state and hands it to `AlarmsViewModel.onPermissionState`, so the ViewModel touches no
 * platform API.
 *
 * Untested by construction, like the location requester; it is checked by hand.
 */
interface NotificationPermissionController {

    /**
     * The current state. [askedBefore] is the persisted "the prompt was shown once" flag
     * (`NotificationPermissionPreferences`), which Android needs to tell a first request from a
     * permanent denial; iOS knows this itself and ignores it.
     *
     * Suspending because iOS only answers through a completion handler.
     */
    suspend fun currentStatus(askedBefore: Boolean): NotificationPermissionState

    /** Shows the system prompt and answers [onResult] with whether notifications were allowed. */
    fun request(onResult: (granted: Boolean) -> Unit)

    /** Opens this app's notification settings. */
    fun openSettings()
}

@Composable
expect fun rememberNotificationPermissionController(): NotificationPermissionController
