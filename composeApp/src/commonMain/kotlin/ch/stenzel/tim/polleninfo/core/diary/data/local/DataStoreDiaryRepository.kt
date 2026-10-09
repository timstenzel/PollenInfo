package ch.stenzel.tim.polleninfo.core.diary.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.diary.domain.model.recording
import ch.stenzel.tim.polleninfo.core.diary.domain.repository.DiaryRepository
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/**
 * DataStore-backed [DiaryRepository].
 *
 * Deliberately logic-free for the same reason as `DataStoreSelectedStationRepository`: it reads and
 * writes two string keys and has no unit test of its own. The encoding is [DiaryCodec] and the
 * "never overwrite a date" rule is [recording], both pure and tested; consumers are tested against
 * `FakeDiaryRepository`.
 */
class DataStoreDiaryRepository(
    private val dataStore: DataStore<Preferences>,
) : DiaryRepository {

    override val entries: Flow<List<DiaryEntry>> =
        dataStore.data.map { preferences -> preferences[ENTRIES_KEY]?.let(DiaryCodec::decode).orEmpty() }

    override val dismissedOn: Flow<LocalDate?> =
        dataStore.data.map { preferences -> preferences[DISMISSED_ON_KEY]?.let(DiaryCodec::decodeDate) }

    override suspend fun record(date: LocalDate, feeling: Feeling): Result<Unit> = safeCall {
        // Read and write in one edit, so two quick taps cannot both see "no entry yet".
        dataStore.edit { preferences ->
            val current = preferences[ENTRIES_KEY]?.let(DiaryCodec::decode).orEmpty()
            preferences[ENTRIES_KEY] = DiaryCodec.encode(current.recording(date, feeling))
        }
    }

    override suspend fun dismiss(date: LocalDate): Result<Unit> = safeCall {
        dataStore.edit { it[DISMISSED_ON_KEY] = date.toString() }
    }

    private companion object {
        val ENTRIES_KEY = stringPreferencesKey("diary_entries")
        val DISMISSED_ON_KEY = stringPreferencesKey("diary_prompt_dismissed_on")
    }
}
