package ch.stenzel.tim.polleninfo.core.push

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import ch.stenzel.tim.polleninfo.R
import ch.stenzel.tim.polleninfo.core.language.appLanguageContext

/**
 * The notification channels pollen alarms arrive on, one per alarm type, so the user can mute one
 * kind in system settings without losing the other.
 *
 * The ids are the backend's `PushChannel` ids: FCM files a message under the channel its
 * `channel_id` names, and one the app never created falls back to a generic channel.
 */
enum class AlarmNotificationChannel(val id: String, @StringRes val nameRes: Int) {
    DAILY_REPORT("daily_report", R.string.notification_channel_daily_report),
    THRESHOLD_ALERT("threshold_alert", R.string.notification_channel_threshold_alert),
    ;

    companion object {
        fun fromId(id: String?): AlarmNotificationChannel? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Creates every [AlarmNotificationChannel], named in the app's language. Safe to call again: an
 * existing channel keeps the importance and sound the user gave it and only takes the new name, which
 * is how the names follow a language change — `MainActivity` calls this whenever it is created.
 */
fun createAlarmNotificationChannels(context: Context) {
    val localized = context.appLanguageContext()
    NotificationManagerCompat.from(context).createNotificationChannelsCompat(
        AlarmNotificationChannel.entries.map { channel ->
            NotificationChannelCompat.Builder(channel.id, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(localized.getString(channel.nameRes))
                .build()
        },
    )
}
