package ch.stenzel.tim.polleninfo.server.pollen.model

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.SpeciesThresholds
import kotlinx.serialization.Serializable

@Serializable
data class StationDto(
    val abbr: String,
    val name: String,
    val canton: String,
    val latitude: Double,
    val longitude: Double,
    val altitudeMasl: Int,
)

@Serializable
data class SpeciesDto(
    val id: String,
    val name: String,
    val latinName: String,
)

@Serializable
data class ThresholdsDto(
    val unit: String,
    val bySpecies: Map<String, SpeciesThresholds>,
)

/**
 * Wire shape of `GET /pollen/stations/{abbr}/measurements`.
 *
 * [measuredAt] is an ISO-8601 instant in UTC and is **mandatory**: a response that can be served
 * from a cache after an upstream failure has to say when its reading is from.
 *
 * There is deliberately no `overallSeverity` field and [species] is unsorted (declaration order).
 * The worst severity and the display order are pure functions of [species]; deriving them on the
 * wire as well would give the two sides two answers that can disagree.
 */
@Serializable
data class StationMeasurementDto(
    val stationAbbr: String,
    val measuredAt: String,
    val unit: String,
    val species: List<SpeciesReadingDto>,
)

/**
 * One taxon in a measurement response. All seven are always present.
 *
 * [concentration] and [severity] are null together and mean "no reading", which is a different fact
 * from a concentration of `0` with a severity of `NONE`.
 */
@Serializable
data class SpeciesReadingDto(
    val id: String,
    val name: String,
    val latinName: String,
    val concentration: Int?,
    val severity: PollenSeverity?,
)

/**
 * Wire shape of `GET /pollen/stations/{abbr}/history?range=week|month|year`.
 *
 * [days] holds one entry per date from [from] to [until] (ISO `yyyy-MM-dd`, both inclusive), oldest
 * first, with no date left out: a day the publisher has no value for is present with every taxon
 * empty.
 */
@Serializable
data class StationHistoryDto(
    val stationAbbr: String,
    val range: String,
    val from: String,
    val until: String,
    val days: List<HistoryDayDto>,
)

/** One day of a history response. [species] holds all seven taxa in declaration order. */
@Serializable
data class HistoryDayDto(
    val date: String,
    val species: List<HistorySpeciesDto>,
)

/**
 * One taxon's daily mean. [concentration] and [severity] are null together and mean "no value",
 * as in [SpeciesReadingDto].
 */
@Serializable
data class HistorySpeciesDto(
    val id: String,
    val concentration: Int?,
    val severity: PollenSeverity?,
)

/**
 * A `400` body. The same `{ "error": "…" }` shape as the alarm routes' — declared again here
 * because `pollen/` does not import from `alarm/`.
 */
@Serializable
data class ErrorDto(val error: String)
