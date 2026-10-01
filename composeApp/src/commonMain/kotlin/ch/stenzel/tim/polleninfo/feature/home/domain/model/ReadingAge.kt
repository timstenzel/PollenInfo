package ch.stenzel.tim.polleninfo.feature.home.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * How current a reading is, already converted into the zone the user reads clocks in.
 *
 * Two cases, rendered differently: a [Fresh] reading is labelled with its time as a caption, a
 * [Stale] one with its date as a warning. The backend may serve its last known reading through an
 * upstream outage with no maximum age, so the [Stale] case is what keeps that resilience from
 * passing an old reading off as today's air — the two must not be separated.
 */
sealed interface ReadingAge {

    /** Taken less than [STALE_AFTER] ago; [localTime] is enough to place it. */
    data class Fresh(val localTime: LocalTime) : ReadingAge

    /** Taken [STALE_AFTER] ago or more; named by its [localDate], since its time alone would mislead. */
    data class Stale(val localDate: LocalDate) : ReadingAge
}

/** A reading this old or older is no longer presented as current. */
val STALE_AFTER: Duration = 3.hours

/**
 * Classifies [measuredAt] against [now] and converts it from the source's UTC into [timeZone].
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
    return if (now - measuredAt < STALE_AFTER) {
        ReadingAge.Fresh(local.time)
    } else {
        ReadingAge.Stale(local.date)
    }
}
