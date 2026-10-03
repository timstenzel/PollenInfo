package ch.stenzel.tim.polleninfo.core.preferences

import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Whether the app has ever shown the system notification-permission prompt.
 *
 * Android cannot answer this itself. Before the first request and after the user has permanently
 * denied, `shouldShowRequestPermissionRationale` returns `false` both times. Only this flag tells
 * "never asked, so prompt" apart from "denied for good, so send them to settings". See
 * `rememberNotificationPermissionController`.
 */
interface NotificationPermissionPreferences {

    /** `false` until [markAsked] has succeeded once. */
    val askedBefore: Flow<Boolean>

    suspend fun markAsked(): Result<Unit>
}
