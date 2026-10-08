package ch.stenzel.tim.polleninfo.core.ui.error

import androidx.compose.runtime.Composable
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.error_action_delete_alarm
import ch.stenzel.tim.polleninfo.resources.error_action_pause_alarm
import ch.stenzel.tim.polleninfo.resources.error_action_resume_alarm
import ch.stenzel.tim.polleninfo.resources.error_action_save_alarm
import ch.stenzel.tim.polleninfo.resources.error_alarm_limit_reached
import ch.stenzel.tim.polleninfo.resources.error_alarm_not_found
import ch.stenzel.tim.polleninfo.resources.error_invalid_alarm
import ch.stenzel.tim.polleninfo.resources.error_network
import ch.stenzel.tim.polleninfo.resources.error_no_readings
import ch.stenzel.tim.polleninfo.resources.error_no_station_selected
import ch.stenzel.tim.polleninfo.resources.error_no_stations
import ch.stenzel.tim.polleninfo.resources.error_not_found
import ch.stenzel.tim.polleninfo.resources.error_push_unavailable
import ch.stenzel.tim.polleninfo.resources.error_server_unavailable
import ch.stenzel.tim.polleninfo.resources.error_unknown
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** What the user was doing when the error happened; it picks the wording around the reason. */
enum class ErrorContext { LOAD, SAVE_ALARM, DELETE_ALARM, PAUSE_ALARM, RESUME_ALARM }

/**
 * The sentences an error is shown as: [action] says what failed ("Couldn't save the alarm.") and is
 * `null` while loading, where the screen's own heading already says so; [reason] says why.
 */
data class ErrorText(val action: StringResource?, val reason: StringResource)

/** Pure, so the choice of wording is tested without composing anything. */
fun AppError.text(context: ErrorContext): ErrorText {
    val reason = when (this) {
        AppError.Network -> Res.string.error_network
        AppError.ServerUnavailable -> Res.string.error_server_unavailable
        // Outside loading, the only thing an alarm action can fail to find is the alarm itself.
        AppError.NotFound ->
            if (context == ErrorContext.LOAD) Res.string.error_not_found else Res.string.error_alarm_not_found
        AppError.NoStationSelected -> Res.string.error_no_station_selected
        AppError.NoReadings -> Res.string.error_no_readings
        AppError.NoStations -> Res.string.error_no_stations
        AppError.PushUnavailable -> Res.string.error_push_unavailable
        AppError.AlarmLimitReached -> Res.string.error_alarm_limit_reached
        AppError.InvalidAlarm -> Res.string.error_invalid_alarm
        AppError.Unknown -> Res.string.error_unknown
    }
    val action = when (context) {
        ErrorContext.LOAD -> null
        ErrorContext.SAVE_ALARM -> Res.string.error_action_save_alarm
        ErrorContext.DELETE_ALARM -> Res.string.error_action_delete_alarm
        ErrorContext.PAUSE_ALARM -> Res.string.error_action_pause_alarm
        ErrorContext.RESUME_ALARM -> Res.string.error_action_resume_alarm
    }
    return ErrorText(action, reason)
}

/** The error as one or two sentences in the app's language — never exception text. */
@Composable
fun AppError.message(context: ErrorContext = ErrorContext.LOAD): String {
    val text = text(context)
    val reason = stringResource(text.reason)
    return text.action?.let { "${stringResource(it)} $reason" } ?: reason
}

/** [message] for code outside composition, such as a snackbar shown from an event. */
suspend fun AppError.loadMessage(context: ErrorContext = ErrorContext.LOAD): String {
    val text = text(context)
    val reason = getString(text.reason)
    return text.action?.let { "${getString(it)} $reason" } ?: reason
}
