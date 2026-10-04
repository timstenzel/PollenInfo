package ch.stenzel.tim.polleninfo.feature.alarms.data.mapper

import ch.stenzel.tim.polleninfo.core.measurement.data.mapper.toPollenSeverity
import ch.stenzel.tim.polleninfo.core.measurement.data.mapper.toWireName
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.AlarmDto
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.AlarmInputDto
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.ScheduleDto
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.Alarm
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmDraft
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmSchedule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.char

fun AlarmDto.toDomain() = Alarm(
    id = id,
    enabled = enabled,
    stationAbbr = stationAbbr,
    species = species.toSet(),
    minSeverity = minSeverity.toPollenSeverity(),
    days = days.map { it.toDayOfWeek() }.toSet(),
    schedule = schedule.toDomain(),
)

fun List<AlarmDto>.toDomain(): List<Alarm> = map { it.toDomain() }

/** The request body that would store [this] alarm as it is; its id travels in the path. */
fun Alarm.toInputDto() = AlarmDraft(
    enabled = enabled,
    stationAbbr = stationAbbr,
    species = species,
    minSeverity = minSeverity,
    days = days,
    schedule = schedule,
).toInputDto()

/** The body of a create. Sets are sent sorted, so the same alarm always reads the same on the wire. */
fun AlarmDraft.toInputDto() = AlarmInputDto(
    enabled = enabled,
    stationAbbr = stationAbbr,
    species = species.sorted(),
    minSeverity = minSeverity.toWireName(),
    days = days.sortedBy { it.ordinal }.map { it.name },
    schedule = when (val schedule = schedule) {
        is AlarmSchedule.Daily -> ScheduleDto.Daily(TIME_FORMAT.format(schedule.at))
        is AlarmSchedule.Threshold -> ScheduleDto.Threshold(
            from = TIME_FORMAT.format(schedule.from),
            until = TIME_FORMAT.format(schedule.until),
        )
    },
)

private fun ScheduleDto.toDomain(): AlarmSchedule = when (this) {
    is ScheduleDto.Daily -> AlarmSchedule.Daily(TIME_FORMAT.parse(at))
    is ScheduleDto.Threshold -> AlarmSchedule.Threshold(
        from = TIME_FORMAT.parse(from),
        until = TIME_FORMAT.parse(until),
    )
}

/** An unknown day throws, which the repository turns into a `Failure`, rather than being dropped. */
private fun String.toDayOfWeek(): DayOfWeek =
    DayOfWeek.entries.firstOrNull { it.name == this }
        ?: throw IllegalArgumentException("unknown day of week '$this'")

/** `HH:mm`, the backend's time format in both directions. */
private val TIME_FORMAT = LocalTime.Format {
    hour()
    char(':')
    minute()
}
