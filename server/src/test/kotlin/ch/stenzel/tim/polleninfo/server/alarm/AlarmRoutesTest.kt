package ch.stenzel.tim.polleninfo.server.alarm

import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmId
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import ch.stenzel.tim.polleninfo.server.alarm.model.RegisterDeviceResponse
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedAlarmStore
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedDeviceStore
import ch.stenzel.tim.polleninfo.server.alarm.store.PollenInfoDatabase
import ch.stenzel.tim.polleninfo.server.alarm.store.dailyAlarm
import ch.stenzel.tim.polleninfo.server.alarm.store.insertAlarm
import ch.stenzel.tim.polleninfo.server.alarm.store.thresholdAlarm
import ch.stenzel.tim.polleninfo.server.plugins.configureRouting
import ch.stenzel.tim.polleninfo.server.plugins.configureSerialization
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AlarmRoutesTest {

    private val database = PollenInfoDatabase.inMemory()
    private val devices = ExposedDeviceStore(database)
    private val alarms = ExposedAlarmStore(database)

    private fun ApplicationTestBuilder.installApp() {
        application {
            configureSerialization()
            configureRouting(database = database, devices = devices, alarms = alarms)
        }
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) { json() }
    }

    private suspend fun ApplicationTestBuilder.register(): String {
        val response = client.post("/devices") {
            contentType(ContentType.Application.Json)
            setBody("""{"fcmToken":"token-1"}""")
        }
        return Json.decodeFromString<RegisterDeviceResponse>(response.bodyAsText()).deviceId
    }

    @Test
    fun `POST devices returns 201 with a 22 character id`() = testApplication {
        installApp()

        val response = jsonClient().post("/devices") {
            contentType(ContentType.Application.Json)
            setBody("""{"fcmToken":"token-1"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val deviceId = response.body<RegisterDeviceResponse>().deviceId
        assertEquals(22, deviceId.length)
        assertTrue(devices.exists(DeviceId(deviceId)))
    }

    @Test
    fun `PUT token returns 204 for a known device and stores the new token`() = testApplication {
        installApp()
        val deviceId = register()
        database.insertAlarm(dailyAlarm(DeviceId(deviceId)), createdAtMillis = 1)

        val response = client.put("/devices/$deviceId/token") {
            contentType(ContentType.Application.Json)
            setBody("""{"fcmToken":"token-2"}""")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals(listOf("token-2"), alarms.enabledWithDeliverableDevice().map { it.fcmToken })
    }

    @Test
    fun `PUT token returns 404 for an unknown device`() = testApplication {
        installApp()

        val response = client.put("/devices/never-registered/token") {
            contentType(ContentType.Application.Json)
            setBody("""{"fcmToken":"token-2"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT token with a blank or missing token returns 400`() = testApplication {
        installApp()
        val deviceId = register()

        listOf("""{"fcmToken":" "}""", """{}""").forEach { body ->
            val response = client.put("/devices/$deviceId/token") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            assertEquals(HttpStatusCode.BadRequest, response.status, body)
            assertTrue(Json.parseToJsonElement(response.bodyAsText()).jsonObject.containsKey("error"))
        }
    }

    @Test
    fun `POST devices without a token returns 400 with an error message`() = testApplication {
        installApp()

        val response = client.post("/devices") {
            contentType(ContentType.Application.Json)
            setBody("""{}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(Json.parseToJsonElement(response.bodyAsText()).jsonObject.containsKey("error"))
    }

    @Test
    fun `POST devices with a blank token returns 400`() = testApplication {
        installApp()

        val response = client.post("/devices") {
            contentType(ContentType.Application.Json)
            setBody("""{"fcmToken":"  "}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET alarms returns 200 with an empty list for a new device`() = testApplication {
        installApp()
        val deviceId = register()

        val response = client.get("/devices/$deviceId/alarms")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(JsonArray(emptyList()), Json.parseToJsonElement(response.bodyAsText()))
    }

    @Test
    fun `GET alarms returns 404 for an unknown device`() = testApplication {
        installApp()

        assertEquals(HttpStatusCode.NotFound, client.get("/devices/never-registered/alarms").status)
    }

    @Test
    fun `an alarm of each schedule variant serialises with its type discriminator`() = testApplication {
        installApp()
        val deviceId = DeviceId(register())
        database.insertAlarm(dailyAlarm(deviceId, AlarmId("daily-1")), createdAtMillis = 1)
        database.insertAlarm(thresholdAlarm(deviceId, AlarmId("threshold-1")), createdAtMillis = 2)

        val body = client.get("/devices/${deviceId.value}/alarms").bodyAsText()

        val (daily, threshold) = Json.parseToJsonElement(body).jsonArray.map { it.jsonObject }
        assertEquals(
            Json.parseToJsonElement(
                """
                { "id": "daily-1", "enabled": true, "stationAbbr": "PZH",
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
        assertEquals("threshold-1", threshold.getValue("id").jsonPrimitive.content)
        assertEquals("PBE", threshold.getValue("stationAbbr").jsonPrimitive.content)
        assertEquals("false", threshold.getValue("enabled").jsonPrimitive.content)
    }

    private val validDailyJson = """
        { "enabled": true, "stationAbbr": "PBE",
          "species": ["BIRCH", "GRASSES"], "minSeverity": "MODERATE",
          "days": ["SATURDAY", "SUNDAY"],
          "schedule": { "type": "daily", "at": "07:30" } }
    """.trimIndent()

    private suspend fun ApplicationTestBuilder.postAlarm(deviceId: String, body: String) =
        client.post("/devices/$deviceId/alarms") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    @Test
    fun `a valid POST returns 201 with the stored alarm`() = testApplication {
        installApp()
        val deviceId = register()

        val response = postAlarm(deviceId, validDailyJson)

        assertEquals(HttpStatusCode.Created, response.status)
        val created = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(created.getValue("id").jsonPrimitive.content.isNotBlank())
        assertEquals(Json.parseToJsonElement(validDailyJson).jsonObject, JsonObject(created - "id"))
    }

    @Test
    fun `a valid threshold POST returns 201 with the stored alarm`() = testApplication {
        installApp()
        val deviceId = register()
        val thresholdJson = validDailyJson
            .replace("\"MODERATE\"", "\"HIGH\"")
            .replace("""{ "type": "daily", "at": "07:30" }""", """{ "type": "threshold", "from": "07:00", "until": "21:00" }""")

        val response = postAlarm(deviceId, thresholdJson)

        assertEquals(HttpStatusCode.Created, response.status)
        val created = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(Json.parseToJsonElement(thresholdJson).jsonObject, JsonObject(created - "id"))
    }

    @Test
    fun `a threshold POST whose window does not open returns 400`() = testApplication {
        installApp()
        val deviceId = register()
        val body = validDailyJson
            .replace("\"MODERATE\"", "\"HIGH\"")
            .replace("""{ "type": "daily", "at": "07:30" }""", """{ "type": "threshold", "from": "21:00", "until": "21:00" }""")

        val response = postAlarm(deviceId, body)

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("error")
        assertEquals("End time must be after the start time", error.jsonPrimitive.content)
    }

    @Test
    fun `a created alarm appears in the next GET`() = testApplication {
        installApp()
        val deviceId = register()

        val created = Json.parseToJsonElement(postAlarm(deviceId, validDailyJson).bodyAsText())
        val listed = Json.parseToJsonElement(client.get("/devices/$deviceId/alarms").bodyAsText())

        assertEquals(JsonArray(listOf(created)), listed)
    }

    @Test
    fun `alarms created one after another are listed in creation order`() = testApplication {
        installApp()
        val deviceId = register()

        val ids = listOf("06:00", "05:00", "07:00").map { time ->
            val body = validDailyJson.replace("07:30", time)
            Json.parseToJsonElement(postAlarm(deviceId, body).bodyAsText()).jsonObject.getValue("id")
        }
        val listed = Json.parseToJsonElement(client.get("/devices/$deviceId/alarms").bodyAsText())

        assertEquals(ids, listed.jsonArray.map { it.jsonObject.getValue("id") })
    }

    @Test
    fun `a lowercase station abbreviation is stored in its official form`() = testApplication {
        installApp()
        val deviceId = register()

        val response = postAlarm(deviceId, validDailyJson.replace("\"PBE\"", "\"pbe\""))

        assertEquals(HttpStatusCode.Created, response.status)
        val created = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("PBE", created.getValue("stationAbbr").jsonPrimitive.content)
    }

    @Test
    fun `an invalid POST returns 400 with an error message and stores nothing`() = testApplication {
        installApp()
        val deviceId = register()

        val response = postAlarm(deviceId, validDailyJson.replace("[\"BIRCH\", \"GRASSES\"]", "[]"))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("error")
        assertEquals("Select at least one pollen type", error.jsonPrimitive.content)
        assertEquals(JsonArray(emptyList()), Json.parseToJsonElement(client.get("/devices/$deviceId/alarms").bodyAsText()))
    }

    @Test
    fun `a malformed time returns 400`() = testApplication {
        installApp()
        val deviceId = register()

        assertEquals(HttpStatusCode.BadRequest, postAlarm(deviceId, validDailyJson.replace("07:30", "7.30")).status)
    }

    @Test
    fun `a malformed body returns 400 with an error message`() = testApplication {
        installApp()
        val deviceId = register()

        val response = postAlarm(deviceId, """{"stationAbbr":"PZH"}""")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(Json.parseToJsonElement(response.bodyAsText()).jsonObject.containsKey("error"))
    }

    @Test
    fun `an unknown schedule type returns 400`() = testApplication {
        installApp()
        val deviceId = register()

        val response = postAlarm(deviceId, validDailyJson.replace("\"daily\"", "\"weekly\""))

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST alarms for an unknown device returns 404`() = testApplication {
        installApp()

        assertEquals(HttpStatusCode.NotFound, postAlarm("never-registered", validDailyJson).status)
    }

    @Test
    fun `a POST at ten alarms returns 409 with an error message and stores nothing`() = testApplication {
        installApp()
        val deviceId = register()
        repeat(10) { assertEquals(HttpStatusCode.Created, postAlarm(deviceId, validDailyJson).status) }

        val response = postAlarm(deviceId, validDailyJson)

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(Json.parseToJsonElement(response.bodyAsText()).jsonObject.containsKey("error"))
        assertEquals(10, Json.parseToJsonElement(client.get("/devices/$deviceId/alarms").bodyAsText()).jsonArray.size)
    }

    @Test
    fun `a device does not see another device's alarms`() = testApplication {
        installApp()
        val mine = register()
        val theirs = DeviceId(register())
        database.insertAlarm(dailyAlarm(theirs), createdAtMillis = 1)

        val body = client.get("/devices/$mine/alarms").bodyAsText()

        assertEquals(JsonArray(emptyList()), Json.parseToJsonElement(body))
    }

    private suspend fun ApplicationTestBuilder.putAlarm(deviceId: String, alarmId: String, body: String) =
        client.put("/devices/$deviceId/alarms/$alarmId") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private suspend fun ApplicationTestBuilder.createdId(deviceId: String): String =
        Json.parseToJsonElement(postAlarm(deviceId, validDailyJson).bodyAsText()).jsonObject
            .getValue("id").jsonPrimitive.content

    @Test
    fun `PUT returns 200 with the updated alarm and the next GET lists it`() = testApplication {
        installApp()
        val deviceId = register()
        val alarmId = createdId(deviceId)
        val changed = validDailyJson.replace("true", "false").replace("07:30", "18:15")

        val response = putAlarm(deviceId, alarmId, changed)

        assertEquals(HttpStatusCode.OK, response.status)
        val updated = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(alarmId, updated.getValue("id").jsonPrimitive.content)
        assertEquals(Json.parseToJsonElement(changed).jsonObject, JsonObject(updated - "id"))
        val listed = Json.parseToJsonElement(client.get("/devices/$deviceId/alarms").bodyAsText())
        assertEquals(JsonArray(listOf(updated)), listed)
    }

    @Test
    fun `an invalid PUT returns 400 with an error message and changes nothing`() = testApplication {
        installApp()
        val deviceId = register()
        val alarmId = createdId(deviceId)
        val before = client.get("/devices/$deviceId/alarms").bodyAsText()

        val response = putAlarm(deviceId, alarmId, validDailyJson.replace("[\"SATURDAY\", \"SUNDAY\"]", "[]"))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("error")
        assertEquals("Select at least one day", error.jsonPrimitive.content)
        assertEquals(before, client.get("/devices/$deviceId/alarms").bodyAsText())
    }

    @Test
    fun `a malformed PUT returns 400`() = testApplication {
        installApp()
        val deviceId = register()

        assertEquals(HttpStatusCode.BadRequest, putAlarm(deviceId, createdId(deviceId), """{"stationAbbr":"PZH"}""").status)
    }

    @Test
    fun `PUT on another device's alarm returns 404 and leaves it unchanged`() = testApplication {
        installApp()
        val owner = register()
        val alarmId = createdId(owner)
        val before = client.get("/devices/$owner/alarms").bodyAsText()

        val response = putAlarm(register(), alarmId, validDailyJson.replace("07:30", "18:15"))

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(before, client.get("/devices/$owner/alarms").bodyAsText())
    }

    @Test
    fun `PUT on an unknown alarm or for an unknown device returns 404`() = testApplication {
        installApp()
        val deviceId = register()
        val alarmId = createdId(deviceId)

        assertEquals(HttpStatusCode.NotFound, putAlarm(deviceId, "never-created", validDailyJson).status)
        assertEquals(HttpStatusCode.NotFound, putAlarm("never-registered", alarmId, validDailyJson).status)
    }

    @Test
    fun `DELETE returns 204 and the alarm is gone from GET`() = testApplication {
        installApp()
        val deviceId = register()
        val alarmId = createdId(deviceId)

        val response = client.delete("/devices/$deviceId/alarms/$alarmId")

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals(JsonArray(emptyList()), Json.parseToJsonElement(client.get("/devices/$deviceId/alarms").bodyAsText()))
        assertEquals(HttpStatusCode.NotFound, client.delete("/devices/$deviceId/alarms/$alarmId").status)
    }

    @Test
    fun `DELETE on another device's alarm returns 404 and keeps it`() = testApplication {
        installApp()
        val owner = register()
        val alarmId = createdId(owner)

        val response = client.delete("/devices/${register()}/alarms/$alarmId")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(1, Json.parseToJsonElement(client.get("/devices/$owner/alarms").bodyAsText()).jsonArray.size)
    }

    @Test
    fun `DELETE for an unknown device returns 404`() = testApplication {
        installApp()
        val alarmId = createdId(register())

        assertEquals(HttpStatusCode.NotFound, client.delete("/devices/never-registered/alarms/$alarmId").status)
    }
}
