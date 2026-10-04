package ch.stenzel.tim.polleninfo.core.push

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ch.stenzel.tim.polleninfo.MainActivity
import ch.stenzel.tim.polleninfo.R
import ch.stenzel.tim.polleninfo.core.result.Result
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.ext.android.inject

/**
 * Receives pollen alarms from FCM.
 *
 * In the background the system shows a notification message by itself, on the channel the backend
 * named, and a tap opens the launcher activity. In the foreground FCM hands the message here
 * instead and shows nothing, so this posts it the same way — same channel, same tap — and the user
 * sees no difference.
 *
 * A rotated token is reported through [PushTokenUpdater] — only for an install that has registered;
 * one that has not sends whatever token is current when it first does.
 */
class PollenFirebaseMessagingService : FirebaseMessagingService() {

    private val tokenUpdater: PushTokenUpdater by inject()

    /**
     * Called on a Firebase worker thread, and the service may be stopped as soon as this returns,
     * so the update runs to completion here rather than in a scope that would be cancelled with it.
     */
    override fun onNewToken(token: String) {
        runBlocking {
            val result = withTimeoutOrNull(TOKEN_UPDATE_TIMEOUT) { tokenUpdater.updateToken(token) }
            if (result !is Result.Success) Log.w(TAG, "Sending the new push token failed: $result")
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        val manager = NotificationManagerCompat.from(this)
        // POST_NOTIFICATIONS revoked, or notifications switched off: nothing may be shown.
        if (!manager.areNotificationsEnabled()) return

        val channel = AlarmNotificationChannel.fromId(notification.channelId) ?: AlarmNotificationChannel.DAILY_REPORT
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val shown = NotificationCompat.Builder(this, channel.id)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        try {
            // Each message gets its own notification; a later one must not replace an earlier one.
            manager.notify((message.messageId ?: message.sentTime.toString()).hashCode(), shown)
        } catch (e: SecurityException) {
            // The permission was revoked between the check above and this call.
        }
    }
}

private const val TAG = "PollenPush"

private val TOKEN_UPDATE_TIMEOUT = 20.seconds
