package ch.stenzel.tim.polleninfo.core.species.data.remote.dto

import kotlinx.serialization.Serializable

/** Wire shape of one entry in `GET /pollen/species`, mirroring the server's own `SpeciesDto`. */
@Serializable
data class SpeciesDto(
    val id: String,
    val name: String,
    val latinName: String,
)
