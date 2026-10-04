package ch.stenzel.tim.polleninfo.feature.alarms.data.repository

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.network.createHttpClient
import ch.stenzel.tim.polleninfo.core.preferences.FakeDeviceRegistrationRepository
import ch.stenzel.tim.polleninfo.core.push.FakePushTokenProvider
import ch.stenzel.tim.polleninfo.core.push.PushTokenResult
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.alarms.data.mapper.toDomain
import ch.stenzel.tim.polleninfo.feature.alarms.data.mapper.toInputDto
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.AlarmApiService
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.dto.AlarmDto
import ch.stenzel.tim.polleninfo.feature.alarms.dailyAlarm
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.Alarm
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmDraft
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmNotFoundException
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmSchedule
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmLimitReachedException
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.InvalidAlarmException
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.PushUnavailableException
import ch.stenzel.tim.polleninfo.feature.alarms.thresholdAlarm
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Through a real Ktor client backed by [MockEngine], so the JSON contract with the backend —
 * including the polymorphic `schedule` — is under test alongside the registration logic.
 */
class AlarmRepositoryImplTest {

    private val jsonHeaders = headersOf("Content-Type" to listOf("application/json"))

    private val registration = FakeDeviceRegistrationRepository()
    private val pushTokens = FakePushTokenProvider()
    private val recorded = mutableListOf<HttpRequestData>()

    private val dailyJson = """
        { "id": "daily-1", "enabled": true, "stationAbbr": "PZH",
          "species": ["BIRCH", "GRASSES"], "minSeverity": "NONE",
          "days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
          "schedule": { "type": "daily", "at": "08:00" } }
    """.trimIndent()

    private val thresholdJson = """
        { "id": "threshold-1", "enabled": false, "stationAbbr": "PBE",
          "species": ["HAZEL"], "minSeverity": "HIGH",
          "days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"],
          "schedule": { "type": "threshold", "from": "07:00", "until": "21:00" } }
    """.trimIndent()

    private fun repository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): AlarmRepositoryImpl {
        val engine = MockEngine { request ->
            recorded += request
            handler(request)
        }
        val api = AlarmApiService(createHttpClient(engine), "http://backend.test:8080")
        return AlarmRepositoryImpl(api, registration, pushTokens)
    }

    /**
     * A backend that issues `device-N` ids and lists [alarmsJson] for the devices it knows. Any known
     * device can update or delete the alarms in [alarmIds]; anything else is a `404`, as on the real
     * backend.
     */
    private fun backend(
        knownDevices: MutableSet<String> = mutableSetOf(),
        alarmsJson: String = "[]",
        alarmIds: Set<String> = setOf("daily-1"),
    ): suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { request ->
        val path = request.url.encodedPath
        val alarmPath = Regex("/devices/([^/]+)/alarms/([^/]+)").matchEntire(path)
        when {
            alarmPath != null -> {
                val (deviceId, alarmId) = alarmPath.destructured
                when {
                    deviceId !in knownDevices || alarmId !in alarmIds -> respondError(HttpStatusCode.NotFound)
                    request.method == HttpMethod.Put -> {
                        val input = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                        val updated = JsonObject(mapOf("id" to JsonPrimitive(alarmId)) + input)
                        respond(updated.toString(), HttpStatusCode.OK, jsonHeaders)
                    }
                    request.method == HttpMethod.Delete -> respond("", HttpStatusCode.NoContent)
                    else -> respondError(HttpStatusCode.MethodNotAllowed)
                }
            }
            request.method == HttpMethod.Put && path.endsWith("/token") -> {
                val deviceId = path.removePrefix("/devices/").removeSuffix("/token")
                if (deviceId in knownDevices) respond("", HttpStatusCode.NoContent) else respondError(HttpStatusCode.NotFound)
            }
            request.method == HttpMethod.Post && path == "/devices" -> {
                val id = "device-${knownDevices.size + 1}"
                knownDevices += id
                respond("""{"deviceId":"$id"}""", HttpStatusCode.Created, jsonHeaders)
            }
            request.method == HttpMethod.Post && path.endsWith("/alarms") -> {
                val deviceId = path.removePrefix("/devices/").removeSuffix("/alarms")
                if (deviceId in knownDevices) {
                    val input = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                    val created = JsonObject(mapOf("id" to JsonPrimitive("created-1")) + input)
                    respond(created.toString(), HttpStatusCode.Created, jsonHeaders)
                } else {
                    respondError(HttpStatusCode.NotFound)
                }
            }
            request.method == HttpMethod.Get && path.endsWith("/alarms") -> {
                val deviceId = path.removePrefix("/devices/").removeSuffix("/alarms")
                if (deviceId in knownDevices) {
                    respond(alarmsJson, HttpStatusCode.OK, jsonHeaders)
                } else {
                    respondError(HttpStatusCode.NotFound)
                }
            }
            else -> respondError(HttpStatusCode.MethodNotAllowed)
        }
    }

    private fun HttpRequestData.describe() = "${method.value} ${url.encodedPath}"

    @Test
    fun `the first call registers with the push token and stores the issued id`() = runTest {
        val repository = repository(backend())

        val result = repository.alarms()

        assertIs<Result.Success<List<Alarm>>>(result)
        assertEquals(listOf("POST /devices", "GET /devices/device-1/alarms"), recorded.map { it.describe() })
        val body = Json.parseToJsonElement((recorded.first().body as TextContent).text).jsonObject
        assertEquals("fcm-token-1", body.getValue("fcmToken").jsonPrimitive.content)
        assertEquals("device-1", registration.stored)
    }

    @Test
    fun `later calls reuse the stored id without registering again`() = runTest {
        val repository = repository(backend())

        repository.alarms()
        repository.alarms()

        assertEquals(
            listOf("POST /devices", "GET /devices/device-1/alarms", "GET /devices/device-1/alarms"),
            recorded.map { it.describe() },
        )
        assertEquals(1, pushTokens.callCount)
        assertEquals(listOf("device-1"), registration.storedIds)
    }

    @Test
    fun `an id stored earlier is used without registering`() = runTest {
        registration.store("device-7")
        val repository = repository(backend(knownDevices = mutableSetOf("device-7")))

        repository.alarms()

        assertEquals(listOf("GET /devices/device-7/alarms"), recorded.map { it.describe() })
        assertEquals(0, pushTokens.callCount)
    }

    @Test
    fun `updateToken with a stored id sends PUT token with the new token`() = runTest {
        registration.store("device-7")
        val repository = repository(backend(knownDevices = mutableSetOf("device-7")))

        val result = repository.updateToken("fcm-token-2")

        assertIs<Result.Success<Unit>>(result)
        assertEquals(listOf("PUT /devices/device-7/token"), recorded.map { it.describe() })
        val body = Json.parseToJsonElement((recorded.single().body as TextContent).text).jsonObject
        assertEquals("fcm-token-2", body.getValue("fcmToken").jsonPrimitive.content)
        assertEquals("device-7", registration.stored)
    }

    @Test
    fun `updateToken without a stored id makes no request and does not register`() = runTest {
        val repository = repository(backend())

        val result = repository.updateToken("fcm-token-2")

        assertIs<Result.Success<Unit>>(result)
        assertTrue(recorded.isEmpty())
        assertEquals(0, pushTokens.callCount)
        assertEquals(null, registration.stored)
    }

    @Test
    fun `updateToken for a device the backend forgot drops the id so the next call registers afresh`() = runTest {
        registration.store("forgotten")
        val repository = repository(backend())

        val result = repository.updateToken("fcm-token-2")
        repository.alarms()

        assertIs<Result.Success<Unit>>(result)
        assertEquals(
            listOf("PUT /devices/forgotten/token", "POST /devices", "GET /devices/device-1/alarms"),
            recorded.map { it.describe() },
        )
        assertEquals("device-1", registration.stored)
    }

    @Test
    fun `updateToken fails on a server error and keeps the stored id`() = runTest {
        registration.store("device-7")
        val repository = repository { respondError(HttpStatusCode.InternalServerError) }

        val result = repository.updateToken("fcm-token-2")

        assertIs<Result.Failure>(result)
        assertEquals("device-7", registration.stored)
        assertEquals(0, registration.clearCount)
    }

    @Test
    fun `an unavailable push token fails with PushUnavailable and makes no request`() = runTest {
        pushTokens.result = PushTokenResult.Unavailable
        val repository = repository(backend())

        val result = repository.alarms()

        assertIs<PushUnavailableException>(assertIs<Result.Failure>(result).exception)
        assertTrue(recorded.isEmpty())
        assertEquals(null, registration.stored)
    }

    @Test
    fun `a 404 for the stored id clears it then registers once and retries`() = runTest {
        registration.store("forgotten")
        val repository = repository(backend(alarmsJson = "[$dailyJson]"))

        val result = repository.alarms()

        assertEquals(listOf(dailyAlarm()), assertIs<Result.Success<List<Alarm>>>(result).data)
        assertEquals(
            listOf("GET /devices/forgotten/alarms", "POST /devices", "GET /devices/device-1/alarms"),
            recorded.map { it.describe() },
        )
        assertEquals(1, registration.clearCount)
        assertEquals("device-1", registration.stored)
    }

    @Test
    fun `a second 404 after re-registering fails instead of registering again`() = runTest {
        val repository = repository { request ->
            if (request.method == HttpMethod.Post) {
                respond("""{"deviceId":"device-1"}""", HttpStatusCode.Created, jsonHeaders)
            } else {
                respondError(HttpStatusCode.NotFound)
            }
        }
        registration.store("forgotten")

        val result = repository.alarms()

        assertIs<Result.Failure>(result)
        assertEquals(1, recorded.count { it.method == HttpMethod.Post })
        assertEquals(2, recorded.count { it.method == HttpMethod.Get })
    }

    @Test
    fun `a failed registration fails the call and stores nothing`() = runTest {
        val repository = repository { respondError(HttpStatusCode.InternalServerError) }

        assertIs<Result.Failure>(repository.alarms())
        assertEquals(null, registration.stored)
    }

    @Test
    fun `a server error on the list fails without re-registering`() = runTest {
        registration.store("device-1")
        val repository = repository { respondError(HttpStatusCode.InternalServerError) }

        assertIs<Result.Failure>(repository.alarms())
        assertEquals(1, recorded.size)
        assertEquals(0, registration.clearCount)
    }

    @Test
    fun `an unreachable backend fails instead of throwing`() = runTest {
        val repository = repository { throw RuntimeException("Connection refused") }

        assertIs<Result.Failure>(repository.alarms())
    }

    @Test
    fun `both schedule variants map from the wire in creation order`() = runTest {
        val repository = repository(backend(alarmsJson = "[$dailyJson, $thresholdJson]"))

        val result = repository.alarms()

        assertEquals(
            listOf(dailyAlarm(), thresholdAlarm()),
            assertIs<Result.Success<List<Alarm>>>(result).data,
        )
    }

    @Test
    fun `both schedule variants round trip through the DTO and the mapper`() {
        val json = Json { ignoreUnknownKeys = true }
        listOf(dailyJson, thresholdJson).forEach { wire ->
            val alarm = json.decodeFromString<AlarmDto>(wire).toDomain()

            val encoded = json.parseToJsonElement(json.encodeToString(alarm.toInputDto())).jsonObject
            val original = json.parseToJsonElement(wire).jsonObject

            assertEquals(original - "id", encoded)
        }
    }

    private val draft = AlarmDraft(
        enabled = true,
        stationAbbr = "PBE",
        species = setOf("GRASSES", "BIRCH"),
        minSeverity = PollenSeverity.MODERATE,
        days = setOf(DayOfWeek.SUNDAY, DayOfWeek.SATURDAY),
        schedule = AlarmSchedule.Daily(LocalTime(7, 30)),
    )

    @Test
    fun `create posts the draft and returns the stored alarm`() = runTest {
        registration.store("device-1")
        val repository = repository(backend(knownDevices = mutableSetOf("device-1")))

        val result = repository.create(draft)

        val created = assertIs<Result.Success<Alarm>>(result).data
        assertEquals("created-1", created.id)
        assertEquals(draft.stationAbbr, created.stationAbbr)
        assertEquals(draft.species, created.species)
        assertEquals(draft.schedule, created.schedule)
        assertEquals(listOf("POST /devices/device-1/alarms"), recorded.map { it.describe() })
        assertEquals(
            Json.parseToJsonElement(
                """
                { "enabled": true, "stationAbbr": "PBE",
                  "species": ["BIRCH", "GRASSES"], "minSeverity": "MODERATE",
                  "days": ["SATURDAY", "SUNDAY"],
                  "schedule": { "type": "daily", "at": "07:30" } }
                """,
            ),
            Json.parseToJsonElement((recorded.single().body as TextContent).text),
        )
    }

    @Test
    fun `create registers first on a fresh install`() = runTest {
        val repository = repository(backend())

        assertIs<Result.Success<Alarm>>(repository.create(draft))
        assertEquals(listOf("POST /devices", "POST /devices/device-1/alarms"), recorded.map { it.describe() })
    }

    @Test
    fun `create re-registers once and retries on an unknown-device 404`() = runTest {
        registration.store("forgotten")
        val repository = repository(backend())

        assertIs<Result.Success<Alarm>>(repository.create(draft))
        assertEquals(
            listOf("POST /devices/forgotten/alarms", "POST /devices", "POST /devices/device-1/alarms"),
            recorded.map { it.describe() },
        )
    }

    @Test
    fun `a 400 on create fails with InvalidAlarm carrying the backend's reason`() = runTest {
        registration.store("device-1")
        val repository = repository {
            respond("""{"error":"Select at least one day"}""", HttpStatusCode.BadRequest, jsonHeaders)
        }

        val failure = assertIs<Result.Failure>(repository.create(draft))

        assertEquals("Select at least one day", assertIs<InvalidAlarmException>(failure.exception).message)
        assertEquals(0, registration.clearCount)
    }

    @Test
    fun `a 409 on create fails with AlarmLimitReached without re-registering`() = runTest {
        registration.store("device-1")
        val repository = repository {
            respond("""{"error":"A device can hold at most 10 alarms"}""", HttpStatusCode.Conflict, jsonHeaders)
        }

        val failure = assertIs<Result.Failure>(repository.create(draft))

        assertIs<AlarmLimitReachedException>(failure.exception)
        assertEquals(1, recorded.size)
        assertEquals(0, registration.clearCount)
    }

    @Test
    fun `a server error on create fails without re-registering`() = runTest {
        registration.store("device-1")
        val repository = repository { respondError(HttpStatusCode.InternalServerError) }

        assertIs<Result.Failure>(repository.create(draft))
        assertEquals(1, recorded.size)
    }

    @Test
    fun `an unknown severity on the wire fails the call rather than defaulting`() = runTest {
        val repository = repository(backend(alarmsJson = "[${dailyJson.replace("\"NONE\"", "\"EXTREME\"")}]"))

        assertIs<Result.Failure>(repository.alarms())
    }

    // --- single alarms ---

    @Test
    fun `alarm finds the alarm in the device's list`() = runTest {
        registration.store("device-1")
        val repository = repository(backend(knownDevices = mutableSetOf("device-1"), alarmsJson = "[$dailyJson, $thresholdJson]"))

        assertEquals(thresholdAlarm(), assertIs<Result.Success<Alarm>>(repository.alarm("threshold-1")).data)
    }

    @Test
    fun `alarm fails with AlarmNotFound for an id the device does not have`() = runTest {
        registration.store("device-1")
        val repository = repository(backend(knownDevices = mutableSetOf("device-1"), alarmsJson = "[$dailyJson]"))

        val failure = assertIs<Result.Failure>(repository.alarm("gone"))

        assertIs<AlarmNotFoundException>(failure.exception)
    }

    @Test
    fun `update puts the draft and returns the stored alarm`() = runTest {
        registration.store("device-1")
        val repository = repository(backend(knownDevices = mutableSetOf("device-1")))

        val updated = assertIs<Result.Success<Alarm>>(repository.update("daily-1", draft)).data

        assertEquals("daily-1", updated.id)
        assertEquals(draft.schedule, updated.schedule)
        assertEquals(listOf("PUT /devices/device-1/alarms/daily-1"), recorded.map { it.describe() })
        assertEquals(
            Json.parseToJsonElement(draft.toInputDto().let { Json.encodeToString(it) }),
            Json.parseToJsonElement((recorded.single().body as TextContent).text),
        )
    }

    @Test
    fun `update re-registers once and retries on an unknown-device 404`() = runTest {
        registration.store("forgotten")
        val repository = repository(backend())

        assertIs<Result.Success<Alarm>>(repository.update("daily-1", draft))
        assertEquals(
            listOf(
                "PUT /devices/forgotten/alarms/daily-1",
                "GET /devices/forgotten/alarms",
                "POST /devices",
                "PUT /devices/device-1/alarms/daily-1",
            ),
            recorded.map { it.describe() },
        )
        assertEquals(1, registration.clearCount)
    }

    @Test
    fun `delete sends a DELETE and succeeds on 204`() = runTest {
        registration.store("device-1")
        val repository = repository(backend(knownDevices = mutableSetOf("device-1")))

        assertIs<Result.Success<Unit>>(repository.delete("daily-1"))
        assertEquals(listOf("DELETE /devices/device-1/alarms/daily-1"), recorded.map { it.describe() })
    }

    @Test
    fun `delete re-registers once and retries on an unknown-device 404`() = runTest {
        registration.store("forgotten")
        val repository = repository(backend())

        assertIs<Result.Success<Unit>>(repository.delete("daily-1"))
        assertEquals(
            listOf(
                "DELETE /devices/forgotten/alarms/daily-1",
                "GET /devices/forgotten/alarms",
                "POST /devices",
                "DELETE /devices/device-1/alarms/daily-1",
            ),
            recorded.map { it.describe() },
        )
        assertEquals(1, registration.clearCount)
    }

    @Test
    fun `a 404 for an alarm the known device does not have fails without re-registering`() = runTest {
        registration.store("device-1")
        val repository = repository(backend(knownDevices = mutableSetOf("device-1"), alarmIds = emptySet()))

        val updateFailure = assertIs<Result.Failure>(repository.update("gone", draft))
        val deleteFailure = assertIs<Result.Failure>(repository.delete("gone"))

        assertIs<AlarmNotFoundException>(updateFailure.exception)
        assertIs<AlarmNotFoundException>(deleteFailure.exception)
        assertEquals(0, registration.clearCount)
        assertEquals(0, recorded.count { it.method == HttpMethod.Post })
    }

    @Test
    fun `a 400 on update fails with InvalidAlarm carrying the backend's reason`() = runTest {
        registration.store("device-1")
        val repository = repository {
            respond("""{"error":"Select at least one day"}""", HttpStatusCode.BadRequest, jsonHeaders)
        }

        val failure = assertIs<Result.Failure>(repository.update("daily-1", draft))

        assertEquals("Select at least one day", assertIs<InvalidAlarmException>(failure.exception).message)
    }
}
