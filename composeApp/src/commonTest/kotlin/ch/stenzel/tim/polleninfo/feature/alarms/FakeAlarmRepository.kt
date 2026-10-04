package ch.stenzel.tim.polleninfo.feature.alarms

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.Alarm
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmSchedule
import ch.stenzel.tim.polleninfo.feature.alarms.domain.repository.AlarmRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

/**
 * While [gate] is set, every call suspends until it is completed — how a test holds a load in
 * flight to observe the state the screen shows meanwhile. [result] is read after the gate opens.
 */
class FakeAlarmRepository(
    var result: Result<List<Alarm>> = Result.Success(emptyList()),
    var gate: CompletableDeferred<Unit>? = null,
) : AlarmRepository {

    var callCount: Int = 0
        private set

    override suspend fun alarms(): Result<List<Alarm>> {
        callCount++
        gate?.await()
        return result
    }
}

val WEEKDAYS = setOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
)

fun dailyAlarm(
    id: String = "daily-1",
    stationAbbr: String = "PZH",
    at: LocalTime = LocalTime(8, 0),
) = Alarm(
    id = id,
    enabled = true,
    stationAbbr = stationAbbr,
    species = setOf("BIRCH", "GRASSES"),
    minSeverity = PollenSeverity.NONE,
    days = WEEKDAYS,
    schedule = AlarmSchedule.Daily(at),
)

fun thresholdAlarm(
    id: String = "threshold-1",
    stationAbbr: String = "PBE",
) = Alarm(
    id = id,
    enabled = false,
    stationAbbr = stationAbbr,
    species = setOf("HAZEL"),
    minSeverity = PollenSeverity.HIGH,
    days = DayOfWeek.entries.toSet(),
    schedule = AlarmSchedule.Threshold(from = LocalTime(7, 0), until = LocalTime(21, 0)),
)
