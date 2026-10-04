package ch.stenzel.tim.polleninfo.server.alarm

import ch.stenzel.tim.polleninfo.server.alarm.domain.ALARM_TIME_FORMAT
import ch.stenzel.tim.polleninfo.server.alarm.domain.Alarm
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSchedule
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import ch.stenzel.tim.polleninfo.server.alarm.model.AlarmDto
import ch.stenzel.tim.polleninfo.server.alarm.model.ErrorDto
import ch.stenzel.tim.polleninfo.server.alarm.model.RegisterDeviceRequest
import ch.stenzel.tim.polleninfo.server.alarm.model.RegisterDeviceResponse
import ch.stenzel.tim.polleninfo.server.alarm.model.ScheduleDto
import ch.stenzel.tim.polleninfo.server.alarm.store.AlarmStore
import ch.stenzel.tim.polleninfo.server.alarm.store.DeviceStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * The device's alarms. There are no accounts: a device registers once, anonymously, and its id is
 * the key to everything else under `/devices/{deviceId}`. An unknown id is a `404`, which is how the
 * app learns that the server has forgotten it and must register again.
 */
fun Route.alarmRoutes(devices: DeviceStore, alarms: AlarmStore) {
    route("/devices") {
        post {
            // Caught here rather than left to StatusPages, whose catch-all would answer a malformed
            // body with a 500.
            val request = try {
                call.receive<RegisterDeviceRequest>()
            } catch (e: BadRequestException) {
                return@post call.respond(HttpStatusCode.BadRequest, ErrorDto("Expected {\"fcmToken\": \"…\"}"))
            }
            if (request.fcmToken.isBlank()) {
                return@post call.respond(HttpStatusCode.BadRequest, ErrorDto("fcmToken must not be blank"))
            }
            val id = devices.register(request.fcmToken)
            call.respond(HttpStatusCode.Created, RegisterDeviceResponse(id.value))
        }

        get("/{deviceId}/alarms") {
            val deviceId = call.parameters["deviceId"]?.let(::DeviceId)
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            val list = alarms.list(deviceId)
                ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(list.map { it.toDto() })
        }
    }
}

private fun Alarm.toDto() = AlarmDto(
    id = id.value,
    enabled = enabled,
    stationAbbr = station.abbr,
    species = species.sorted(),
    minSeverity = minSeverity,
    days = days.sorted().map { it.name },
    schedule = when (val schedule = schedule) {
        is AlarmSchedule.Daily -> ScheduleDto.Daily(schedule.at.format(ALARM_TIME_FORMAT))
        is AlarmSchedule.Threshold -> ScheduleDto.Threshold(
            from = schedule.from.format(ALARM_TIME_FORMAT),
            until = schedule.until.format(ALARM_TIME_FORMAT),
        )
    },
)
