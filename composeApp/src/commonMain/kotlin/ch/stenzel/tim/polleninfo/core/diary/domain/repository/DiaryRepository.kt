package ch.stenzel.tim.polleninfo.core.diary.domain.repository

import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.datetime.LocalDate

/**
 * The logged-in account's diary, kept by the backend. Every call needs a session: without one it
 * fails with `SignedOutException` and sends nothing, and a `401` from the backend forgets the
 * session on this phone and fails the same way.
 *
 * "Today" is the backend's — the Swiss calendar day of its clock — so the phone never says which
 * date an answer belongs to.
 */
interface DiaryRepository {

    /**
     * Sends [feeling] as today's answer. The result is the answer the account holds for today: this
     * one, or — if today was answered already, on this phone or another — that first one, which is
     * never overwritten.
     */
    suspend fun record(feeling: Feeling): Result<DiaryEntry>

    /** Today's answer, or `null` if today has none yet. */
    suspend fun today(): Result<DiaryEntry?>

    /**
     * The answers from [from] to [until], both included, oldest first; a day without an answer is
     * absent. The backend reads at most 366 days at once.
     */
    suspend fun entries(from: LocalDate, until: LocalDate): Result<List<DiaryEntry>>
}
