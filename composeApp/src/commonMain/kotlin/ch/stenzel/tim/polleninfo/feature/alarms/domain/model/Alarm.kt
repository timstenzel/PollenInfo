package ch.stenzel.tim.polleninfo.feature.alarms.domain.model

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

/**
 * One of this device's pollen alarms, as the backend stores it. Every time is Swiss local time.
 *
 * [species] holds the backend's species ids (`"BIRCH"`, …) — the vocabulary the app loads from
 * `GET /pollen/species` — and [stationAbbr] a station abbreviation. A [minSeverity] of
 * [PollenSeverity.NONE] is "Any": a daily report sent whatever the levels.
 */
data class Alarm(
    val id: String,
    val enabled: Boolean,
    val stationAbbr: String,
    val species: Set<String>,
    val minSeverity: PollenSeverity,
    val days: Set<DayOfWeek>,
    val schedule: AlarmSchedule,
) {
    /** Everything but the id: what an update sends to store this alarm as it is. */
    fun toDraft() = AlarmDraft(
        enabled = enabled,
        stationAbbr = stationAbbr,
        species = species,
        minSeverity = minSeverity,
        days = days,
        schedule = schedule,
    )
}

sealed interface AlarmSchedule {

    /** A summary sent once, at [at], on each selected day. */
    data class Daily(val at: LocalTime) : AlarmSchedule

    /** Alerts from [from] until [until] on each selected day. */
    data class Threshold(val from: LocalTime, val until: LocalTime) : AlarmSchedule
}
