package ch.stenzel.tim.polleninfo.core.push

/** Replays [result] and counts how often a token was asked for. */
class FakePushTokenProvider(
    var result: PushTokenResult = PushTokenResult.Available("fcm-token-1"),
) : PushTokenProvider {

    var callCount: Int = 0
        private set

    override suspend fun token(): PushTokenResult {
        callCount++
        return result
    }
}
