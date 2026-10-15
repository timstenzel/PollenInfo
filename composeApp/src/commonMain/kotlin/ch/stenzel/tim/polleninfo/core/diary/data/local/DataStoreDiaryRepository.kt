package ch.stenzel.tim.polleninfo.core.diary.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.repository.LocalDiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed [LocalDiaryRepository]: the answers recorded on this phone, read-only.
 *
 * Deliberately logic-free for the same reason as `DataStoreSelectedStationRepository`: it reads one
 * string key and has no unit test of its own. The encoding is [DiaryCodec], pure and tested;
 * consumers are tested against `FakeLocalDiaryRepository`.
 */
class DataStoreDiaryRepository(
    private val dataStore: DataStore<Preferences>,
) : LocalDiaryRepository {

    override val entries: Flow<List<DiaryEntry>> =
        dataStore.data.map { preferences -> preferences[ENTRIES_KEY]?.let(DiaryCodec::decode).orEmpty() }

    private companion object {
        val ENTRIES_KEY = stringPreferencesKey("diary_entries")
    }
}
