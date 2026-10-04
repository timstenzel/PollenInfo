package ch.stenzel.tim.polleninfo.core.push

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

/**
 * The notification channels pollen alarms arrive on, one per alarm type, so the user can mute one
 * kind in system settings without losing the other.
 *
 * The ids are the backend's `PushChannel` ids: FCM files a message under the channel its
 * `channel_id` names, and one the app never created falls back to a generic channel.
 */
enum class AlarmNotificationChannel(val id: String, val displayName: String) {
    DAILY_REPORT("daily_report", "Daily reports"),
    THRESHOLD_ALERT("threshold_alert", "Threshold alerts"),
    ;

    companion object {
        fun fromId(id: String?): AlarmNotificationChannel? = entries.firstOrNull { it.id == id }
    }
}

/** Creates every [AlarmNotificationChannel]. Safe to call on every start: existing channels are kept as the user left them. */
fun createAlarmNotificationChannels(context: Context) {
    NotificationManagerCompat.from(context).createNotificationChannelsCompat(
        AlarmNotificationChannel.entries.map { channel ->
            NotificationChannelCompat.Builder(channel.id, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(channel.displayName)
                .build()
        },
    )
}
