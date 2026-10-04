package ch.stenzel.tim.polleninfo.core.history.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire shape of `GET /pollen/stations/{abbr}/history?range=…`, mirroring the server's own DTO.
 * Dates are ISO `yyyy-MM-dd` strings, parsed by the mapper.
 */
@Serializable
data class StationHistoryDto(
    val stationAbbr: String,
    val range: String,
    val from: String,
    val until: String,
    val days: List<HistoryDayDto>,
)

@Serializable
data class HistoryDayDto(
    val date: String,
    val species: List<HistorySpeciesDto>,
)

/**
 * One species on one day. [concentration] and [severity] are null together and mean "no value".
 * [severity] is a [String] for the reason given on
 * [SpeciesReadingDto][ch.stenzel.tim.polleninfo.core.measurement.data.remote.dto.SpeciesReadingDto].
 */
@Serializable
data class HistorySpeciesDto(
    val id: String,
    val concentration: Int?,
    val severity: String?,
)
