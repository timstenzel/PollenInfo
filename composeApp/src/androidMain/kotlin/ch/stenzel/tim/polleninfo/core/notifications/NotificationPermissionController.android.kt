package ch.stenzel.tim.polleninfo.core.notifications

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat

@Composable
actual fun rememberNotificationPermissionController(): NotificationPermissionController {
    val context = LocalContext.current
    // Each request installs its own callback here; the launcher itself is registered only once.
    val pendingResult = remember { PendingResult() }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> pendingResult.deliver(granted) }

    return remember(context, launcher) {
        AndroidNotificationPermissionController(context.findActivity(), launcher, pendingResult)
    }
}

private class AndroidNotificationPermissionController(
    private val activity: Activity,
    private val launcher: ActivityResultLauncher<String>,
    private val pendingResult: PendingResult,
) : NotificationPermissionController {

    /**
     * `areNotificationsEnabled` covers both the runtime permission (API 33+) and the app-wide switch
     * in system settings, so a granted permission with notifications switched off is not ENABLED.
     *
     * When not enabled on API 33+, a rationale means one denial so far, so the prompt is still
     * available. No rationale is ambiguous: either never asked, or denied for good. [askedBefore]
     * decides. Below API 33 there is no prompt at all, so settings is the only route.
     */
    override suspend fun currentStatus(askedBefore: Boolean): NotificationPermissionState = when {
        NotificationManagerCompat.from(activity).areNotificationsEnabled() ->
            NotificationPermissionState.ENABLED

        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ->
            NotificationPermissionState.MUST_OPEN_SETTINGS

        askedBefore && !ActivityCompat.shouldShowRequestPermissionRationale(activity, PERMISSION) ->
            NotificationPermissionState.MUST_OPEN_SETTINGS

        else -> NotificationPermissionState.CAN_REQUEST
    }

    override fun request(onResult: (granted: Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // No runtime prompt exists; answer with what there is rather than leave the caller waiting.
            onResult(NotificationManagerCompat.from(activity).areNotificationsEnabled())
            return
        }
        pendingResult.callback = onResult
        launcher.launch(PERMISSION)
    }

    override fun openSettings() {
        activity.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName),
        )
    }

    private companion object {
        // Only referenced on API 33+; the constant itself is inlined at compile time.
        const val PERMISSION = Manifest.permission.POST_NOTIFICATIONS
    }
}

/** Holds the callback of the request in flight, so the once-registered launcher can reach it. */
private class PendingResult {
    var callback: ((Boolean) -> Unit)? = null

    fun deliver(granted: Boolean) {
        callback?.invoke(granted)
        callback = null
    }
}

/** Compose hands out the activity wrapped in `ContextWrapper`s; the rationale check needs the activity. */
private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("Notification permission requested outside an activity")
}
