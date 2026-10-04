package ch.stenzel.tim.polleninfo.core.push

/**
 * The address the backend sends this install's push notifications to.
 *
 * A plain interface bound in `platformModule`, like `CoarseLocationProvider`: only the
 * implementation differs between platforms, not the shape.
 */
interface PushTokenProvider {

    /**
     * [PushTokenResult.Unavailable] means this platform cannot receive push at all, which is a
     * permanent answer. A transient failure to obtain a token — no network, Play services busy —
     * is thrown instead, so the caller treats it as an error worth retrying.
     */
    suspend fun token(): PushTokenResult
}

sealed interface PushTokenResult {
    data class Available(val token: String) : PushTokenResult

    /** Push is not supported here: iOS until the APNs leg exists. */
    data object Unavailable : PushTokenResult
}
