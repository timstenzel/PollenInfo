package ch.stenzel.tim.polleninfo.core.push

import ch.stenzel.tim.polleninfo.core.result.Result

/**
 * Tells the backend that this install's push token changed.
 *
 * In `core/` so that the platform's push receiver can report a rotated token without depending on
 * the alarms feature; the alarm repository implements it, since it owns the device registration.
 */
interface PushTokenUpdater {

    /**
     * Sends [token] for the registered device. Without a stored device id there is nothing to
     * update — the first registration sends whatever token is current then — so this succeeds
     * without contacting the backend.
     */
    suspend fun updateToken(token: String): Result<Unit>
}
