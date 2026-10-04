package ch.stenzel.tim.polleninfo.core.history.data.mapper

import ch.stenzel.tim.polleninfo.core.history.data.remote.dto.HistoryDayDto
import ch.stenzel.tim.polleninfo.core.history.data.remote.dto.StationHistoryDto
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryDay
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.core.history.domain.model.StationHistory
import ch.stenzel.tim.polleninfo.core.measurement.data.mapper.toPollenSeverity
import kotlinx.datetime.LocalDate

// A malformed date or an unknown severity throws, which the repository turns into a `Failure`.
fun StationHistoryDto.toDomain(): StationHistory = StationHistory(
    stationAbbr = stationAbbr,
    from = LocalDate.parse(from),
    until = LocalDate.parse(until),
    days = days.map { it.toDomain() },
)

fun HistoryDayDto.toDomain(): HistoryDay = HistoryDay(
    date = LocalDate.parse(date),
    levels = species.associate { it.id to it.severity?.toPollenSeverity() },
)

/** The `range` query value. Spelled out, like the severity names, so a rename cannot change it. */
internal fun HistoryRange.toWireName(): String = when (this) {
    HistoryRange.WEEK -> "week"
    HistoryRange.MONTH -> "month"
    HistoryRange.YEAR -> "year"
}
