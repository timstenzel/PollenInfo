package ch.stenzel.tim.polleninfo.server.alarm.model

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body of `POST /devices`. */
@Serializable
data class RegisterDeviceRequest(val fcmToken: String)

/** Body of `PUT /devices/{deviceId}/token`: the token FCM rotated to. */
@Serializable
data class UpdateTokenRequest(val fcmToken: String)

/** Response of `POST /devices`: the id the app sends with every later alarm call. */
@Serializable
data class RegisterDeviceResponse(val deviceId: String)

/**
 * Wire shape of one alarm. [days] are `java.time.DayOfWeek` names, and every time is `HH:mm` in
 * Swiss local time.
 */
@Serializable
data class AlarmDto(
    val id: String,
    val enabled: Boolean,
    val stationAbbr: String,
    val species: List<PollenSpecies>,
    val minSeverity: PollenSeverity,
    val days: List<String>,
    val schedule: ScheduleDto,
)

/**
 * Body of `POST /devices/{deviceId}/alarms`: an [AlarmDto] without its id.
 *
 * Every value is a plain string here, unlike in [AlarmDto], so an unknown station, species or day
 * reaches `AlarmValidation` and is answered with a message naming it rather than a decoder error.
 */
@Serializable
data class AlarmInputDto(
    val enabled: Boolean,
    val stationAbbr: String,
    val species: List<String>,
    val minSeverity: String,
    val days: List<String>,
    val schedule: ScheduleDto,
)

/**
 * Polymorphic on the wire through the default `type` class discriminator:
 * `{ "type": "daily", "at": "08:00" }` or `{ "type": "threshold", "from": "07:00", "until": "21:00" }`.
 */
@Serializable
sealed interface ScheduleDto {

    @Serializable
    @SerialName("daily")
    data class Daily(val at: String) : ScheduleDto

    @Serializable
    @SerialName("threshold")
    data class Threshold(val from: String, val until: String) : ScheduleDto
}

/** Body of every `400` response from the alarm endpoints. */
@Serializable
data class ErrorDto(val error: String)
