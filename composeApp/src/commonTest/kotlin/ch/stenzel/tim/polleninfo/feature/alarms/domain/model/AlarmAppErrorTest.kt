package ch.stenzel.tim.polleninfo.feature.alarms.domain.model

import ch.stenzel.tim.polleninfo.core.network.HttpStatusException
import ch.stenzel.tim.polleninfo.core.result.AppError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.io.IOException

class AlarmAppErrorTest {

    @Test
    fun `PushUnavailableException is PushUnavailable`() {
        assertEquals(AppError.PushUnavailable, PushUnavailableException().toAlarmAppError())
    }

    @Test
    fun `InvalidAlarmException is InvalidAlarm`() {
        assertEquals(AppError.InvalidAlarm, InvalidAlarmException("Select at least one day").toAlarmAppError())
    }

    @Test
    fun `AlarmLimitReachedException is AlarmLimitReached`() {
        assertEquals(AppError.AlarmLimitReached, AlarmLimitReachedException().toAlarmAppError())
    }

    @Test
    fun `AlarmNotFoundException is NotFound`() {
        assertEquals(AppError.NotFound, AlarmNotFoundException().toAlarmAppError())
    }

    @Test
    fun `UnknownDeviceException after the retry is Unknown`() {
        assertEquals(AppError.Unknown, UnknownDeviceException().toAlarmAppError())
    }

    @Test
    fun `any other exception is delegated to the generic mapping`() {
        assertEquals(AppError.Network, IOException("offline").toAlarmAppError())
        assertEquals(AppError.ServerUnavailable, HttpStatusException(502).toAlarmAppError())
        assertEquals(AppError.NotFound, HttpStatusException(404).toAlarmAppError())
        assertEquals(AppError.Unknown, RuntimeException().toAlarmAppError())
    }
}
