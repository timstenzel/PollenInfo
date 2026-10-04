package ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequestDto(val fcmToken: String)

/** Body of `PUT /devices/{deviceId}/token`. */
@Serializable
data class UpdateTokenRequestDto(val fcmToken: String)

@Serializable
data class RegisterDeviceResponseDto(val deviceId: String)

/**
 * Wire shape of one alarm, mirroring the server's `AlarmDto`. [days] are `DayOfWeek` names and every
 * time is `HH:mm`.
 *
 * [minSeverity] is a [String] for the same reason as `SpeciesReadingDto.severity`: the mapper holds
 * the five wire strings in one tested place instead of tying them to the enum's constant names.
 */
@Serializable
data class AlarmDto(
    val id: String,
    val enabled: Boolean,
    val stationAbbr: String,
    val species: List<String>,
    val minSeverity: String,
    val days: List<String>,
    val schedule: ScheduleDto,
)

/** The body of a create or update: an [AlarmDto] without its id. */
@Serializable
data class AlarmInputDto(
    val enabled: Boolean,
    val stationAbbr: String,
    val species: List<String>,
    val minSeverity: String,
    val days: List<String>,
    val schedule: ScheduleDto,
)

/** Polymorphic on the wire through the default `type` class discriminator, as on the server. */
@Serializable
sealed interface ScheduleDto {

    @Serializable
    @SerialName("daily")
    data class Daily(val at: String) : ScheduleDto

    @Serializable
    @SerialName("threshold")
    data class Threshold(val from: String, val until: String) : ScheduleDto
}

/** Body of every `400` from the alarm endpoints. */
@Serializable
data class ErrorDto(val error: String)
