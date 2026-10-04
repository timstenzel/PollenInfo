package ch.stenzel.tim.polleninfo.core.history.domain.model

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import kotlinx.datetime.LocalDate

/**
 * A station's daily pollen levels from [from] to [until] (both inclusive, Swiss dates), as the
 * backend classified them.
 *
 * [days] has one entry per date, oldest first, with no date left out — the backend fills a day the
 * publisher has no value for, so position and date always agree.
 */
data class StationHistory(
    val stationAbbr: String,
    val from: LocalDate,
    val until: LocalDate,
    val days: List<HistoryDay>,
)

/**
 * One day of a history. [levels] is keyed by species id and holds every species the backend
 * reports; a `null` level means the station has no value for it that day — not `NONE`, which is a
 * measured day without pollen.
 */
data class HistoryDay(
    val date: LocalDate,
    val levels: Map<String, PollenSeverity?>,
)
