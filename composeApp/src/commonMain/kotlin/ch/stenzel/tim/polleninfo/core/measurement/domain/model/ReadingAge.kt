package ch.stenzel.tim.polleninfo.core.measurement.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * How current a reading is, already converted into the zone the user reads clocks in.
 *
 * Two cases, rendered differently: a [Fresh] reading is labelled with its time as a caption, a
 * [Stale] one as a warning. The backend may serve its last known reading through an
 * upstream outage with no maximum age, so the [Stale] case is what keeps that resilience from
 * passing an old reading off as today's air — the two must not be separated.
 */
sealed interface ReadingAge {

    /** Taken less than [STALE_AFTER] ago; [localTime] is enough to place it. */
    data class Fresh(val localTime: LocalTime) : ReadingAge

    /**
     * Taken [STALE_AFTER] ago or more. Named by whatever places it unambiguously: its time if it is
     * from today — a warning saying "data from 1 October" on 1 October reads as a contradiction —
     * otherwise its date, since a time alone would pass an old reading off as today's.
     */
    sealed interface Stale : ReadingAge {

        /** Stale, but from the same local calendar day as now. */
        data class Today(val localTime: LocalTime) : Stale

        /** From an earlier local calendar day. */
        data class Earlier(val localDate: LocalDate) : Stale
    }
}

/** A reading this old or older is no longer presented as current. */
val STALE_AFTER: Duration = 3.hours

/**
 * Classifies [measuredAt] against [now] and converts it from the source's UTC into [timeZone].
 *
 * Whether a stale reading is from "today" is decided by calendar day in [timeZone], not by
 * elapsed time: a reading from 23:00 is from yesterday at 01:00 even though only two hours apart.
 *
 * The boundary is exclusive for [ReadingAge.Fresh]: exactly [STALE_AFTER] is already stale. A
 * timestamp slightly in the future — a device clock running behind the server's — counts as fresh
 * rather than as an error, since the reading itself is fine.
 *
 * [now] and [timeZone] are parameters rather than read here so the boundary and the conversion can
 * be tested without a clock, a ViewModel or a composable.
 */
fun readingAgeOf(
    measuredAt: Instant,
    now: Instant,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): ReadingAge {
    val local = measuredAt.toLocalDateTime(timeZone)
    return when {
        now - measuredAt < STALE_AFTER -> ReadingAge.Fresh(local.time)
        local.date == now.toLocalDateTime(timeZone).date -> ReadingAge.Stale.Today(local.time)
        else -> ReadingAge.Stale.Earlier(local.date)
    }
}
