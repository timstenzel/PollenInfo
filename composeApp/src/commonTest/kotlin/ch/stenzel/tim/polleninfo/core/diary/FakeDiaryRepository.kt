package ch.stenzel.tim.polleninfo.core.diary

import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.diary.domain.repository.DiaryRepository
import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.LocalDate

/**
 * In-memory stand-in for the account's diary, with the backend's rules: [today] is the server's
 * day, and the first answer for it is final — a second [record] returns the first. [entries] reads
 * [stored], which a test can script with answers on earlier days.
 *
 * [recordFailure], [todayFailure] and [entriesFailure] fail the call (and, if [onFailure] is set,
 * run it first, as the real repository clears the session on a `401`). [recordGate], [todayGate]
 * and [entriesGate] hold a call in flight.
 */
class FakeDiaryRepository(
    var today: LocalDate,
    initialToday: Feeling? = null,
    initialEntries: List<DiaryEntry> = emptyList(),
) : DiaryRepository {

    /** The account's answers by date. */
    val stored = mutableMapOf<LocalDate, Feeling>().apply {
        initialEntries.forEach { put(it.date, it.feeling) }
        initialToday?.let { put(today, it) }
    }

    var recordFailure: Exception? = null
    var todayFailure: Exception? = null
    var entriesFailure: Exception? = null
    var onFailure: (suspend () -> Unit)? = null
    var recordGate: CompletableDeferred<Unit>? = null
    var todayGate: CompletableDeferred<Unit>? = null
    var entriesGate: CompletableDeferred<Unit>? = null

    /** Every range [entries] was asked for, in order, as `from..until`. */
    val entriesRequests = mutableListOf<ClosedRange<LocalDate>>()

    /** Every answer sent, in order, whether or not it was stored. */
    val recorded = mutableListOf<Feeling>()

    /** How often [today] was asked. */
    var todayCalls = 0
        private set

    override suspend fun record(feeling: Feeling): Result<DiaryEntry> {
        recorded += feeling
        recordGate?.await()
        recordFailure?.let { onFailure?.invoke(); return Result.Failure(it) }
        val kept = stored.getOrPut(today) { feeling }
        return Result.Success(DiaryEntry(today, kept))
    }

    override suspend fun today(): Result<DiaryEntry?> {
        todayCalls++
        todayGate?.await()
        todayFailure?.let { onFailure?.invoke(); return Result.Failure(it) }
        return Result.Success(stored[today]?.let { DiaryEntry(today, it) })
    }

    override suspend fun entries(from: LocalDate, until: LocalDate): Result<List<DiaryEntry>> {
        entriesRequests += from..until
        entriesGate?.await()
        entriesFailure?.let { onFailure?.invoke(); return Result.Failure(it) }
        return Result.Success(
            stored.filterKeys { it in from..until }.map { (date, feeling) -> DiaryEntry(date, feeling) }.sortedBy { it.date },
        )
    }
}
