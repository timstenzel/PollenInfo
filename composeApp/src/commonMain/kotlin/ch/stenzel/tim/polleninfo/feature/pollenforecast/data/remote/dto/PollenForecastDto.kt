package ch.stenzel.tim.polleninfo.feature.pollenforecast.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AirQualityResponseDto(
    val latitude: Double,
    val longitude: Double,
    @SerialName("timezone") val timezone: String,
    @SerialName("hourly") val hourly: HourlyDto,
)

@Serializable
data class HourlyDto(
    @SerialName("time") val time: List<String>,
    @SerialName("birch_pollen") val birchPollen: List<Float?> = emptyList(),
    @SerialName("grass_pollen") val grassPollen: List<Float?> = emptyList(),
    @SerialName("mugwort_pollen") val mugwortPollen: List<Float?> = emptyList(),
    @SerialName("alder_pollen") val alderPollen: List<Float?> = emptyList(),
    @SerialName("olive_pollen") val olivePollen: List<Float?> = emptyList(),
    @SerialName("ragweed_pollen") val ragweedPollen: List<Float?> = emptyList(),
)
