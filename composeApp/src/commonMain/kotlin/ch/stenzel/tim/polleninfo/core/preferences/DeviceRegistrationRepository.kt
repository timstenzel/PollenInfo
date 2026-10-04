package ch.stenzel.tim.polleninfo.core.preferences

import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * The anonymous device id the backend issued when this install registered for alarms.
 *
 * It is the only key to this install's alarms on the backend, so it is kept on the device and
 * nowhere else. Losing it (clearing app data) loses access to those alarms.
 */
interface DeviceRegistrationRepository {

    /** `null` until an id has been stored, and again after [clear]. */
    val deviceId: Flow<String?>

    suspend fun store(id: String): Result<Unit>

    suspend fun clear(): Result<Unit>
}
