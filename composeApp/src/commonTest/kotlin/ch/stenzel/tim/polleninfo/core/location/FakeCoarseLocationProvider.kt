package ch.stenzel.tim.polleninfo.core.location

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlin.time.Duration

/**
 * A programmable stand-in for the platform location providers. Hand-written — the project adds no
 * mocking framework.
 *
 * [answerDelay] lets a test observe `isLocating` while a lookup is in flight; [neverAnswers] lets it
 * drive the timeout, since the timeout lives above this interface in the ViewModel and so has to be
 * exercised through a source that genuinely never returns.
 */
class FakeCoarseLocationProvider(
    var result: CoarseLocationResult = CoarseLocationResult.Success(47.4989, 8.7286),
    var answerDelay: Duration = Duration.ZERO,
    var neverAnswers: Boolean = false,
) : CoarseLocationProvider {

    var callCount: Int = 0
        private set

    /**
     * Set when a lookup was cancelled before it could answer. It stands in for "the platform
     * request stopped": the real actuals hang their `invokeOnCancellation` off the same signal.
     */
    var wasCancelled: Boolean = false
        private set

    override suspend fun currentLocation(): CoarseLocationResult {
        callCount++
        try {
            if (neverAnswers) awaitCancellation()
            if (answerDelay > Duration.ZERO) delay(answerDelay)
        } catch (cancellation: CancellationException) {
            wasCancelled = true
            throw cancellation
        }
        return result
    }
}
