package ch.stenzel.tim.polleninfo.core.diary.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ch.stenzel.tim.polleninfo.core.diary.domain.repository.FeelingPromptPreferences
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/**
 * DataStore-backed [FeelingPromptPreferences], in the shared store and under the key the on-device
 * diary used, so a dismissal from before the move still holds.
 *
 * Deliberately logic-free, like the other DataStore classes: one ISO date string, no unit test of
 * its own; consumers are tested against `FakeFeelingPromptPreferences`.
 */
class DataStoreFeelingPromptPreferences(
    private val dataStore: DataStore<Preferences>,
) : FeelingPromptPreferences {

    override val dismissedOn: Flow<LocalDate?> =
        dataStore.data.map { preferences -> preferences[DISMISSED_ON_KEY]?.let(DiaryCodec::decodeDate) }

    override suspend fun dismiss(date: LocalDate): Result<Unit> = safeCall {
        dataStore.edit { it[DISMISSED_ON_KEY] = date.toString() }
    }

    private companion object {
        val DISMISSED_ON_KEY = stringPreferencesKey("diary_prompt_dismissed_on")
    }
}
