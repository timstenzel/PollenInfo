package ch.stenzel.tim.polleninfo.core.location

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * One coarse fix, or a reason there is none.
 *
 * Three outcomes and no more: everything a platform can go wrong with — no provider, provider
 * disabled, no fix in time, delegate failure — collapses into [Unavailable], because the screen has
 * exactly two things it can say and both callers do the same thing with all of those cases.
 */
sealed interface CoarseLocationResult {
    data class Success(val latitude: Double, val longitude: Double) : CoarseLocationResult
    data object PermissionDenied : CoarseLocationResult
    data object Unavailable : CoarseLocationResult
}

/**
 * A single approximate position, resolved on the device.
 *
 * Deliberately *not* responsible for prompting — see [rememberCoarseLocationPermissionRequester] for
 * why the prompt lives in the composition. The actuals bridge their platform's callback API through
 * a cancellable continuation, so cancelling the calling coroutine stops the platform request rather
 * than leaving it running with nobody listening.
 *
 * The result never leaves the device: no API service in this app takes a coordinate.
 */
interface CoarseLocationProvider {
    suspend fun currentLocation(): CoarseLocationResult
}

/**
 * How long a lookup may take before it is treated as [CoarseLocationResult.Unavailable].
 *
 * Applied in common code (`OnboardingViewModel`), not in the actuals: one constant, one place it can
 * drift, and a timeout that is testable under virtual time in `commonTest` rather than only on a
 * device.
 */
val LOCATION_TIMEOUT: Duration = 10.seconds
