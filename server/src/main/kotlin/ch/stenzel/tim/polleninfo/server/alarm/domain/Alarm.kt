package ch.stenzel.tim.polleninfo.server.alarm.domain

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import ch.stenzel.tim.polleninfo.server.pollen.domain.SWISS_ZONE
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

/**
 * The one zone every alarm's days and times are in: Swiss time. Every "which day is it" and "what
 * time is it" question about an alarm goes through this. An alias of [SWISS_ZONE], so alarms and
 * the pollen history can never disagree about the date.
 */
val ALARM_ZONE: ZoneId = SWISS_ZONE
