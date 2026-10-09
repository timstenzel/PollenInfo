package ch.stenzel.tim.polleninfo.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed [DeviceRegistrationRepository].
 *
 * Deliberately logic-free for the same reason as [DataStoreSelectedStationRepository]: it reads and
 * writes one key and has no unit test of its own. Consumers are tested against
 * `FakeDeviceRegistrationRepository`.
 */
class DataStoreDeviceRegistrationRepository(
    private val dataStore: DataStore<Preferences>,
) : DeviceRegistrationRepository {

    override val deviceId: Flow<String?> = dataStore.data.map { it[DEVICE_ID_KEY] }

    override suspend fun store(id: String): Result<Unit> = safeCall {
        dataStore.edit { it[DEVICE_ID_KEY] = id }
    }

    override suspend fun clear(): Result<Unit> = safeCall {
        dataStore.edit { it.remove(DEVICE_ID_KEY) }
    }

    private companion object {
        val DEVICE_ID_KEY = stringPreferencesKey("alarm_device_id")
    }
}
