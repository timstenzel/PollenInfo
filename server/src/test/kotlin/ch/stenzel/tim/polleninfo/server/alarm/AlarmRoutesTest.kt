package ch.stenzel.tim.polleninfo.server.alarm

import ch.stenzel.tim.polleninfo.server.alarm.domain.DEVICE_TOKEN_LENGTH
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceToken
import ch.stenzel.tim.polleninfo.server.alarm.domain.newDeviceToken
import ch.stenzel.tim.polleninfo.server.alarm.model.RegisterDeviceResponse
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedAlarmStore
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedDeviceStore
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedNotificationLog
import ch.stenzel.tim.polleninfo.server.alarm.store.TestPostgres
import ch.stenzel.tim.polleninfo.server.alarm.store.alarmId
import ch.stenzel.tim.polleninfo.server.alarm.store.dailyAlarm
import ch.stenzel.tim.polleninfo.server.alarm.store.insertAlarm
import ch.stenzel.tim.polleninfo.server.alarm.store.thresholdAlarm
import ch.stenzel.tim.polleninfo.server.plugins.configureAlarmRouting
import ch.stenzel.tim.polleninfo.server.plugins.configureSecurity
import ch.stenzel.tim.polleninfo.server.plugins.configureSerialization
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AlarmRoutesTest {

    private val database = TestPostgres.cleanDatabase()
    private val devices = ExposedDeviceStore(database)
    private val alarms = ExposedAlarmStore(database)

    private fun ApplicationTestBuilder.installApp() {
        application {
            configureSerialization()
            configureSecurity()
            configureAlarmRouting(devices = devices, alarms = alarms)
        }
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) { json() }
    }

    /** Registers a device and returns the device token it was issued. */
    private suspend fun ApplicationTestBuilder.register(): String {
        val response = client.post("/devices") {
            contentType(ContentType.Application.Json)
            setBody("""{"fcmToken":"token-1"}""")
        }
        return Json.decodeFromString<RegisterDeviceResponse>(response.bodyAsText()).deviceToken
    }

    /** The internal id behind [token], for inserting alarms directly. */
    private suspend fun deviceIdOf(token: String): DeviceId = checkNotNull(devices.authenticate(DeviceToken(token)))

    private suspend fun ApplicationTestBuilder.getAlarms(token: String) =
        client.get("/devices/me/alarms") { bearerAuth(token) }

    private fun HttpRequestBuilder.jsonBody(body: String) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private fun assertUnauthorized(response: HttpResponse) {
        assertEquals(HttpStatusCode.Unauthorized, response.status, response.call.request.url.encodedPath)
        assertTrue(
            response.headers[HttpHeaders.WWWAuthenticate].orEmpty().startsWith("Bearer"),
            "WWW-Authenticate: ${response.headers[HttpHeaders.WWWAuthenticate]}",
        )
    }

    // --- Registration ---

    @Test
    fun `POST devices returns 201 with a 43 character token that authenticates`() = testApplication {
        installApp()

        val response = jsonClient().post("/devices") { jsonBody("""{"fcmToken":"token-1"}""") }

        assertEquals(HttpStatusCode.Created, response.status)
        val token = response.body<RegisterDeviceResponse>().deviceToken
        assertEquals(DEVICE_TOKEN_LENGTH, token.length)
        assertTrue(token.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertEquals(HttpStatusCode.OK, getAlarms(token).status)
    }

    @Test
    fun `two registrations get different tokens`() = testApplication {
        installApp()

        assertTrue(register() != register())
    }

    @Test
    fun `POST devices without a push address returns 400 with an error message`() = testApplication {
        installApp()

        val response = client.post("/devices") { jsonBody("""{}""") }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(Json.parseToJsonElement(response.bodyAsText()).jsonObject.containsKey("error"))
    }

    @Test
    fun `POST devices with a blank push address returns 400`() = testApplication {
        installApp()

        assertEquals(HttpStatusCode.BadRequest, client.post("/devices") { jsonBody("""{"fcmToken":"  "}""") }.status)
    }

    // --- Authentication ---

    /** Every route under /devices/me, each with a body that would be valid. */
    private val deviceCalls: List<Pair<String, suspend ApplicationTestBuilder.(HttpRequestBuilder.() -> Unit) -> HttpResponse>> =
        listOf(
            "DELETE /devices/me" to { auth -> client.delete("/devices/me") { auth() } },
            "PUT /devices/me/fcm-token" to { auth ->
                client.put("/devices/me/fcm-token") { auth(); jsonBody("""{"fcmToken":"token-2"}""") }
            },
            "GET /devices/me/alarms" to { auth -> client.get("/devices/me/alarms") { auth() } },
            "POST /devices/me/alarms" to { auth -> client.post("/devices/me/alarms") { auth(); jsonBody(validDailyJson) } },
            "PUT /devices/me/alarms/{id}" to { auth ->
                client.put("/devices/me/alarms/${UUID.randomUUID()}") { auth(); jsonBody(validDailyJson) }
            },
            "DELETE /devices/me/alarms/{id}" to { auth -> client.delete("/devices/me/alarms/${UUID.randomUUID()}") { auth() } },
        )

    @Test
    fun `every device call without a token returns 401 with a Bearer challenge`() = testApplication {
        installApp()

        deviceCalls.forEach { (_, call) -> assertUnauthorized(call {}) }
    }

    @Test
    fun `every device call with an unknown token returns 401`() = testApplication {
        installApp()
        register()
        val unknown = newDeviceToken().value

        deviceCalls.forEach { (name, call) ->
            assertEquals(HttpStatusCode.Unauthorized, call { bearerAuth(unknown) }.status, name)
        }
    }

    @Test
    fun `a malformed token or another scheme returns 401`() = testApplication {
        installApp()
        val token = register()

        listOf("Bearer", "Bearer ", "Bearer not a token", "Bearer ${token}x", "Bearer ${token.dropLast(1)}", "Basic $token", token)
            .forEach { value ->
                val response = client.get("/devices/me/alarms") { header(HttpHeaders.Authorization, value) }
                assertEquals(HttpStatusCode.Unauthorized, response.status, value)
            }
    }

    @Test
    fun `an invalid body without a token returns 401 rather than 400`() = testApplication {
        installApp()

        assertUnauthorized(client.post("/devices/me/alarms") { jsonBody("""{"stationAbbr":"PZH"}""") })
        assertUnauthorized(client.put("/devices/me/alarms/${UUID.randomUUID()}") { jsonBody("not json") })
        assertUnauthorized(client.put("/devices/me/fcm-token") { jsonBody("""{}""") })
    }

    // --- Push address ---

    @Test
    fun `PUT fcm-token returns 204 and stores the new push address`() = testApplication {
        installApp()
        val token = register()
        database.insertAlarm(dailyAlarm(deviceIdOf(token)), createdAtMillis = 1)

        val response = client.put("/devices/me/fcm-token") {
            bearerAuth(token)
            jsonBody("""{"fcmToken":"token-2"}""")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals(listOf("token-2"), alarms.enabledWithDeliverableDevice().map { it.fcmToken })
    }

    @Test
    fun `PUT fcm-token with a blank or missing push address returns 400`() = testApplication {
        installApp()
        val token = register()

        listOf("""{"fcmToken":" "}""", """{}""").forEach { body ->
            val response = client.put("/devices/me/fcm-token") {
                bearerAuth(token)
                jsonBody(body)
            }

            assertEquals(HttpStatusCode.BadRequest, response.status, body)
            assertTrue(Json.parseToJsonElement(response.bodyAsText()).jsonObject.containsKey("error"))
        }
    }

    // --- Erasing a device ---

    @Test
    fun `DELETE devices me returns 204 and removes the device with its alarms and their log`() = testApplication {
        installApp()
        val token = register()
        val deviceId = deviceIdOf(token)
        val alarm = thresholdAlarm(deviceId)
        database.insertAlarm(alarm, createdAtMillis = 1)
        val log = ExposedNotificationLog(database)
        val today = LocalDate.of(2026, 8, 3)
        log.record(alarm.id, setOf(PollenSpecies.HAZEL), today)
        val other = register()

        val response = client.delete("/devices/me") { bearerAuth(token) }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals(emptyList(), alarms.list(deviceId))
        assertEquals(emptySet(), log.notifiedSpecies(alarm.id, today))
        assertNull(devices.authenticate(DeviceToken(token)))
        assertEquals(HttpStatusCode.OK, getAlarms(other).status)
    }

    @Test
    fun `every device call after DELETE devices me returns 401`() = testApplication {
        installApp()
        val token = register()
        client.delete("/devices/me") { bearerAuth(token) }

        deviceCalls.forEach { (name, call) ->
            assertEquals(HttpStatusCode.Unauthorized, call { bearerAuth(token) }.status, name)
        }
    }

    // --- Listing ---

    @Test
    fun `GET alarms returns 200 with an empty list for a new device`() = testApplication {
        installApp()
        val token = register()

        val response = getAlarms(token)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(JsonArray(emptyList()), Json.parseToJsonElement(response.bodyAsText()))
    }

    @Test
    fun `an alarm of each schedule variant serialises with its type discriminator`() = testApplication {
        installApp()
        val token = register()
        val deviceId = deviceIdOf(token)
        database.insertAlarm(dailyAlarm(deviceId, alarmId('1')), createdAtMillis = 1)
        database.insertAlarm(thresholdAlarm(deviceId, alarmId('2')), createdAtMillis = 2)

        val body = getAlarms(token).bodyAsText()

        val (daily, threshold) = Json.parseToJsonElement(body).jsonArray.map { it.jsonObject }
        assertEquals(
            Json.parseToJsonElement(
                """
                { "id": "00000000-0000-0000-0000-000000000001", "enabled": true, "stationAbbr": "PZH",
                  "species": ["BIRCH", "GRASSES"], "minSeverity": "NONE",
                  "days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
                  "schedule": { "type": "daily", "at": "08:00" } }
                """,
            ),
            daily,
        )
        assertEquals(
            Json.parseToJsonElement("""{ "type": "threshold", "from": "07:00", "until": "21:00" }"""),
            threshold.getValue("schedule"),
        )
        assertEquals("00000000-0000-0000-0000-000000000002", threshold.getValue("id").jsonPrimitive.content)
        assertEquals("PBE", threshold.getValue("stationAbbr").jsonPrimitive.content)
        assertEquals("false", threshold.getValue("enabled").jsonPrimitive.content)
    }

    @Test
    fun `a device does not see another device's alarms`() = testApplication {
        installApp()
        val mine = register()
        database.insertAlarm(dailyAlarm(deviceIdOf(register())), createdAtMillis = 1)

        assertEquals(JsonArray(emptyList()), Json.parseToJsonElement(getAlarms(mine).bodyAsText()))
    }

    // --- Creating ---

    private val validDailyJson = """
        { "enabled": true, "stationAbbr": "PBE",
          "species": ["BIRCH", "GRASSES"], "minSeverity": "MODERATE",
          "days": ["SATURDAY", "SUNDAY"],
          "schedule": { "type": "daily", "at": "07:30" } }
    """.trimIndent()

    private suspend fun ApplicationTestBuilder.postAlarm(token: String, body: String) =
        client.post("/devices/me/alarms") {
            bearerAuth(token)
            jsonBody(body)
        }

    @Test
    fun `a valid POST returns 201 with the stored alarm`() = testApplication {
        installApp()
        val token = register()

        val response = postAlarm(token, validDailyJson)

        assertEquals(HttpStatusCode.Created, response.status)
        val created = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(created.getValue("id").jsonPrimitive.content.isNotBlank())
        assertEquals(Json.parseToJsonElement(validDailyJson).jsonObject, JsonObject(created - "id"))
    }

    @Test
    fun `a valid threshold POST returns 201 with the stored alarm`() = testApplication {
        installApp()
        val token = register()
        val thresholdJson = validDailyJson
            .replace("\"MODERATE\"", "\"HIGH\"")
            .replace("""{ "type": "daily", "at": "07:30" }""", """{ "type": "threshold", "from": "07:00", "until": "21:00" }""")

        val response = postAlarm(token, thresholdJson)

        assertEquals(HttpStatusCode.Created, response.status)
        val created = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(Json.parseToJsonElement(thresholdJson).jsonObject, JsonObject(created - "id"))
    }

    @Test
    fun `a threshold POST whose window does not open returns 400`() = testApplication {
        installApp()
        val token = register()
        val body = validDailyJson
            .replace("\"MODERATE\"", "\"HIGH\"")
            .replace("""{ "type": "daily", "at": "07:30" }""", """{ "type": "threshold", "from": "21:00", "until": "21:00" }""")

        val response = postAlarm(token, body)

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("error")
        assertEquals("End time must be after the start time", error.jsonPrimitive.content)
    }

    @Test
    fun `a created alarm appears in the next GET`() = testApplication {
        installApp()
        val token = register()

        val created = Json.parseToJsonElement(postAlarm(token, validDailyJson).bodyAsText())
        val listed = Json.parseToJsonElement(getAlarms(token).bodyAsText())

        assertEquals(JsonArray(listOf(created)), listed)
    }

    @Test
    fun `alarms created one after another are listed in creation order`() = testApplication {
        installApp()
        val token = register()

        val ids = listOf("06:00", "05:00", "07:00").map { time ->
            val body = validDailyJson.replace("07:30", time)
            Json.parseToJsonElement(postAlarm(token, body).bodyAsText()).jsonObject.getValue("id")
        }
        val listed = Json.parseToJsonElement(getAlarms(token).bodyAsText())

        assertEquals(ids, listed.jsonArray.map { it.jsonObject.getValue("id") })
    }

    @Test
    fun `a lowercase station abbreviation is stored in its official form`() = testApplication {
        installApp()
        val token = register()

        val response = postAlarm(token, validDailyJson.replace("\"PBE\"", "\"pbe\""))

        assertEquals(HttpStatusCode.Created, response.status)
        val created = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("PBE", created.getValue("stationAbbr").jsonPrimitive.content)
    }

    @Test
    fun `an invalid POST returns 400 with an error message and stores nothing`() = testApplication {
        installApp()
        val token = register()

        val response = postAlarm(token, validDailyJson.replace("[\"BIRCH\", \"GRASSES\"]", "[]"))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("error")
        assertEquals("Select at least one pollen type", error.jsonPrimitive.content)
        assertEquals(JsonArray(emptyList()), Json.parseToJsonElement(getAlarms(token).bodyAsText()))
    }

    @Test
    fun `a malformed time returns 400`() = testApplication {
        installApp()
        val token = register()

        assertEquals(HttpStatusCode.BadRequest, postAlarm(token, validDailyJson.replace("07:30", "7.30")).status)
    }

    @Test
    fun `a malformed body returns 400 with an error message`() = testApplication {
        installApp()
        val token = register()

        val response = postAlarm(token, """{"stationAbbr":"PZH"}""")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(Json.parseToJsonElement(response.bodyAsText()).jsonObject.containsKey("error"))
    }

    @Test
    fun `an unknown schedule type returns 400`() = testApplication {
        installApp()
        val token = register()

        assertEquals(HttpStatusCode.BadRequest, postAlarm(token, validDailyJson.replace("\"daily\"", "\"weekly\"")).status)
    }

    @Test
    fun `a POST at ten alarms returns 409 with an error message and stores nothing`() = testApplication {
        installApp()
        val token = register()
        repeat(10) { assertEquals(HttpStatusCode.Created, postAlarm(token, validDailyJson).status) }

        val response = postAlarm(token, validDailyJson)

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(Json.parseToJsonElement(response.bodyAsText()).jsonObject.containsKey("error"))
        assertEquals(10, Json.parseToJsonElement(getAlarms(token).bodyAsText()).jsonArray.size)
    }

    // --- Updating and deleting ---

    private suspend fun ApplicationTestBuilder.putAlarm(token: String, alarmId: String, body: String) =
        client.put("/devices/me/alarms/$alarmId") {
            bearerAuth(token)
            jsonBody(body)
        }

    private suspend fun ApplicationTestBuilder.createdId(token: String): String =
        Json.parseToJsonElement(postAlarm(token, validDailyJson).bodyAsText()).jsonObject
            .getValue("id").jsonPrimitive.content

    @Test
    fun `PUT returns 200 with the updated alarm and the next GET lists it`() = testApplication {
        installApp()
        val token = register()
        val alarmId = createdId(token)
        val changed = validDailyJson.replace("true", "false").replace("07:30", "18:15")

        val response = putAlarm(token, alarmId, changed)

        assertEquals(HttpStatusCode.OK, response.status)
        val updated = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(alarmId, updated.getValue("id").jsonPrimitive.content)
        assertEquals(Json.parseToJsonElement(changed).jsonObject, JsonObject(updated - "id"))
        assertEquals(JsonArray(listOf(updated)), Json.parseToJsonElement(getAlarms(token).bodyAsText()))
    }

    @Test
    fun `an invalid PUT returns 400 with an error message and changes nothing`() = testApplication {
        installApp()
        val token = register()
        val alarmId = createdId(token)
        val before = getAlarms(token).bodyAsText()

        val response = putAlarm(token, alarmId, validDailyJson.replace("[\"SATURDAY\", \"SUNDAY\"]", "[]"))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("error")
        assertEquals("Select at least one day", error.jsonPrimitive.content)
        assertEquals(before, getAlarms(token).bodyAsText())
    }

    @Test
    fun `a malformed PUT returns 400`() = testApplication {
        installApp()
        val token = register()

        assertEquals(HttpStatusCode.BadRequest, putAlarm(token, createdId(token), """{"stationAbbr":"PZH"}""").status)
    }

    @Test
    fun `PUT on another device's alarm returns 404 and leaves it unchanged`() = testApplication {
        installApp()
        val owner = register()
        val alarmId = createdId(owner)
        val before = getAlarms(owner).bodyAsText()

        val response = putAlarm(register(), alarmId, validDailyJson.replace("07:30", "18:15"))

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(before, getAlarms(owner).bodyAsText())
    }

    @Test
    fun `PUT on an unknown alarm or an id that is not a UUID returns 404`() = testApplication {
        installApp()
        val token = register()
        createdId(token)

        assertEquals(HttpStatusCode.NotFound, putAlarm(token, "never-created", validDailyJson).status)
        assertEquals(HttpStatusCode.NotFound, putAlarm(token, UUID.randomUUID().toString(), validDailyJson).status)
    }

    @Test
    fun `DELETE returns 204 and the alarm is gone from GET`() = testApplication {
        installApp()
        val token = register()
        val alarmId = createdId(token)

        val response = client.delete("/devices/me/alarms/$alarmId") { bearerAuth(token) }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals(JsonArray(emptyList()), Json.parseToJsonElement(getAlarms(token).bodyAsText()))
        assertEquals(HttpStatusCode.NotFound, client.delete("/devices/me/alarms/$alarmId") { bearerAuth(token) }.status)
    }

    @Test
    fun `DELETE on another device's alarm returns 404 and keeps it`() = testApplication {
        installApp()
        val owner = register()
        val alarmId = createdId(owner)

        val response = client.delete("/devices/me/alarms/$alarmId") { bearerAuth(register()) }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(1, Json.parseToJsonElement(getAlarms(owner).bodyAsText()).jsonArray.size)
    }

    @Test
    fun `DELETE on an id that is not a UUID returns 404`() = testApplication {
        installApp()
        val token = register()
        createdId(token)

        assertEquals(HttpStatusCode.NotFound, client.delete("/devices/me/alarms/not-a-uuid") { bearerAuth(token) }.status)
        assertEquals(1, Json.parseToJsonElement(getAlarms(token).bodyAsText()).jsonArray.size)
    }
}
