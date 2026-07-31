package ch.stenzel.tim.polleninfo.core.location

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberCoarseLocationPermissionRequester(
    onResult: (granted: Boolean) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    // The launcher's callback is registered once; rememberUpdatedState makes it call the newest
    // onResult rather than the one captured on first composition.
    val currentOnResult by rememberUpdatedState(onResult)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> currentOnResult(granted) }

    return remember(context, launcher) {
        {
            val alreadyGranted = context.checkSelfPermission(PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
            // Launching when the permission is already held shows no dialog but still round-trips
            // through the activity result machinery; answering directly keeps the fast path fast.
            if (alreadyGranted) currentOnResult(true) else launcher.launch(PERMISSION)
        }
    }
}

private const val PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION
