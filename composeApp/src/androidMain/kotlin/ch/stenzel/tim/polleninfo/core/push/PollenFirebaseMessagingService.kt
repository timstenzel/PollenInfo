package ch.stenzel.tim.polleninfo.core.push

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ch.stenzel.tim.polleninfo.MainActivity
import ch.stenzel.tim.polleninfo.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives pollen alarms from FCM.
 *
 * In the background the system shows a notification message by itself, on the channel the backend
 * named, and a tap opens the launcher activity. In the foreground FCM hands the message here
 * instead and shows nothing, so this posts it the same way — same channel, same tap — and the user
 * sees no difference.
 */
class PollenFirebaseMessagingService : FirebaseMessagingService() {

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
