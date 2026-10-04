package ch.stenzel.tim.polleninfo.core.push

/**
 * iOS has no push leg yet (that needs APNs and the app wrapper), so there is never a token. The
 * Alarms tab says so instead of registering a device that could never be reached.
 */
class UnavailablePushTokenProvider : PushTokenProvider {
    override suspend fun token(): PushTokenResult = PushTokenResult.Unavailable
}
