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
 * DataStore-backed [DeviceRegistrationRepository], over a DataStore of its own
 * (`polleninfo_device`) so Android's backup rules can leave exactly this file out.
 *
 * Deliberately logic-free for the same reason as [DataStoreSelectedStationRepository]: it reads and
 * writes one key and has no unit test of its own. Consumers are tested against
 * `FakeDeviceRegistrationRepository`.
 */
class DataStoreDeviceRegistrationRepository(
    private val dataStore: DataStore<Preferences>,
) : DeviceRegistrationRepository {

    override val deviceToken: Flow<String?> = dataStore.data.map { it[DEVICE_TOKEN_KEY] }

    override suspend fun store(token: String): Result<Unit> = safeCall {
        dataStore.edit { it[DEVICE_TOKEN_KEY] = token }
    }

    override suspend fun clear(): Result<Unit> = safeCall {
        dataStore.edit { it.remove(DEVICE_TOKEN_KEY) }
    }

    private companion object {
        val DEVICE_TOKEN_KEY = stringPreferencesKey("deviceToken")
    }
}
