package ch.stenzel.tim.polleninfo.server.alarm.domain

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** An anonymous app install. Whoever holds the value can read and change its alarms. */
@JvmInline
value class DeviceId(val value: String)

@JvmInline
value class AlarmId(val value: String)

/**
 * One pollen alarm: a station watched on [days], either as a report at a fixed time or as an alert
 * during a window. All times are Swiss local time.
 *
 * [species] and [days] are never empty, and [minSeverity] is `NONE` ("Any") only for a
 * [AlarmSchedule.Daily] report; the validation that guarantees it arrives with alarm creation.
 */
data class Alarm(
    val id: AlarmId,
    val deviceId: DeviceId,
    val enabled: Boolean,
    val station: PollenStation,
    val species: Set<PollenSpecies>,
    val minSeverity: PollenSeverity,
    val days: Set<DayOfWeek>,
    val schedule: AlarmSchedule,
)

sealed interface AlarmSchedule {

    /** A summary sent once, at [at], on each selected day. */
    data class Daily(val at: LocalTime) : AlarmSchedule

    /** Alerts from [from] (inclusive) until [until] (exclusive); [until] is after [from]. */
    data class Threshold(val from: LocalTime, val until: LocalTime) : AlarmSchedule
}

/** How alarm times travel and are stored: `HH:mm`, Swiss local time. */
val ALARM_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
