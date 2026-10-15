package ch.stenzel.tim.polleninfo.core.session.domain.model

import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.result.toAppError

/** An account with this email exists already (`409` on registration). */
class EmailTakenException : Exception("An account with this email exists already")

/**
 * The account failures as the user sees them; anything else — a lost connection, a server error —
 * goes through the generic [toAppError].
 */
fun Throwable.toSessionAppError(): AppError = when (this) {
    is EmailTakenException -> AppError.EmailTaken
    else -> toAppError()
}
