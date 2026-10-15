package ch.stenzel.tim.polleninfo.core.session.domain.model

import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.result.toAppError

/** An account with this email exists already (`409` on registration). */
class EmailTakenException : Exception("An account with this email exists already")

/** The email or the password is wrong (`401` on login) — the backend never says which. */
class InvalidCredentialsException : Exception("Email or password is incorrect")

/**
 * The backend does not know this phone's session any more (`401` on an authenticated call): it
 * expired after 90 days unused, or was ended elsewhere. The session is cleared on this phone.
 */
class SignedOutException : Exception("The session is no longer valid")

/**
 * The account failures as the user sees them; anything else — a lost connection, a server error —
 * goes through the generic [toAppError].
 */
fun Throwable.toSessionAppError(): AppError = when (this) {
    is EmailTakenException -> AppError.EmailTaken
    is InvalidCredentialsException -> AppError.InvalidCredentials
    is SignedOutException -> AppError.SignedOut
    else -> toAppError()
}
