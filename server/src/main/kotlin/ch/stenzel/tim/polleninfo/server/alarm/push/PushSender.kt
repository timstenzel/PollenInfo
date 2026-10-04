package ch.stenzel.tim.polleninfo.server.alarm.push

import ch.stenzel.tim.polleninfo.server.alarm.domain.PushMessage

/** Delivers a [PushMessage] to one device's push token. Never throws for a delivery failure. */
interface PushSender {
    suspend fun send(token: String, message: PushMessage): PushResult
}

sealed interface PushResult {

    data object Sent : PushResult

    /** The push service no longer knows the token — the app was uninstalled, say. Retrying is pointless. */
    data object Unregistered : PushResult

    /** Anything else: the service failed, timed out or could not be reached. A later attempt may work. */
    data class Failed(val cause: Throwable) : PushResult
}
