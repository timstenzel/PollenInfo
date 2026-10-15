package ch.stenzel.tim.polleninfo.core.diary

import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.diary.domain.model.recording
import ch.stenzel.tim.polleninfo.core.diary.domain.repository.LocalDiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate

/**
 * In-memory stand-in for `DataStoreDiaryRepository`. [add] stands in for an answer that reaches
 * the stored answers while a test runs, with the same never-overwrite rule.
 */
class FakeLocalDiaryRepository(initialEntries: List<DiaryEntry> = emptyList()) : LocalDiaryRepository {

    private val _entries = MutableStateFlow(initialEntries.sortedBy { it.date })
    override val entries: Flow<List<DiaryEntry>> = _entries.asStateFlow()

    fun add(date: LocalDate, feeling: Feeling) {
        _entries.value = _entries.value.recording(date, feeling)
    }
}
