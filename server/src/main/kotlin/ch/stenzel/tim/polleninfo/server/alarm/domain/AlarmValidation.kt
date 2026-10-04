package ch.stenzel.tim.polleninfo.server.alarm.domain

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/**
 * An alarm as a client submitted it, before anything has been checked. Everything is a plain
 * string so that [AlarmValidation] — not the JSON decoder — decides what is wrong, and can say so.
 */
data class AlarmInput(
    val enabled: Boolean,
    val stationAbbr: String,
    val species: List<String>,
    val minSeverity: String,
    val days: List<String>,
    val schedule: ScheduleInput,
)

sealed interface ScheduleInput {
    data class Daily(val at: String) : ScheduleInput
    data class Threshold(val from: String, val until: String) : ScheduleInput
}

/** A checked alarm without its identity: everything [Alarm] holds except its id and its device. */
data class AlarmSpec(
    val enabled: Boolean,
    val station: PollenStation,
    val species: Set<PollenSpecies>,
    val minSeverity: PollenSeverity,
    val days: Set<DayOfWeek>,
    val schedule: AlarmSchedule,
) {
    fun toAlarm(id: AlarmId, deviceId: DeviceId) = Alarm(
        id = id,
        deviceId = deviceId,
        enabled = enabled,
        station = station,
        species = species,
        minSeverity = minSeverity,
        days = days,
        schedule = schedule,
    )
}

sealed interface ValidationResult {
    data class Valid(val spec: AlarmSpec) : ValidationResult

    /** [message] is meant for the `400` body, so it names the field that is wrong. */
    data class Invalid(val message: String) : ValidationResult
}

/**
 * The rules every stored alarm satisfies, applied to whatever a client sends. Pure: the routes call
 * it before touching the store, so nothing invalid is ever written.
 */
object AlarmValidation {

    fun validate(input: AlarmInput): ValidationResult {
        val station = PollenStation.fromAbbr(input.stationAbbr)
            ?: return invalid("Unknown station '${input.stationAbbr}'")

        if (input.species.isEmpty()) return invalid("Select at least one pollen type")
        val species = input.species.map { name ->
            enumValueOrNull<PollenSpecies>(name) ?: return invalid("Unknown pollen type '$name'")
        }.toSet()

        val minSeverity = enumValueOrNull<PollenSeverity>(input.minSeverity)
            ?: return invalid("Unknown severity '${input.minSeverity}'")

        if (input.days.isEmpty()) return invalid("Select at least one day")
        val days = input.days.map { name ->
            enumValueOrNull<DayOfWeek>(name) ?: return invalid("Unknown day '$name'")
        }.toSet()

        val schedule = when (val schedule = input.schedule) {
            is ScheduleInput.Daily -> AlarmSchedule.Daily(
                at = parseTime(schedule.at) ?: return invalid("Time must be HH:mm, got '${schedule.at}'"),
            )
            // Threshold alerts arrive with their own rules (a window, no "Any"); until then none can
            // be created rather than one being stored unchecked.
            is ScheduleInput.Threshold -> return invalid("Threshold alerts cannot be created yet")
        }

        return ValidationResult.Valid(
            AlarmSpec(
                enabled = input.enabled,
                station = station,
                species = species,
                minSeverity = minSeverity,
                days = days,
                schedule = schedule,
            ),
        )
    }

    /**
     * Exactly `HH:mm`: `8:00`, `08:00:00` and `24:00` are all refused. Strict, because the default
     * resolver quietly reads `24:00` as midnight.
     */
    private fun parseTime(value: String): LocalTime? = try {
        LocalTime.parse(value, STRICT_TIME_FORMAT)
    } catch (e: DateTimeParseException) {
        null
    }

    private val STRICT_TIME_FORMAT = ALARM_TIME_FORMAT.withResolverStyle(ResolverStyle.STRICT)

    private fun invalid(message: String) = ValidationResult.Invalid(message)

    private inline fun <reified E : Enum<E>> enumValueOrNull(name: String): E? =
        enumValues<E>().firstOrNull { it.name == name }
}
