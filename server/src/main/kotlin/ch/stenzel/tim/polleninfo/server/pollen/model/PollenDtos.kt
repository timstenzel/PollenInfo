package ch.stenzel.tim.polleninfo.server.pollen.model

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
