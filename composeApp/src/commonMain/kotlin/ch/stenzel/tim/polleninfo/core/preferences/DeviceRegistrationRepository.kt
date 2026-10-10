package ch.stenzel.tim.polleninfo.core.preferences

import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * The secret device token the backend issued when this install registered for alarms.
 *
 * It is the only key to this install's alarms on the backend, so it is kept on the device and
 * nowhere else — not even in a cloud backup or a device transfer. Losing it (clearing app data,
 * restoring onto another phone) loses access to those alarms; the app then registers afresh.
 */
interface DeviceRegistrationRepository {

    /** `null` until a token has been stored, and again after [clear]. */
    val deviceToken: Flow<String?>

    suspend fun store(token: String): Result<Unit>

    suspend fun clear(): Result<Unit>
}
