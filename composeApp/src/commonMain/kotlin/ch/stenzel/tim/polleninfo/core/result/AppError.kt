package ch.stenzel.tim.polleninfo.core.result

import ch.stenzel.tim.polleninfo.core.network.HttpStatusException
import kotlinx.io.IOException

/**
 * What went wrong, as far as the user is concerned. UI states carry this instead of a message, so no
 * exception text — host names, status codes, library messages — can reach the screen; the screen
 * turns it into a sentence in the app's language (`core/ui/error`).
 *
 * The first three come from [toAppError]; the next three are conditions the app itself finds; the
 * alarm kinds come from `feature/alarms`' own mapping and the account kinds from `core/session`'s,
 * both of which delegate everything else here.
 */
sealed interface AppError {
    /** No connection, DNS failure or timeout — the user's network is the likely cause. */
    data object Network : AppError

    /** The backend answered 5xx, including a 502 when it could not reach MeteoSwiss. */
    data object ServerUnavailable : AppError

    /** The backend answered 404 for something the screen asked for. */
    data object NotFound : AppError

    /** No home station is stored — unreachable past the startup gate, but never a spinner. */
    data object NoStationSelected : AppError

    /** A round of readings in which no station answered. */
    data object NoReadings : AppError

    /** The station list arrived empty, so there is nothing to choose from. */
    data object NoStations : AppError

    /** This platform cannot receive push notifications, so alarms cannot work here. */
    data object PushUnavailable : AppError

    /** The device already holds the most alarms the backend allows. */
    data object AlarmLimitReached : AppError

    /** The backend refused an alarm as invalid; its reason stays in the exception. */
    data object InvalidAlarm : AppError

    /** An account with this email address exists already. */
    data object EmailTaken : AppError

    /** The email or the password of a login is wrong; deliberately not which. */
    data object InvalidCredentials : AppError

    /** The backend no longer knows this phone's session, so the user has to log in again. */
    data object SignedOut : AppError

    data object Unknown : AppError
}

/**
 * The generic part of the mapping: transport failures and timeouts (all `IOException`s on every
 * engine, `HttpRequestTimeoutException` included) are [AppError.Network], a 5xx is
 * [AppError.ServerUnavailable], a 404 [AppError.NotFound], and anything else — a payload that does
 * not parse, an unexpected status — [AppError.Unknown].
 */
fun Throwable.toAppError(): AppError = when (this) {
    is HttpStatusException -> when (status) {
        404 -> AppError.NotFound
        in 500..599 -> AppError.ServerUnavailable
        else -> AppError.Unknown
    }
    is IOException -> AppError.Network
    else -> AppError.Unknown
}
