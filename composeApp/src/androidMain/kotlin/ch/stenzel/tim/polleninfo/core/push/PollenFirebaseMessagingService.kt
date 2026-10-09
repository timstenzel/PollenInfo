package ch.stenzel.tim.polleninfo.core.push

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ch.stenzel.tim.polleninfo.shared.R
import ch.stenzel.tim.polleninfo.core.language.appLanguageContext
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.ui.format.loadDateWording
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Locale
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.getString
import org.koin.android.ext.android.inject

/**
 * Receives pollen alarms from FCM and writes them as notifications in the app's language.
 *
 * The backend sends data-only messages — what happened, not text — so every alarm reaches
 * [onMessageReceived], whether the app is in the foreground, in the background or not running:
 * one path, the same channel, icon and tap (which opens the app). A message it does not fully
 * understand is still shown, as "Pollen in <station>" ([parseAlarmPayload]).
 *
 * The install's push address — its Firebase Installation ID, reported by [onRegistered] whenever
 * FCM (re)registers it — is sent on through [PushTokenUpdater], only for an install that has
 * registered with the backend; one that has not sends whichever id is current when it first does.
 */
class PollenFirebaseMessagingService : FirebaseMessagingService() {

    private val tokenUpdater: PushTokenUpdater by inject()

    /**
     * Called on a Firebase worker thread, and the service may be stopped as soon as this returns,
     * so the update runs to completion here rather than in a scope that would be cancelled with it.
     */
    override fun onRegistered(installationId: String) {
        runBlocking {
            val result = withTimeoutOrNull(TOKEN_UPDATE_TIMEOUT) { tokenUpdater.updateToken(installationId) }
            if (result !is Result.Success) Log.w(TAG, "Sending the new push address failed: $result")
        }
    }

    /**
     * Every alarm arrives here — the backend sends data-only messages, so FCM never shows one by
     * itself, in the foreground or not. Called on a Firebase worker thread, so the strings are loaded
     * blocking it.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val manager = NotificationManagerCompat.from(this)
        // POST_NOTIFICATIONS revoked, or notifications switched off: nothing may be shown.
        if (!manager.areNotificationsEnabled()) return

        val content = parseAlarmPayload(message.data)
        val text = runBlocking {
            useAppLanguage()
            notificationText(content, loadDateWording(), Clock.System) { resource, args ->
                getString(resource, *args.toTypedArray())
            }
        }
        val channel = AlarmNotificationChannel.fromId(content.channel.id) ?: AlarmNotificationChannel.DAILY_REPORT
        // The app's launcher activity (:androidApp's MainActivity), which this library cannot name.
        val openApp = packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
            PendingIntent.getActivity(
                this,
                0,
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        val shown = NotificationCompat.Builder(this, channel.id)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(text.title)
            .setContentText(text.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.body))
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

    /**
     * Compose resources outside composition resolve against `Locale.getDefault()`. In a process FCM
     * started without an activity that may still be the device's language, so it is aligned with the
     * app's language first — what AppCompat does for its activities anyway.
     */
    private fun useAppLanguage() {
        val locales = appLanguageContext().resources.configuration.locales
        if (!locales.isEmpty && Locale.getDefault() != locales[0]) Locale.setDefault(locales[0])
    }
}

private const val TAG = "PollenPush"

private val TOKEN_UPDATE_TIMEOUT = 20.seconds
