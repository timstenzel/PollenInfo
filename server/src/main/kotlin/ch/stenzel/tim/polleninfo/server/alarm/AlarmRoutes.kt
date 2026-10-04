package ch.stenzel.tim.polleninfo.server.alarm

import ch.stenzel.tim.polleninfo.server.alarm.domain.ALARM_TIME_FORMAT
import ch.stenzel.tim.polleninfo.server.alarm.domain.Alarm
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmId
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmInput
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSchedule
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSpec
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmValidation
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import ch.stenzel.tim.polleninfo.server.alarm.domain.ScheduleInput
import ch.stenzel.tim.polleninfo.server.alarm.domain.ValidationResult
import ch.stenzel.tim.polleninfo.server.alarm.model.AlarmDto
import ch.stenzel.tim.polleninfo.server.alarm.model.AlarmInputDto
import ch.stenzel.tim.polleninfo.server.alarm.model.ErrorDto
import ch.stenzel.tim.polleninfo.server.alarm.model.RegisterDeviceRequest
import ch.stenzel.tim.polleninfo.server.alarm.model.RegisterDeviceResponse
import ch.stenzel.tim.polleninfo.server.alarm.model.ScheduleDto
import ch.stenzel.tim.polleninfo.server.alarm.store.AlarmStore
import ch.stenzel.tim.polleninfo.server.alarm.store.CreateResult
import ch.stenzel.tim.polleninfo.server.alarm.store.DeviceStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
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

        post("/{deviceId}/alarms") {
            val deviceId = call.parameters["deviceId"]?.let(::DeviceId)
                ?: return@post call.respond(HttpStatusCode.BadRequest)
            val spec = call.receiveAlarmSpec() ?: return@post
            when (val result = alarms.create(deviceId, spec)) {
                is CreateResult.Created -> call.respond(HttpStatusCode.Created, result.alarm.toDto())
                CreateResult.UnknownDevice -> call.respond(HttpStatusCode.NotFound)
            }
        }

        // An unknown device, an unknown alarm and another device's alarm are all the same 404, so an
        // alarm id reveals nothing to a device it does not belong to.
        put("/{deviceId}/alarms/{alarmId}") {
            val deviceId = call.parameters["deviceId"]?.let(::DeviceId)
                ?: return@put call.respond(HttpStatusCode.BadRequest)
            val alarmId = call.parameters["alarmId"]?.let(::AlarmId)
                ?: return@put call.respond(HttpStatusCode.BadRequest)
            val spec = call.receiveAlarmSpec() ?: return@put
            val updated = alarms.update(deviceId, alarmId, spec)
                ?: return@put call.respond(HttpStatusCode.NotFound)
            call.respond(updated.toDto())
        }

        delete("/{deviceId}/alarms/{alarmId}") {
            val deviceId = call.parameters["deviceId"]?.let(::DeviceId)
                ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val alarmId = call.parameters["alarmId"]?.let(::AlarmId)
                ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (alarms.delete(deviceId, alarmId)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

/**
 * The request body as a valid [AlarmSpec], or `null` once a `400 {error}` has been sent. Validation
 * comes before any store lookup, so an invalid body for an unknown device or alarm is a `400`.
 */
private suspend fun ApplicationCall.receiveAlarmSpec(): AlarmSpec? {
    // As for registration: a malformed body is the client's error, not a 500.
    val input = try {
        receive<AlarmInputDto>()
    } catch (e: BadRequestException) {
        respond(HttpStatusCode.BadRequest, ErrorDto("Malformed alarm"))
        return null
    }
    return when (val result = AlarmValidation.validate(input.toDomain())) {
        is ValidationResult.Valid -> result.spec
        is ValidationResult.Invalid -> {
            respond(HttpStatusCode.BadRequest, ErrorDto(result.message))
            null
        }
    }
}

private fun AlarmInputDto.toDomain() = AlarmInput(
    enabled = enabled,
    stationAbbr = stationAbbr,
    species = species,
    minSeverity = minSeverity,
    days = days,
    schedule = when (schedule) {
        is ScheduleDto.Daily -> ScheduleInput.Daily(schedule.at)
        is ScheduleDto.Threshold -> ScheduleInput.Threshold(schedule.from, schedule.until)
    },
)

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
