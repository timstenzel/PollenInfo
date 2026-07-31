package ch.stenzel.tim.polleninfo.core.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.darwin.NSObject

/**
 * Untested by construction: there is no iOS app wrapper in this project, so this is compile-verified
 * only. It also depends on `NSLocationWhenInUseUsageDescription` being present in the wrapper's
 * `Info.plist` — without it iOS silently never prompts. See `CLAUDE.md`.
 */
@Composable
actual fun rememberCoarseLocationPermissionRequester(
    onResult: (granted: Boolean) -> Unit,
): () -> Unit {
    val currentOnResult by rememberUpdatedState(onResult)
    val manager = remember { CLLocationManager() }
    val delegate = remember {
        AuthorizationDelegate { status -> currentOnResult(status.isGranted()) }
    }

    DisposableEffect(manager, delegate) {
        manager.delegate = delegate
        onDispose { manager.delegate = null }
    }

    return remember(manager) {
        {
            val status = manager.authorizationStatus
            // A determined status needs no prompt — iOS would ignore the request anyway, leaving the
            // caller waiting for a delegate callback that never arrives.
            if (status == kCLAuthorizationStatusNotDetermined) {
                manager.requestWhenInUseAuthorization()
            } else {
                currentOnResult(status.isGranted())
            }
        }
    }
}

private class AuthorizationDelegate(
    private val onStatus: (CLAuthorizationStatus) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        val status = manager.authorizationStatus
        // Ignore the callback iOS fires while the prompt is still on screen; only a resolved answer
        // is one the caller can act on.
        if (status != kCLAuthorizationStatusNotDetermined) onStatus(status)
    }
}

private fun CLAuthorizationStatus.isGranted(): Boolean =
    this == kCLAuthorizationStatusAuthorizedWhenInUse ||
        this == kCLAuthorizationStatusAuthorizedAlways
