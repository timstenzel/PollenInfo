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
import ch.stenzel.tim.polleninfo.server.alarm.model.FcmTokenRequest
import ch.stenzel.tim.polleninfo.server.alarm.model.RegisterDeviceResponse
import ch.stenzel.tim.polleninfo.server.alarm.model.ScheduleDto
import ch.stenzel.tim.polleninfo.server.alarm.store.AlarmStore
import ch.stenzel.tim.polleninfo.server.alarm.store.CreateResult
import ch.stenzel.tim.polleninfo.server.alarm.store.DeviceStore
import ch.stenzel.tim.polleninfo.server.alarm.store.MAX_ALARMS_PER_DEVICE
import ch.stenzel.tim.polleninfo.server.plugins.DEVICE_AUTH
import ch.stenzel.tim.polleninfo.server.plugins.DEVICE_LIMIT
import ch.stenzel.tim.polleninfo.server.plugins.DevicePrincipal
import ch.stenzel.tim.polleninfo.server.plugins.REGISTER_LIMIT
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail

/**
 * Registration and the device's alarms. There are no accounts: a device registers once,
 * anonymously, and receives a device token — the key to everything under `/devices/me`, sent as
 * `Authorization: Bearer …`. A missing or unknown token is a `401`, which is how the app learns that
 * the server has forgotten it and must register again.
 *
 * Authentication runs before anything reads the body, so an invalid body from an unauthenticated
 * caller is a `401`, not a `400`.
 */
fun Route.alarmRoutes(devices: DeviceStore, alarms: AlarmStore) {
    route("/devices") {
        rateLimit(REGISTER_LIMIT) {
            post {
                // Caught here rather than left to StatusPages, so the 400 says what was expected.
                val fcmToken = call.receiveFcmToken() ?: return@post
                val token = devices.register(fcmToken)
                call.respond(HttpStatusCode.Created, RegisterDeviceResponse(token.value))
            }
        }

        // The device limiter inside authentication, so it counts per device and an unknown token
        // never gets a bucket of its own.
        authenticate(DEVICE_AUTH) {
            rateLimit(DEVICE_LIMIT) {
                route("/me") {
                    delete {
                        // Its alarms and their notification log go with it. A device that vanished
                        // since authenticating has been deleted already, which is what was asked for.
                        devices.delete(call.deviceId())
                        call.respond(HttpStatusCode.NoContent)
                    }

                    put("/fcm-token") {
                        val deviceId = call.deviceId()
                        val fcmToken = call.receiveFcmToken() ?: return@put
                        if (devices.updateFcmToken(deviceId, fcmToken)) {
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(HttpStatusCode.Unauthorized)
                        }
                    }

                    get("/alarms") {
                        call.respond(alarms.list(call.deviceId()).map { it.toDto() })
                    }

                    post("/alarms") {
                        val deviceId = call.deviceId()
                        val spec = call.receiveAlarmSpec() ?: return@post
                        when (val result = alarms.create(deviceId, spec)) {
                            is CreateResult.Created -> call.respond(HttpStatusCode.Created, result.alarm.toDto())
                            CreateResult.LimitReached -> call.respond(
                                HttpStatusCode.Conflict,
                                ErrorDto("A device can hold at most $MAX_ALARMS_PER_DEVICE alarms"),
                            )
                        }
                    }

                    // An unknown alarm, another device's alarm and an id that is not a UUID are all the
                    // same 404, so an alarm id reveals nothing to a device it does not belong to.
                    put("/alarms/{alarmId}") {
                        val deviceId = call.deviceId()
                        val alarmId = AlarmId(call.parameters.getOrFail("alarmId"))
                        val spec = call.receiveAlarmSpec() ?: return@put
                        val updated = alarms.update(deviceId, alarmId, spec)
                            ?: return@put call.respond(HttpStatusCode.NotFound)
                        call.respond(updated.toDto())
                    }

                    delete("/alarms/{alarmId}") {
                        val deviceId = call.deviceId()
                        val alarmId = AlarmId(call.parameters.getOrFail("alarmId"))
                        if (alarms.delete(deviceId, alarmId)) {
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }
            }
        }
    }
}

/** The authenticated device; only called inside [authenticate], where a principal always exists. */
private fun ApplicationCall.deviceId(): DeviceId = checkNotNull(principal<DevicePrincipal>()).deviceId

/** The body's push address, or `null` once a `400 {error}` has been sent. */
private suspend fun ApplicationCall.receiveFcmToken(): String? {
    val request = try {
        receive<FcmTokenRequest>()
    } catch (e: BadRequestException) {
        respond(HttpStatusCode.BadRequest, ErrorDto("Expected {\"fcmToken\": \"…\"}"))
        return null
    }
    if (request.fcmToken.isBlank()) {
        respond(HttpStatusCode.BadRequest, ErrorDto("fcmToken must not be blank"))
        return null
    }
    return request.fcmToken
}

/**
 * The request body as a valid [AlarmSpec], or `null` once a `400 {error}` has been sent. Validation
 * comes before any store lookup, so an invalid body for an unknown alarm is a `400`.
 */
private suspend fun ApplicationCall.receiveAlarmSpec(): AlarmSpec? {
    // As for registration: a malformed body gets a 400 that says so.
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
