package ch.stenzel.tim.polleninfo.core.diary.domain.repository

import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * The user's diary answers, kept **on the device only** — no API service takes them, so health
 * information never leaves the phone. Nothing is ever pruned.
 */
interface DiaryRepository {

    /** Every recorded answer, sorted by date. */
    val entries: Flow<List<DiaryEntry>>

    /** The last date the Home prompt was closed without answering; `null` if never. */
    val dismissedOn: Flow<LocalDate?>

    /** Records [feeling] for [date]; a date that already has an answer is left as it is. */
    suspend fun record(date: LocalDate, feeling: Feeling): Result<Unit>

    /** Closes the Home prompt for [date] without recording anything. */
    suspend fun dismiss(date: LocalDate): Result<Unit>
}
