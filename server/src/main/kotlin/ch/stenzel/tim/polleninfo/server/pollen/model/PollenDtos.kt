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
