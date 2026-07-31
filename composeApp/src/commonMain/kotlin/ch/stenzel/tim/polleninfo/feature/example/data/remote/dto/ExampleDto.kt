package ch.stenzel.tim.polleninfo.feature.example.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Wire shape of the placeholder endpoint the reference feature talks to. Field names match the JSON
 * exactly, so no `@SerialName` mapping is needed — add it when a real payload uses snake_case or
 * names that read badly in Kotlin.
 */
@Serializable
data class ExampleResponseDto(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val hourly: HourlyDto,
)

/**
 * Column-oriented payload: [time] is the axis and every species list runs parallel to it.
 *
 * Each series defaults to empty so a payload that omits one still deserializes; the mapper then
 * reads the missing hours as zero.
 */
@Serializable
data class HourlyDto(
    val time: List<String>,
    val birch: List<Float?> = emptyList(),
    val grass: List<Float?> = emptyList(),
    val mugwort: List<Float?> = emptyList(),
    val alder: List<Float?> = emptyList(),
    val olive: List<Float?> = emptyList(),
    val ragweed: List<Float?> = emptyList(),
)
