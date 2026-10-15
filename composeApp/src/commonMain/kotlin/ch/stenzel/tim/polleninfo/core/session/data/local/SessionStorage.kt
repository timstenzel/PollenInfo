package ch.stenzel.tim.polleninfo.core.session.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ch.stenzel.tim.polleninfo.core.session.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Where this phone keeps its login. Like the device token it lives on the device alone — in a
 * DataStore file of its own that Android's backup rules exclude, so a restored backup or a device
 * transfer never carries a login to another phone.
 */
interface SessionStorage {

    /** `null` while nothing is stored. */
    val session: Flow<Session?>

    suspend fun store(session: Session)

    suspend fun clear()
}

/**
 * DataStore-backed [SessionStorage] over the `polleninfo_session` file. Deliberately logic-free, as
 * the other DataStore classes: it reads and writes two keys and has no unit test of its own;
 * `SessionRepositoryImpl` is tested against a fake.
 */
class DataStoreSessionStorage(private val dataStore: DataStore<Preferences>) : SessionStorage {

    override val session: Flow<Session?> = dataStore.data.map { preferences ->
        val token = preferences[TOKEN_KEY]
        val email = preferences[EMAIL_KEY]
        if (token != null && email != null) Session(token, email) else null
    }

    override suspend fun store(session: Session) {
        dataStore.edit {
            it[TOKEN_KEY] = session.token
            it[EMAIL_KEY] = session.email
        }
    }

    override suspend fun clear() {
        dataStore.edit {
            it.remove(TOKEN_KEY)
            it.remove(EMAIL_KEY)
        }
    }

    private companion object {
        val TOKEN_KEY = stringPreferencesKey("sessionToken")
        val EMAIL_KEY = stringPreferencesKey("email")
    }
}
