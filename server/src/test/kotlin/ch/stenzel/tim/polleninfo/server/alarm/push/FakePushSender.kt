package ch.stenzel.tim.polleninfo.server.alarm.push

import ch.stenzel.tim.polleninfo.server.alarm.domain.PushMessage

/**
 * Records every message instead of delivering it. [results] scripts the answer per token (default
 * [PushResult.Sent]); a token in [throwingTokens] makes [send] throw, which a real sender never does,
 * to show that one misbehaving delivery stays contained.
 */
class FakePushSender : PushSender {

    data class Delivery(val token: String, val message: PushMessage)

    val sent = mutableListOf<Delivery>()
    val results = mutableMapOf<String, PushResult>()
    val throwingTokens = mutableSetOf<String>()

    override suspend fun send(token: String, message: PushMessage): PushResult {
        if (token in throwingTokens) throw IllegalStateException("send to $token blew up")
        sent += Delivery(token, message)
        return results[token] ?: PushResult.Sent
    }
}
