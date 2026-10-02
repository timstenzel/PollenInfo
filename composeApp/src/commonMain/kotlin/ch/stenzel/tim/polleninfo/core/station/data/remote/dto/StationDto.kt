package ch.stenzel.tim.polleninfo.core.station.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire shape of one entry in `GET /pollen/stations`, mirroring the server's own `StationDto`.
 * Field names match the JSON exactly, so no `@SerialName` mapping is needed.
 */
@Serializable
data class StationDto(
    val abbr: String,
    val name: String,
    val canton: String,
    val latitude: Double,
    val longitude: Double,
    val altitudeMasl: Int,
)
