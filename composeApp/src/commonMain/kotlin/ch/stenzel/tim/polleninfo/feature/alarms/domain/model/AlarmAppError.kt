package ch.stenzel.tim.polleninfo.feature.alarms.domain.model

import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.result.toAppError

/**
 * The alarm failures as the user sees them; anything that is not one of them — a lost connection, a
 * server error — goes through the generic [toAppError]. An [UnknownDeviceException] only gets here
 * after the repository's one re-registration failed as well, which the user cannot act on, so it is
 * [AppError.Unknown].
 */
fun Throwable.toAlarmAppError(): AppError = when (this) {
    is PushUnavailableException -> AppError.PushUnavailable
    is InvalidAlarmException -> AppError.InvalidAlarm
    is AlarmLimitReachedException -> AppError.AlarmLimitReached
    is AlarmNotFoundException -> AppError.NotFound
    is UnknownDeviceException -> AppError.Unknown
    else -> toAppError()
}
