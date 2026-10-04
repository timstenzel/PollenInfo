package ch.stenzel.tim.polleninfo.core.diary

import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.diary.domain.model.recording
import ch.stenzel.tim.polleninfo.core.diary.domain.repository.DiaryRepository
import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate

/**
 * In-memory stand-in for `DataStoreDiaryRepository`, with the same never-overwrite rule.
 *
 * [failWrite] makes [record] and [dismiss] fail without storing anything.
 */
class FakeDiaryRepository(
    initialEntries: List<DiaryEntry> = emptyList(),
    initialDismissedOn: LocalDate? = null,
    var failWrite: Boolean = false,
) : DiaryRepository {

    private val _entries = MutableStateFlow(initialEntries.sortedBy { it.date })
    override val entries: Flow<List<DiaryEntry>> = _entries.asStateFlow()

    private val _dismissedOn = MutableStateFlow(initialDismissedOn)
    override val dismissedOn: Flow<LocalDate?> = _dismissedOn.asStateFlow()

    override suspend fun record(date: LocalDate, feeling: Feeling): Result<Unit> {
        if (failWrite) return Result.Failure(RuntimeException("disk full"))
        _entries.value = _entries.value.recording(date, feeling)
        return Result.Success(Unit)
    }

    override suspend fun dismiss(date: LocalDate): Result<Unit> {
        if (failWrite) return Result.Failure(RuntimeException("disk full"))
        _dismissedOn.value = date
        return Result.Success(Unit)
    }

    /** The currently stored values, for assertions that do not want to collect the flows. */
    val storedEntries: List<DiaryEntry> get() = _entries.value
    val storedDismissedOn: LocalDate? get() = _dismissedOn.value
}
