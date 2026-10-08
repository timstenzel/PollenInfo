package ch.stenzel.tim.polleninfo.server.alarm.domain

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.measurement.CacheResult
import ch.stenzel.tim.polleninfo.server.pollen.measurement.SpeciesMeasurement
import ch.stenzel.tim.polleninfo.server.pollen.measurement.StationMeasurement
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * A reading this old or older is not current. It must stay equal to the app's `STALE_AFTER`
 * (`core/measurement/domain/model/ReadingAge.kt`): a notification that calls a reading current while
 * the app warns that the same reading is not would be two answers to one question.
 */
val READING_STALE_AFTER: Duration = Duration.ofHours(3)

/** A threshold alert to send, and the pollen types to record as notified once it is delivered. */
data class ThresholdOutcome(val message: PushMessage, val species: Set<PollenSpecies>)

/**
 * When an alarm is due and what it says. Pure — no clock, no I/O, no logging — so every rule is
 * tested with an explicit `now`. Times are compared in [ALARM_ZONE] whatever zone `now` comes in.
 */
object AlarmRules {

    /** True when [alarm] is an enabled daily report whose day and minute [now] is. */
    fun isDailyDue(alarm: Alarm, now: ZonedDateTime): Boolean {
        val schedule = alarm.schedule as? AlarmSchedule.Daily ?: return false
        val local = now.withZoneSameInstant(ALARM_ZONE)
        return alarm.enabled &&
            local.dayOfWeek in alarm.days &&
            local.toLocalTime().truncatedTo(ChronoUnit.MINUTES) == schedule.at
    }

    /**
     * The daily report to send at [now], or `null` when there is none.
     *
     * - Not due ([isDailyDue]) → `null`.
     * - A current reading → sent when "Any" is chosen or at least one selected type has reached the
     *   minimum; `null` otherwise.
     * - No current reading (an old one, or none at all) → for "Any", a message saying so and when the
     *   latest reading is from; `null` with a minimum, which must not be judged on old data.
     *
     * The message says what happened ([PushKind]); the app words it in its own language.
     */
    fun evaluateDaily(
        alarm: Alarm,
        now: ZonedDateTime,
        reading: CacheResult<StationMeasurement>,
    ): PushMessage? {
        if (!isDailyDue(alarm, now)) return null
        val local = now.withZoneSameInstant(ALARM_ZONE)
        val measurement = when (reading) {
            is CacheResult.Fresh -> reading.value
            is CacheResult.Stale -> reading.value
            is CacheResult.Failed -> null
        }
        val anySeverity = alarm.minSeverity == PollenSeverity.NONE

        if (measurement == null || !isCurrent(measurement.measuredAt, local)) {
            if (!anySeverity) return null
            return if (measurement == null) {
                message(alarm, PushKind.UNAVAILABLE, PushChannel.DAILY_REPORT)
            } else {
                message(alarm, PushKind.NO_CURRENT_READING, PushChannel.DAILY_REPORT, measuredAt = measurement.measuredAt)
            }
        }

        val selected = measurement.species.filter { it.species in alarm.species && it.severity != null }
        if (!anySeverity && selected.none { it.severity!!.atLeast(alarm.minSeverity) }) return null
        return when {
            selected.isEmpty() -> message(alarm, PushKind.NOT_REPORTED, PushChannel.DAILY_REPORT)
            selected.all { it.severity == PollenSeverity.NONE } ->
                message(alarm, PushKind.NO_POLLEN, PushChannel.DAILY_REPORT)
            else -> message(alarm, PushKind.REPORT, PushChannel.DAILY_REPORT, levels = levels(selected))
        }
    }

    /**
     * True when [alarm] is an enabled threshold alert whose day [now] is and whose window [now] is
     * in — from `from` (inclusive) until `until` (exclusive).
     */
    fun isThresholdActive(alarm: Alarm, now: ZonedDateTime): Boolean {
        val schedule = alarm.schedule as? AlarmSchedule.Threshold ?: return false
        val local = now.withZoneSameInstant(ALARM_ZONE)
        val time = local.toLocalTime()
        return alarm.enabled &&
            local.dayOfWeek in alarm.days &&
            !time.isBefore(schedule.from) &&
            time.isBefore(schedule.until)
    }

    /**
     * The threshold alert to send at [now], or `null` when there is none.
     *
     * - Not active ([isThresholdActive]), or no current [reading] ([isCurrent]) → `null`: an alert
     *   is never raised on old data, nor on none.
     * - Otherwise every selected type with a reading at or above the minimum that is not in
     *   [notifiedToday] qualifies, and all of them go into one message. None qualifies → `null`.
     *
     * The caller records [ThresholdOutcome.species] once the message is delivered, which is what
     * makes it at most once per type per day. A type that was already over the threshold before the
     * window opened therefore notifies at the first evaluation inside it.
     */
    fun evaluateThreshold(
        alarm: Alarm,
        now: ZonedDateTime,
        reading: StationMeasurement?,
        notifiedToday: Set<PollenSpecies>,
    ): ThresholdOutcome? {
        if (!isThresholdActive(alarm, now)) return null
        if (reading == null || !isCurrent(reading.measuredAt, now)) return null
        val qualifying = reading.species.filter {
            it.species in alarm.species &&
                it.species !in notifiedToday &&
                it.severity?.atLeast(alarm.minSeverity) == true
        }
        if (qualifying.isEmpty()) return null
        return ThresholdOutcome(
            message = message(alarm, PushKind.ALERT, PushChannel.THRESHOLD_ALERT, levels = levels(qualifying)),
            species = qualifying.map { it.species }.toSet(),
        )
    }

    /**
     * Current means younger than [READING_STALE_AFTER] **and** from today in [ALARM_ZONE], so a
     * reading from shortly before midnight never stands in for today's.
     */
    fun isCurrent(measuredAt: Instant, now: ZonedDateTime): Boolean =
        Duration.between(measuredAt, now.toInstant()) < READING_STALE_AFTER &&
            measuredAt.atZone(ALARM_ZONE).toLocalDate() == now.withZoneSameInstant(ALARM_ZONE).toLocalDate()

    /**
     * Worst first, then in [PollenSpecies] declaration order — the order the app's
     * `GetStationMeasurementUseCase` displays a station in. Every entry has a severity.
     */
    private fun levels(readings: List<SpeciesMeasurement>): List<Pair<PollenSpecies, PollenSeverity>> = readings
        .sortedWith(compareByDescending<SpeciesMeasurement> { it.severity }.thenBy { it.species.ordinal })
        .map { it.species to it.severity!! }

    private fun message(
        alarm: Alarm,
        kind: PushKind,
        channel: PushChannel,
        levels: List<Pair<PollenSpecies, PollenSeverity>> = emptyList(),
        measuredAt: Instant? = null,
    ) = PushMessage(
        kind = kind,
        channel = channel,
        station = alarm.station,
        alarmId = alarm.id,
        levels = levels,
        measuredAt = measuredAt,
    )
}
