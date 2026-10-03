package ch.stenzel.tim.polleninfo.core.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatus
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Untested by construction: there is no iOS app wrapper in this project, so this is compile-verified
 * only. Unlike location, no `Info.plist` key is needed to prompt for notifications.
 */
@Composable
actual fun rememberNotificationPermissionController(): NotificationPermissionController =
    remember { IosNotificationPermissionController() }

private class IosNotificationPermissionController : NotificationPermissionController {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    /** iOS records a determined answer itself, so [askedBefore] is not needed here. */
    override suspend fun currentStatus(askedBefore: Boolean): NotificationPermissionState =
        suspendCoroutine { continuation ->
            center.getNotificationSettingsWithCompletionHandler { settings ->
                continuation.resume(settings?.authorizationStatus.toState())
            }
        }

    override fun request(onResult: (granted: Boolean) -> Unit) {
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        ) { granted, _ ->
            // The completion handler runs on a background queue; the caller is UI code.
            dispatch_async(dispatch_get_main_queue()) { onResult(granted) }
        }
    }

    override fun openSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
    }
}

private fun UNAuthorizationStatus?.toState(): NotificationPermissionState = when (this) {
    UNAuthorizationStatusAuthorized,
    UNAuthorizationStatusProvisional,
    UNAuthorizationStatusEphemeral,
    -> NotificationPermissionState.ENABLED

    UNAuthorizationStatusNotDetermined -> NotificationPermissionState.CAN_REQUEST

    // Denied, and anything a later iOS adds: settings is the one route that always exists.
    else -> NotificationPermissionState.MUST_OPEN_SETTINGS
}
