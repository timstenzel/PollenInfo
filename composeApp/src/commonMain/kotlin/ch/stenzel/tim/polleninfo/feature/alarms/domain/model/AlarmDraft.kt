package ch.stenzel.tim.polleninfo.feature.alarms.domain.model

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import kotlinx.datetime.DayOfWeek

/**
 * An alarm as the user configured it, before the backend has stored it: an [Alarm] without its id.
 * What a create sends, and later what an update sends.
 */
data class AlarmDraft(
    val enabled: Boolean,
    val stationAbbr: String,
    val species: Set<String>,
    val minSeverity: PollenSeverity,
    val days: Set<DayOfWeek>,
    val schedule: AlarmSchedule,
)
