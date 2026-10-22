package ch.stenzel.tim.polleninfo.core.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyReduced
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * Coarse location from `CLLocationManager` at reduced accuracy.
 *
 * Checked by hand on the iOS Simulator (`iosApp/CHECKLIST.md`): a simulated position near Zurich
 * suggests Zürich, no position at all ends in the shared timeout, and a station picked meanwhile is
 * kept. It depends on two `Info.plist` keys of the wrapper — see `CLAUDE.md`.
 *
 * `.restricted` is mapped to [CoarseLocationResult.PermissionDenied] like `.denied`, which sends the
 * user to Settings for something they may not be able to change. Knowingly imprecise, and preferred
 * over inventing a third message the screen would have to explain.
 */
class IosCoarseLocationProvider : CoarseLocationProvider {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun currentLocation(): CoarseLocationResult {
        val manager = CLLocationManager()
        val status = manager.authorizationStatus
        val granted = status == kCLAuthorizationStatusAuthorizedWhenInUse ||
            status == kCLAuthorizationStatusAuthorizedAlways
        if (!granted) return CoarseLocationResult.PermissionDenied

        return suspendCancellableCoroutine { continuation ->
            val delegate = SingleFixDelegate(continuation)
            manager.desiredAccuracy = kCLLocationAccuracyReduced
            // CLLocationManager holds its delegate weakly, so the strong reference below — captured
            // by the cancellation handler — is what keeps it alive until a callback arrives.
            manager.delegate = delegate

            continuation.invokeOnCancellation {
                manager.stopUpdatingLocation()
                manager.delegate = null
                delegate.detach()
            }

            manager.requestLocation()
        }
    }
}

/**
 * Answers [continuation] with the first thing `CLLocationManager` reports, once.
 *
 * Both delegate methods can fire, and a continuation may only be resumed once, so the first answer
 * wins and later ones are dropped.
 */
private class SingleFixDelegate(
    private val continuation: CancellableContinuation<CoarseLocationResult>,
) : NSObject(), CLLocationManagerDelegateProtocol {

    private var answered = false

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation
        answer(location?.toResult() ?: CoarseLocationResult.Unavailable)
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        answer(CoarseLocationResult.Unavailable)
    }

    fun detach() {
        answered = true
    }

    private fun answer(result: CoarseLocationResult) {
        if (answered) return
        answered = true
        if (continuation.isActive) continuation.resume(result)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun CLLocation.toResult(): CoarseLocationResult =
    coordinate.useContents { CoarseLocationResult.Success(latitude, longitude) }
