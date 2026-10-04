package ch.stenzel.tim.polleninfo.server.alarm.push

import ch.stenzel.tim.polleninfo.server.alarm.domain.PushMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Stands in for FCM when no credentials are configured, so the backend runs and can be developed
 * locally: it logs what it would have sent and reports it as sent.
 *
 * Only the token's last characters are logged — enough to tell devices apart, not enough to push to one.
 */
class LoggingPushSender(
    private val logger: Logger = LoggerFactory.getLogger(LoggingPushSender::class.java),
) : PushSender {

    override suspend fun send(token: String, message: PushMessage): PushResult {
        logger.info(
            "Push (not sent, no FCM credentials) to …{} on {}: {} — {} {}",
            token.takeLast(TOKEN_SUFFIX_LENGTH),
            message.channel.id,
            message.title,
            message.body,
            message.data,
        )
        return PushResult.Sent
    }

    private companion object {
        const val TOKEN_SUFFIX_LENGTH = 6
    }
}
