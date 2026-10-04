package ch.stenzel.tim.polleninfo.server.alarm.domain

/**
 * A notification ready to send, independent of the push service that delivers it.
 *
 * [data] travels with the notification for the app to read; it holds the station and the alarm so
 * that opening a specific screen from a notification can be added without a backend change.
 */
data class PushMessage(
    val title: String,
    val body: String,
    val channel: PushChannel,
    val data: Map<String, String>,
)

/**
 * The Android notification channel a message is posted on. [id] must equal the channel ids the app
 * creates, so the user's per-channel settings apply.
 */
enum class PushChannel(val id: String) {
    DAILY_REPORT("daily_report"),
    THRESHOLD_ALERT("threshold_alert"),
}
