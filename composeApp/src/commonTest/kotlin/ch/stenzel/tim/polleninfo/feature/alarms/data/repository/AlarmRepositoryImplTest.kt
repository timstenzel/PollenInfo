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
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.UnknownDeviceException
import ch.stenzel.tim.polleninfo.feature.alarms.thresholdAlarm
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
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
     * A backend that issues `token-N` device tokens and lists [alarmsJson] for the tokens it knows.
     * Any known token can update or delete the alarms in [alarmIds]. As on the real backend, a
     * missing or unknown token is a `401` on every device path, and a `404` on a single alarm's path
     * only ever means the alarm.
     */
    private fun backend(
        knownTokens: MutableSet<String> = mutableSetOf(),
        alarmsJson: String = "[]",
        alarmIds: Set<String> = setOf("daily-1"),
    ): suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { request ->
        val path = request.url.encodedPath
        val known = request.bearerToken() in knownTokens
        val alarmPath = Regex("/devices/me/alarms/([^/]+)").matchEntire(path)
        when {
            request.method == HttpMethod.Post && path == "/devices" -> {
                val token = "token-${knownTokens.size + 1}"
                knownTokens += token
                respond("""{"deviceToken":"$token"}""", HttpStatusCode.Created, jsonHeaders)
            }
            !path.startsWith("/devices/me/") -> respondError(HttpStatusCode.NotFound)
            !known -> respondError(HttpStatusCode.Unauthorized)
            alarmPath != null -> {
                val alarmId = alarmPath.groupValues[1]
                when {
                    alarmId !in alarmIds -> respondError(HttpStatusCode.NotFound)
                    request.method == HttpMethod.Put -> {
                        val input = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                        val updated = JsonObject(mapOf("id" to JsonPrimitive(alarmId)) + input)
                        respond(updated.toString(), HttpStatusCode.OK, jsonHeaders)
                    }
                    request.method == HttpMethod.Delete -> respond("", HttpStatusCode.NoContent)
                    else -> respondError(HttpStatusCode.MethodNotAllowed)
                }
            }
            request.method == HttpMethod.Put && path == "/devices/me/fcm-token" -> respond("", HttpStatusCode.NoContent)
            request.method == HttpMethod.Post && path == "/devices/me/alarms" -> {
                val input = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                val created = JsonObject(mapOf("id" to JsonPrimitive("created-1")) + input)
                respond(created.toString(), HttpStatusCode.Created, jsonHeaders)
            }
            request.method == HttpMethod.Get && path == "/devices/me/alarms" ->
                respond(alarmsJson, HttpStatusCode.OK, jsonHeaders)
            else -> respondError(HttpStatusCode.MethodNotAllowed)
        }
    }

    /** The token of an `Authorization: Bearer …` header, or `null` without one. */
    private fun HttpRequestData.bearerToken(): String? =
        headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")?.takeIf { it != headers[HttpHeaders.Authorization] }

    /** The request with the device token it was sent with, so a test sees which token each call used. */
    private fun HttpRequestData.describe() =
        "${method.value} ${url.encodedPath}" + (bearerToken()?.let { " [$it]" } ?: "")

    @Test
    fun `the first call registers with the push token and stores the issued device token`() = runTest {
        val repository = repository(backend())

        val result = repository.alarms()

        assertIs<Result.Success<List<Alarm>>>(result)
        assertEquals(listOf("POST /devices", "GET /devices/me/alarms [token-1]"), recorded.map { it.describe() })
        val body = Json.parseToJsonElement((recorded.first().body as TextContent).text).jsonObject
        assertEquals("fcm-token-1", body.getValue("fcmToken").jsonPrimitive.content)
        assertEquals("token-1", registration.stored)
    }

    @Test
    fun `registration sends no device token`() = runTest {
        val repository = repository(backend())

        repository.alarms()

        assertEquals(null, recorded.first().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `every device call carries the stored device token as a bearer header`() = runTest {
        registration.store("token-7")
        val repository = repository(backend(knownTokens = mutableSetOf("token-7"), alarmsJson = "[$dailyJson]"))

        repository.alarms()
        repository.alarm("daily-1")
        repository.create(draft)
        repository.update("daily-1", draft)
        repository.delete("daily-1")
        repository.updateToken("fcm-token-2")

        assertEquals(
            listOf(
                "GET /devices/me/alarms",
                "GET /devices/me/alarms",
                "POST /devices/me/alarms",
                "PUT /devices/me/alarms/daily-1",
                "DELETE /devices/me/alarms/daily-1",
                "PUT /devices/me/fcm-token",
            ),
            recorded.map { "${it.method.value} ${it.url.encodedPath}" },
        )
        recorded.forEach { request ->
            assertEquals("Bearer token-7", request.headers[HttpHeaders.Authorization], request.describe())
        }
    }

    @Test
    fun `later calls reuse the stored token without registering again`() = runTest {
        val repository = repository(backend())

        repository.alarms()
        repository.alarms()

        assertEquals(
            listOf("POST /devices", "GET /devices/me/alarms [token-1]", "GET /devices/me/alarms [token-1]"),
            recorded.map { it.describe() },
        )
        assertEquals(1, pushTokens.callCount)
        assertEquals(listOf("token-1"), registration.storedTokens)
    }

    @Test
    fun `a token stored earlier is used without registering`() = runTest {
        registration.store("token-7")
        val repository = repository(backend(knownTokens = mutableSetOf("token-7")))

        repository.alarms()

        assertEquals(listOf("GET /devices/me/alarms [token-7]"), recorded.map { it.describe() })
        assertEquals(0, pushTokens.callCount)
    }

    @Test
    fun `updateToken with a stored token sends PUT fcm-token with the new push address`() = runTest {
        registration.store("token-7")
        val repository = repository(backend(knownTokens = mutableSetOf("token-7")))

        val result = repository.updateToken("fcm-token-2")

        assertIs<Result.Success<Unit>>(result)
        assertEquals(listOf("PUT /devices/me/fcm-token [token-7]"), recorded.map { it.describe() })
        val body = Json.parseToJsonElement((recorded.single().body as TextContent).text).jsonObject
        assertEquals("fcm-token-2", body.getValue("fcmToken").jsonPrimitive.content)
        assertEquals("token-7", registration.stored)
    }

    @Test
    fun `updateToken without a stored token makes no request and does not register`() = runTest {
        val repository = repository(backend())

        val result = repository.updateToken("fcm-token-2")

        assertIs<Result.Success<Unit>>(result)
        assertTrue(recorded.isEmpty())
        assertEquals(0, pushTokens.callCount)
        assertEquals(null, registration.stored)
    }

    @Test
    fun `updateToken on a 401 drops the token so the next call registers afresh`() = runTest {
        registration.store("forgotten")
        val repository = repository(backend())

        val result = repository.updateToken("fcm-token-2")
        repository.alarms()

        assertIs<Result.Success<Unit>>(result)
        assertEquals(
            listOf("PUT /devices/me/fcm-token [forgotten]", "POST /devices", "GET /devices/me/alarms [token-1]"),
            recorded.map { it.describe() },
        )
        assertEquals("token-1", registration.stored)
    }

    @Test
    fun `updateToken on a 401 keeps a token stored since by another call`() = runTest {
        registration.store("forgotten")
        val repository = repository {
            // Another call re-registers while this update is on its way.
            registration.store("token-new")
            respondError(HttpStatusCode.Unauthorized)
        }

        val result = repository.updateToken("fcm-token-2")

        assertIs<Result.Success<Unit>>(result)
        assertEquals(listOf("PUT /devices/me/fcm-token [forgotten]"), recorded.map { it.describe() })
        assertEquals("token-new", registration.stored)
        assertEquals(0, registration.clearCount)
    }

    @Test
    fun `updateToken fails on a server error and keeps the stored token`() = runTest {
        registration.store("token-7")
        val repository = repository { respondError(HttpStatusCode.InternalServerError) }

        val result = repository.updateToken("fcm-token-2")

        assertIs<Result.Failure>(result)
        assertEquals("token-7", registration.stored)
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
    fun `a 401 for the stored token clears it then registers once and retries`() = runTest {
        registration.store("forgotten")
        val repository = repository(backend(alarmsJson = "[$dailyJson]"))

        val result = repository.alarms()

        assertEquals(listOf(dailyAlarm()), assertIs<Result.Success<List<Alarm>>>(result).data)
        assertEquals(
            listOf("GET /devices/me/alarms [forgotten]", "POST /devices", "GET /devices/me/alarms [token-1]"),
            recorded.map { it.describe() },
        )
        assertEquals(1, registration.clearCount)
        assertEquals("token-1", registration.stored)
    }

    @Test
    fun `a second 401 after re-registering fails instead of registering again`() = runTest {
        val repository = repository { request ->
            if (request.method == HttpMethod.Post) {
                respond("""{"deviceToken":"token-1"}""", HttpStatusCode.Created, jsonHeaders)
            } else {
                respondError(HttpStatusCode.Unauthorized)
            }
        }
        registration.store("forgotten")

        val result = repository.alarms()

        assertIs<UnknownDeviceException>(assertIs<Result.Failure>(result).exception)
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
        registration.store("token-1")
        val repository = repository { respondError(HttpStatusCode.InternalServerError) }

        assertIs<Result.Failure>(repository.alarms())
        assertEquals(1, recorded.size)
        assertEquals(0, registration.clearCount)
    }

    @Test
    fun `a 404 on the list is not taken for an unknown device`() = runTest {
        registration.store("token-1")
        val repository = repository { respondError(HttpStatusCode.NotFound) }

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
        registration.store("token-1")
        val repository = repository(backend(knownTokens = mutableSetOf("token-1")))

        val result = repository.create(draft)

        val created = assertIs<Result.Success<Alarm>>(result).data
        assertEquals("created-1", created.id)
        assertEquals(draft.stationAbbr, created.stationAbbr)
        assertEquals(draft.species, created.species)
        assertEquals(draft.schedule, created.schedule)
        assertEquals(listOf("POST /devices/me/alarms [token-1]"), recorded.map { it.describe() })
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
        assertEquals(listOf("POST /devices", "POST /devices/me/alarms [token-1]"), recorded.map { it.describe() })
    }

    @Test
    fun `create re-registers once and retries on a 401`() = runTest {
        registration.store("forgotten")
        val repository = repository(backend())

        assertIs<Result.Success<Alarm>>(repository.create(draft))
        assertEquals(
            listOf("POST /devices/me/alarms [forgotten]", "POST /devices", "POST /devices/me/alarms [token-1]"),
            recorded.map { it.describe() },
        )
    }

    @Test
    fun `a 400 on create fails with InvalidAlarm carrying the backend's reason`() = runTest {
        registration.store("token-1")
        val repository = repository {
            respond("""{"error":"Select at least one day"}""", HttpStatusCode.BadRequest, jsonHeaders)
        }

        val failure = assertIs<Result.Failure>(repository.create(draft))

        assertEquals("Select at least one day", assertIs<InvalidAlarmException>(failure.exception).message)
        assertEquals(0, registration.clearCount)
    }

    @Test
    fun `a 409 on create fails with AlarmLimitReached without re-registering`() = runTest {
        registration.store("token-1")
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
        registration.store("token-1")
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
        registration.store("token-1")
        val repository = repository(backend(knownTokens = mutableSetOf("token-1"), alarmsJson = "[$dailyJson, $thresholdJson]"))

        assertEquals(thresholdAlarm(), assertIs<Result.Success<Alarm>>(repository.alarm("threshold-1")).data)
    }

    @Test
    fun `alarm fails with AlarmNotFound for an id the device does not have`() = runTest {
        registration.store("token-1")
        val repository = repository(backend(knownTokens = mutableSetOf("token-1"), alarmsJson = "[$dailyJson]"))

        val failure = assertIs<Result.Failure>(repository.alarm("gone"))

        assertIs<AlarmNotFoundException>(failure.exception)
    }

    @Test
    fun `update puts the draft and returns the stored alarm`() = runTest {
        registration.store("token-1")
        val repository = repository(backend(knownTokens = mutableSetOf("token-1")))

        val updated = assertIs<Result.Success<Alarm>>(repository.update("daily-1", draft)).data

        assertEquals("daily-1", updated.id)
        assertEquals(draft.schedule, updated.schedule)
        assertEquals(listOf("PUT /devices/me/alarms/daily-1 [token-1]"), recorded.map { it.describe() })
        assertEquals(
            Json.parseToJsonElement(draft.toInputDto().let { Json.encodeToString(it) }),
            Json.parseToJsonElement((recorded.single().body as TextContent).text),
        )
    }

    @Test
    fun `update re-registers once and retries on a 401`() = runTest {
        registration.store("forgotten")
        val repository = repository(backend())

        assertIs<Result.Success<Alarm>>(repository.update("daily-1", draft))
        assertEquals(
            listOf(
                "PUT /devices/me/alarms/daily-1 [forgotten]",
                "POST /devices",
                "PUT /devices/me/alarms/daily-1 [token-1]",
            ),
            recorded.map { it.describe() },
        )
        assertEquals(1, registration.clearCount)
    }

    @Test
    fun `delete sends a DELETE and succeeds on 204`() = runTest {
        registration.store("token-1")
        val repository = repository(backend(knownTokens = mutableSetOf("token-1")))

        assertIs<Result.Success<Unit>>(repository.delete("daily-1"))
        assertEquals(listOf("DELETE /devices/me/alarms/daily-1 [token-1]"), recorded.map { it.describe() })
    }

    @Test
    fun `delete re-registers once and retries on a 401`() = runTest {
        registration.store("forgotten")
        val repository = repository(backend())

        assertIs<Result.Success<Unit>>(repository.delete("daily-1"))
        assertEquals(
            listOf(
                "DELETE /devices/me/alarms/daily-1 [forgotten]",
                "POST /devices",
                "DELETE /devices/me/alarms/daily-1 [token-1]",
            ),
            recorded.map { it.describe() },
        )
        assertEquals(1, registration.clearCount)
    }

    @Test
    fun `a single-alarm 404 fails with AlarmNotFound with no list request and no registration`() = runTest {
        registration.store("token-1")
        val repository = repository(backend(knownTokens = mutableSetOf("token-1"), alarmIds = emptySet()))

        val updateFailure = assertIs<Result.Failure>(repository.update("gone", draft))
        val deleteFailure = assertIs<Result.Failure>(repository.delete("gone"))

        assertIs<AlarmNotFoundException>(updateFailure.exception)
        assertIs<AlarmNotFoundException>(deleteFailure.exception)
        assertEquals(
            listOf("PUT /devices/me/alarms/gone [token-1]", "DELETE /devices/me/alarms/gone [token-1]"),
            recorded.map { it.describe() },
        )
        assertEquals(0, registration.clearCount)
        assertEquals(0, pushTokens.callCount)
    }

    @Test
    fun `alarm re-registers once and retries on a 401`() = runTest {
        registration.store("forgotten")
        val repository = repository(backend(alarmsJson = "[$dailyJson]"))

        assertEquals(dailyAlarm(), assertIs<Result.Success<Alarm>>(repository.alarm("daily-1")).data)
        assertEquals(
            listOf("GET /devices/me/alarms [forgotten]", "POST /devices", "GET /devices/me/alarms [token-1]"),
            recorded.map { it.describe() },
        )
    }

    @Test
    fun `a 400 on update fails with InvalidAlarm carrying the backend's reason`() = runTest {
        registration.store("token-1")
        val repository = repository {
            respond("""{"error":"Select at least one day"}""", HttpStatusCode.BadRequest, jsonHeaders)
        }

        val failure = assertIs<Result.Failure>(repository.update("daily-1", draft))

        assertEquals("Select at least one day", assertIs<InvalidAlarmException>(failure.exception).message)
    }
}
