package ch.stenzel.tim.polleninfo.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed [CrashReportingPreferences].
 *
 * Deliberately logic-free for the same reason as [DataStoreSelectedStationRepository]: it reads and
 * writes one key and has no unit test of its own. Consumers are tested against
 * `FakeCrashReportingPreferences`.
 */
class DataStoreCrashReportingPreferences(
    private val dataStore: DataStore<Preferences>,
) : CrashReportingPreferences {

    override val enabled: Flow<Boolean> = dataStore.data.map { it[ENABLED_KEY] ?: true }

    override suspend fun setEnabled(enabled: Boolean): Result<Unit> = safeCall {
        dataStore.edit { it[ENABLED_KEY] = enabled }
    }

    private companion object {
        val ENABLED_KEY = booleanPreferencesKey("crash_reports_enabled")
    }
}
