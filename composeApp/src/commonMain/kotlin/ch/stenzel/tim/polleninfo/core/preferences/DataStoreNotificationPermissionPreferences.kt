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
 * DataStore-backed [NotificationPermissionPreferences].
 *
 * Deliberately logic-free for the same reason as [DataStoreSelectedStationRepository]: it reads and
 * writes one key and has no unit test of its own. Consumers are tested against
 * `FakeNotificationPermissionPreferences`.
 */
class DataStoreNotificationPermissionPreferences(
    private val dataStore: DataStore<Preferences>,
) : NotificationPermissionPreferences {

    override val askedBefore: Flow<Boolean> = dataStore.data.map { it[ASKED_BEFORE_KEY] ?: false }

    override suspend fun markAsked(): Result<Unit> = safeCall {
        dataStore.edit { it[ASKED_BEFORE_KEY] = true }
    }

    private companion object {
        val ASKED_BEFORE_KEY = booleanPreferencesKey("notification_permission_asked_before")
    }
}
