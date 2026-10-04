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
import io.ktor.client.request.get
import io.ktor.client.request.post
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

    @Test
    fun `a device does not see another device's alarms`() = testApplication {
        installApp()
        val mine = register()
        val theirs = DeviceId(register())
        database.insertAlarm(dailyAlarm(theirs), createdAtMillis = 1)

        val body = client.get("/devices/$mine/alarms").bodyAsText()

        assertEquals(JsonArray(emptyList()), Json.parseToJsonElement(body))
    }
}
