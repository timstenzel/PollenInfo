package ch.stenzel.tim.polleninfo.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Coarse location from the platform's **network provider only**.
 *
 * No Play Services and no `FusedLocationProviderClient`: one coarse fix does not justify the
 * dependency. No GPS fallback either — `ACCESS_COARSE_LOCATION` alone does not grant access to
 * `GPS_PROVIDER`, and `FUSED_PROVIDER` needs API 31 while `minSdk` is 26.
 *
 * `LocationManagerCompat.getCurrentLocation` rather than the framework method of the same name,
 * which only exists from API 30. It returns a sufficiently fresh cached fix when there is one, which
 * is why there is no explicit `getLastKnownLocation` fallback: that could hand back a days-old
 * position in another country, silently resolving to the wrong station.
 */
class AndroidCoarseLocationProvider(private val context: Context) : CoarseLocationProvider {

    override suspend fun currentLocation(): CoarseLocationResult {
        if (context.checkSelfPermission(PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            return CoarseLocationResult.PermissionDenied
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return CoarseLocationResult.Unavailable

        return suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            // Cancelling the calling coroutine must reach the platform, not just abandon the result.
            continuation.invokeOnCancellation { cancellationSignal.cancel() }

            try {
                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    LocationManager.NETWORK_PROVIDER,
                    cancellationSignal,
                    ContextCompat.getMainExecutor(context),
                ) { location ->
                    // A null location means the provider gave up without a fix.
                    continuation.resume(
                        location
                            ?.let { CoarseLocationResult.Success(it.latitude, it.longitude) }
                            ?: CoarseLocationResult.Unavailable,
                    )
                }
            } catch (e: IllegalArgumentException) {
                // The image has no network provider at all — common on emulator system images.
                continuation.resume(CoarseLocationResult.Unavailable)
            } catch (e: SecurityException) {
                // The permission was revoked between the check above and this call. Reported as
                // Unavailable rather than PermissionDenied: this is a race in the millisecond
                // between two lines, and the screen's two messages both end in "pick a station
                // manually", which is what the user should do either way.
                continuation.resume(CoarseLocationResult.Unavailable)
            }
        }
    }
}

private const val PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION
