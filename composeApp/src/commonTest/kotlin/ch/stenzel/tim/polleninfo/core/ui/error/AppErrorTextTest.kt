package ch.stenzel.tim.polleninfo.core.ui.error

import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.error_action_delete_alarm
import ch.stenzel.tim.polleninfo.resources.error_action_pause_alarm
import ch.stenzel.tim.polleninfo.resources.error_action_resume_alarm
import ch.stenzel.tim.polleninfo.resources.error_action_save_alarm
import ch.stenzel.tim.polleninfo.resources.error_alarm_limit_reached
import ch.stenzel.tim.polleninfo.resources.error_alarm_not_found
import ch.stenzel.tim.polleninfo.resources.error_network
import ch.stenzel.tim.polleninfo.resources.error_not_found
import ch.stenzel.tim.polleninfo.resources.error_server_unavailable
import ch.stenzel.tim.polleninfo.resources.error_unknown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppErrorTextTest {

    private val allErrors = listOf(
        AppError.Network,
        AppError.ServerUnavailable,
        AppError.NotFound,
        AppError.NoStationSelected,
        AppError.NoReadings,
        AppError.NoStations,
        AppError.PushUnavailable,
        AppError.AlarmLimitReached,
        AppError.InvalidAlarm,
        AppError.Unknown,
    )

    @Test
    fun `every kind has its own reason while loading`() {
        val reasons = allErrors.map { it.text(ErrorContext.LOAD).reason }

        assertEquals(allErrors.size, reasons.toSet().size)
    }

    @Test
    fun `loading has no action sentence`() {
        allErrors.forEach { assertNull(it.text(ErrorContext.LOAD).action, "$it") }
    }

    @Test
    fun `no connection and a server problem read differently`() {
        assertEquals(Res.string.error_network, AppError.Network.text(ErrorContext.LOAD).reason)
        assertEquals(Res.string.error_server_unavailable, AppError.ServerUnavailable.text(ErrorContext.LOAD).reason)
    }

    @Test
    fun `each alarm action names what failed before the reason`() {
        assertEquals(
            ErrorText(Res.string.error_action_save_alarm, Res.string.error_alarm_limit_reached),
            AppError.AlarmLimitReached.text(ErrorContext.SAVE_ALARM),
        )
        assertEquals(
            ErrorText(Res.string.error_action_delete_alarm, Res.string.error_network),
            AppError.Network.text(ErrorContext.DELETE_ALARM),
        )
        assertEquals(
            ErrorText(Res.string.error_action_pause_alarm, Res.string.error_unknown),
            AppError.Unknown.text(ErrorContext.PAUSE_ALARM),
        )
        assertEquals(
            ErrorText(Res.string.error_action_resume_alarm, Res.string.error_server_unavailable),
            AppError.ServerUnavailable.text(ErrorContext.RESUME_ALARM),
        )
    }

    @Test
    fun `NotFound is the alarm itself outside loading`() {
        assertEquals(Res.string.error_not_found, AppError.NotFound.text(ErrorContext.LOAD).reason)
        assertEquals(Res.string.error_alarm_not_found, AppError.NotFound.text(ErrorContext.SAVE_ALARM).reason)
        assertEquals(Res.string.error_alarm_not_found, AppError.NotFound.text(ErrorContext.DELETE_ALARM).reason)
    }
}
