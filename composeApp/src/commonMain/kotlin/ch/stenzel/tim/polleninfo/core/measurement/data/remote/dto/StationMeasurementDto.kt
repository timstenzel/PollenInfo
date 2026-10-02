package ch.stenzel.tim.polleninfo.core.measurement.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire shape of `GET /pollen/stations/{abbr}/measurements`, mirroring the server's own DTO. Field
 * names match the JSON exactly, so no `@SerialName` mapping is needed.
 *
 * [measuredAt] is a non-null ISO-8601 instant, kept as a [String] here and parsed by the mapper. It
 * is required because a response that can be served from a cache after an upstream failure must
 * always say when its reading is from.
 */
@Serializable
data class StationMeasurementDto(
    val stationAbbr: String,
    val measuredAt: String,
    val unit: String,
    val species: List<SpeciesReadingDto>,
)

/**
 * One taxon in the response. [concentration] and [severity] are null together and mean "no
 * reading", as distinct from a concentration of `0` with a severity of `"NONE"`.
 *
 * [severity] is a [String] rather than the enum on purpose: deserialising straight into
 * [PollenSeverity][ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity] would tie
 * the accepted wire strings to whatever the constants happen to be called, so a rename on either
 * side would pass silently. Mapping it by hand puts the five strings in one testable place.
 */
@Serializable
data class SpeciesReadingDto(
    val id: String,
    val name: String,
    val latinName: String,
    val concentration: Int?,
    val severity: String?,
)
