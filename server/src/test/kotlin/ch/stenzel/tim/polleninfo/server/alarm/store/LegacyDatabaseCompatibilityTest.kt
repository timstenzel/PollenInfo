package ch.stenzel.tim.polleninfo.server.alarm.store

import ch.stenzel.tim.polleninfo.server.alarm.domain.Alarm
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmId
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSchedule
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSpec
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity.HIGH
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity.LOW
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity.MODERATE
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity.NONE
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity.VERY_HIGH
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies.ALDER
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies.ASH
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies.BEECH
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies.BIRCH
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies.GRASSES
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies.HAZEL
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies.OAK
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalTime
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Opens a database written by the server on **Exposed 0.61** — `fixtures/db/polleninfo-exposed-0.61.db`,
 * whose README records how it was produced — and reads it through the stores alone. This is what
 * proves that an Exposed upgrade still loads an existing production file, so it must keep passing
 * unchanged across every database library update.
 *
 * Each test works on its own temp copy: opening a database creates missing tables, and the tests
 * write to it. The checked-in fixture is never opened.
 */
class LegacyDatabaseCompatibilityTest {

    private val tempDir = Files.createTempDirectory("polleninfo-legacy-db-test")
    private val database = PollenInfoDatabase.file(
        tempDir.resolve("polleninfo.db").also { copy ->
            val fixture = checkNotNull(javaClass.getResourceAsStream("/$FIXTURE")) { "Missing $FIXTURE" }
            fixture.use { Files.copy(it, copy, StandardCopyOption.REPLACE_EXISTING) }
        },
    )
    private val devices = ExposedDeviceStore(database)
    private val alarms = ExposedAlarmStore(database)
    private val log = ExposedNotificationLog(database)

    @OptIn(ExperimentalPathApi::class)
    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `both devices are still registered`() = runTest {
        assertTrue(devices.exists(FULL_DEVICE))
        assertTrue(devices.exists(CLEARED_DEVICE))
    }

    @Test
    fun `the full device's ten alarms read back with every field in creation order`() = runTest {
        assertEquals(FULL_ALARMS, alarms.list(FULL_DEVICE))
    }

    @Test
    fun `the token-less device's alarms read back with every field in creation order`() = runTest {
        assertEquals(CLEARED_ALARMS, alarms.list(CLEARED_DEVICE))
    }

    @Test
    fun `only the enabled alarms of the device with a token are deliverable`() = runTest {
        val deliverable = alarms.enabledWithDeliverableDevice()

        assertEquals(FULL_ALARMS.filter { it.enabled }, deliverable.map { it.alarm })
        assertTrue(deliverable.all { it.fcmToken == FULL_TOKEN })
    }

    @Test
    fun `the device already holding ten alarms cannot create an eleventh`() = runTest {
        val spec = AlarmSpec(
            enabled = true,
            station = PollenStation.ZUERICH,
            species = setOf(BIRCH),
            minSeverity = NONE,
            days = setOf(MONDAY),
            schedule = AlarmSchedule.Daily(LocalTime.of(9, 0)),
        )

        assertEquals(CreateResult.LimitReached, alarms.create(FULL_DEVICE, spec))
        assertEquals(FULL_ALARMS, alarms.list(FULL_DEVICE))
    }

    @Test
    fun `the logged threshold alert's notifications are kept per day`() = runTest {
        assertEquals(setOf(HAZEL, ALDER), log.notifiedSpecies(LOGGED_ALARM, LocalDate.of(2026, 10, 8)))
        assertEquals(setOf(HAZEL), log.notifiedSpecies(LOGGED_ALARM, LocalDate.of(2026, 10, 7)))
    }

    @Test
    fun `deleting the logged threshold alert removes its notification log`() = runTest {
        assertTrue(alarms.delete(FULL_DEVICE, LOGGED_ALARM))

        assertEquals(emptySet(), log.notifiedSpecies(LOGGED_ALARM, LocalDate.of(2026, 10, 8)))
        assertEquals(emptySet(), log.notifiedSpecies(LOGGED_ALARM, LocalDate.of(2026, 10, 7)))
        assertEquals(FULL_ALARMS.filterNot { it.id == LOGGED_ALARM }, alarms.list(FULL_DEVICE))
    }

    private companion object {
        const val FIXTURE = "fixtures/db/polleninfo-exposed-0.61.db"

        // Everything below is what the generator wrote; see the fixture's README.
        const val FULL_TOKEN = "fixture-token-full"
        val FULL_DEVICE = DeviceId("Vn1knWfcI-E27R3-onc2Ug")
        val CLEARED_DEVICE = DeviceId("MGxouTg2e1sA2e3Vu-Tyvw")
        val LOGGED_ALARM = AlarmId("5268dbbe-72fe-4de6-8c66-a4f44f88fd2b")

        private val WEEKDAYS = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)

        val FULL_ALARMS = listOf(
            daily(FULL_DEVICE, "244ee5c5-18d1-4669-8b41-747fb5ec97f8", true, PollenStation.ZUERICH, setOf(BIRCH, GRASSES), NONE, WEEKDAYS, "08:00"),
            threshold(FULL_DEVICE, "5268dbbe-72fe-4de6-8c66-a4f44f88fd2b", PollenStation.BERN, setOf(HAZEL, ALDER), HIGH, DayOfWeek.entries.toSet(), "07:00", "21:00"),
            daily(FULL_DEVICE, "0635cdf3-e65e-42a7-832b-12809cef47a7", false, PollenStation.LUGANO, setOf(ASH), MODERATE, setOf(SATURDAY, SUNDAY), "06:30"),
            threshold(FULL_DEVICE, "4b782028-2223-4187-8802-efd3fd59023e", PollenStation.GENEVE, setOf(GRASSES), VERY_HIGH, setOf(MONDAY, WEDNESDAY, FRIDAY), "09:15", "17:45"),
            daily(FULL_DEVICE, "79397c1b-5530-431f-945e-6be6363e068e", true, PollenStation.BASEL, setOf(OAK), LOW, setOf(TUESDAY), "12:00"),
            daily(FULL_DEVICE, "d58d7b07-e892-4b24-afc0-537f7569fbeb", true, PollenStation.DAVOS, PollenSpecies.entries.toSet(), NONE, DayOfWeek.entries.toSet(), "18:05"),
            daily(FULL_DEVICE, "0da0301e-f758-4eb8-ae97-fc3953695087", true, PollenStation.SION, setOf(BEECH), HIGH, setOf(THURSDAY), "23:59"),
            daily(FULL_DEVICE, "fafac437-1bea-441a-8db5-8045c8d2816a", true, PollenStation.MUENSTERLINGEN, setOf(BIRCH), VERY_HIGH, setOf(SUNDAY), "00:00"),
            daily(FULL_DEVICE, "264ed258-9f5a-4011-aade-a0847a07d39d", true, PollenStation.LUZERN, setOf(ALDER, HAZEL), MODERATE, setOf(MONDAY), "07:45"),
            daily(FULL_DEVICE, "0850eb45-fed6-447a-955d-b30a55304b90", true, PollenStation.NEUCHATEL, setOf(GRASSES), NONE, setOf(SATURDAY), "10:10"),
        )

        val CLEARED_ALARMS = listOf(
            daily(CLEARED_DEVICE, "ce133e02-b57f-4333-9d39-ecd9a5d1db19", true, PollenStation.PAYERNE, setOf(BIRCH), NONE, setOf(MONDAY), "08:00"),
            threshold(CLEARED_DEVICE, "c333bef1-ca1c-4742-a241-d07c7323b33e", PollenStation.LAUSANNE, setOf(GRASSES, OAK), MODERATE, setOf(TUESDAY, THURSDAY), "06:00", "20:00"),
        )

        fun daily(
            device: DeviceId,
            id: String,
            enabled: Boolean,
            station: PollenStation,
            species: Set<PollenSpecies>,
            minSeverity: PollenSeverity,
            days: Set<DayOfWeek>,
            at: String,
        ) = Alarm(AlarmId(id), device, enabled, station, species, minSeverity, days, AlarmSchedule.Daily(LocalTime.parse(at)))

        fun threshold(
            device: DeviceId,
            id: String,
            station: PollenStation,
            species: Set<PollenSpecies>,
            minSeverity: PollenSeverity,
            days: Set<DayOfWeek>,
            from: String,
            until: String,
        ) = Alarm(
            AlarmId(id), device, true, station, species, minSeverity, days,
            AlarmSchedule.Threshold(LocalTime.parse(from), LocalTime.parse(until)),
        )
    }
}
