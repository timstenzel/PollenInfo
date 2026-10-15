package ch.stenzel.tim.polleninfo.core.diary.domain.repository

import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Whether Home's "How do you feel today?" was closed unanswered — a choice about this phone's
 * prompt, not an answer, so it stays on the device whether or not anyone is logged in.
 */
interface FeelingPromptPreferences {

    /** The last Swiss date the prompt was closed without answering; `null` if never. */
    val dismissedOn: Flow<LocalDate?>

    /** Closes the prompt for [date]. */
    suspend fun dismiss(date: LocalDate): Result<Unit>
}
