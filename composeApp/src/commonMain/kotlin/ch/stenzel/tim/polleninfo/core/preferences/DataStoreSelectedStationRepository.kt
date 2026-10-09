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
 * DataStore-backed [SelectedStationRepository].
 *
 * **Deliberately kept free of logic — it reads and writes two keys and nothing else.** It has no
 * unit test of its own: a real one would need a platform file path, and app tests may not live in
 * `androidUnitTest`. The mitigation is that there is nothing here to get wrong; every consumer is
 * tested against `FakeSelectedStationRepository` instead. Keep it that way — anything worth testing
 * belongs in a caller.
 *
 * The `DataStore<Preferences>` itself is constructed per platform in `platformModule`, because the
 * factory needs a file path and `Dispatchers.IO`, neither of which exists in `commonMain`.
 */
class DataStoreSelectedStationRepository(
    private val dataStore: DataStore<Preferences>,
) : SelectedStationRepository {

    override val selectedStation: Flow<SelectedStation?> = dataStore.data.map { preferences ->
        val abbr = preferences[ABBR_KEY]
        val name = preferences[NAME_KEY]
        if (abbr == null || name == null) null else SelectedStation(abbr = abbr, name = name)
    }

    override suspend fun select(station: SelectedStation): Result<Unit> = safeCall {
        dataStore.edit { preferences ->
            preferences[ABBR_KEY] = station.abbr
            preferences[NAME_KEY] = station.name
        }
    }

    private companion object {
        val ABBR_KEY = stringPreferencesKey("selected_station_abbr")
        val NAME_KEY = stringPreferencesKey("selected_station_name")
    }
}
