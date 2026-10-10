package ch.stenzel.tim.polleninfo.server.alarm.store

import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmId
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSchedule
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSpec
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import ch.stenzel.tim.polleninfo.server.alarm.domain.hash
import ch.stenzel.tim.polleninfo.server.alarm.domain.newDeviceToken
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import ch.stenzel.tim.polleninfo.server.pollen.measurement.MutableClock
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.assertFailsWith

class ExposedStoresTest {

    private val database = TestPostgres.cleanDatabase()
    private val devices = ExposedDeviceStore(database)
    private val alarms = ExposedAlarmStore(database)
    private val log = ExposedNotificationLog(database)

    // --- Devices ---

    @Test
    fun `a registered device authenticates with the token it was issued`() = runTest {
        val token = devices.register("token-1")

        val id = assertNotNull(devices.authenticate(token))

        assertEquals(id, devices.authenticate(token))
    }

    @Test
    fun `every registration issues a different token and a different device`() = runTest {
        val first = devices.register("token-1")
        val second = devices.register("token-1")

        assertNotEquals(first, second)
        assertNotEquals(devices.authenticate(first), devices.authenticate(second))
    }

    @Test
    fun `an unknown token authenticates to no device`() = runTest {
        devices.register("token-1")

        assertNull(devices.authenticate(newDeviceToken()))
    }

    @Test
    fun `the devices row holds the token's hash and not the token`() = runTest {
        val token = devices.register("token-1")

        val row = transaction(database) { DevicesTable.selectAll().single() }

        assertContentEquals(token.hash(), row[DevicesTable.tokenHash])
        val stored = transaction(database) {
            exec("SELECT * FROM devices") { rows ->
                buildList {
                    while (rows.next()) {
                        for (column in 1..rows.metaData.columnCount) add(rows.getString(column).orEmpty())
                    }
                }
            }
        }.orEmpty()
        assertTrue(stored.none { token.value in it }, "a column holds the token: $stored")
    }

    private fun lastSeenAt(id: DeviceId): Instant = transaction(database) {
        DevicesTable.selectAll().where { DevicesTable.id eq id.value }.single()[DevicesTable.lastSeenAt].toInstant()
    }

    @Test
    fun `last seen is refreshed one day after the last refresh and not one millisecond before`() = runTest {
        val clock = MutableClock(Instant.parse("2026-08-03T06:00:00Z"))
        val store = ExposedDeviceStore(database, clock)
        val token = store.register("token-1")
        val id = checkNotNull(store.authenticate(token))
        val registeredAt = clock.now

        clock.advanceBy(Duration.ofDays(1).minusMillis(1))
        store.authenticate(token)
        assertEquals(registeredAt, lastSeenAt(id))

        clock.advanceBy(Duration.ofMillis(1))
        store.authenticate(token)
        assertEquals(clock.now, lastSeenAt(id))

        val refreshedAt = clock.now
        clock.advanceBy(Duration.ofHours(23))
        store.authenticate(token)
        assertEquals(refreshedAt, lastSeenAt(id))
    }

    @Test
    fun `deleting a device removes its alarms and their log and keeps other devices`() = runTest {
        val mine = devices.registerDevice("token-mine")
        val theirs = devices.registerDevice("token-theirs")
        val myAlarm = thresholdAlarm(mine)
        val theirAlarm = thresholdAlarm(theirs)
        database.insertAlarm(myAlarm, createdAtMillis = 1)
        database.insertAlarm(theirAlarm, createdAtMillis = 2)
        log.record(myAlarm.id, setOf(PollenSpecies.BIRCH), today)
        log.record(theirAlarm.id, setOf(PollenSpecies.BIRCH), today)

        assertTrue(devices.delete(mine))

        assertEquals(emptyList(), alarms.list(mine))
        assertEquals(emptySet(), log.notifiedSpecies(myAlarm.id, today))
        assertEquals(listOf(theirAlarm), alarms.list(theirs))
        assertEquals(setOf(PollenSpecies.BIRCH), log.notifiedSpecies(theirAlarm.id, today))
        assertEquals(1L, transaction(database) { DevicesTable.selectAll().count() })
    }

    @Test
    fun `a deleted device no longer authenticates`() = runTest {
        val token = devices.register("token-1")
        val id = checkNotNull(devices.authenticate(token))

        assertTrue(devices.delete(id))

        assertNull(devices.authenticate(token))
        assertFalse(devices.delete(id))
    }

    @Test
    fun `updating the push address of an unknown device returns false`() = runTest {
        assertFalse(devices.updateFcmToken(unknownDeviceId(), "token-1"))
    }

    // --- Alarms ---

    @Test
    fun `a registered device without alarms has an empty list`() = runTest {
        val id = devices.registerDevice("token-1")

        assertEquals(emptyList(), alarms.list(id))
    }

    @Test
    fun `alarms are listed in creation order`() = runTest {
        val id = devices.registerDevice("token-1")
        // Ids chosen so that id order is the reverse of creation order.
        val first = dailyAlarm(id, alarmId('c'))
        val second = thresholdAlarm(id, alarmId('b'))
        val third = dailyAlarm(id, alarmId('a'))
        database.insertAlarm(second, createdAtMillis = 2_000)
        database.insertAlarm(third, createdAtMillis = 3_000)
        database.insertAlarm(first, createdAtMillis = 1_000)

        assertEquals(listOf(first, second, third), alarms.list(id))
    }

    @Test
    fun `both schedule types read back exactly as stored`() = runTest {
        val id = devices.registerDevice("token-1")
        val daily = dailyAlarm(id)
        val threshold = thresholdAlarm(id)
        database.insertAlarm(daily, createdAtMillis = 1)
        database.insertAlarm(threshold, createdAtMillis = 2)

        assertEquals(listOf(daily, threshold), alarms.list(id))
    }

    @Test
    fun `a device sees only its own alarms`() = runTest {
        val mine = devices.registerDevice("token-1")
        val theirs = devices.registerDevice("token-2")
        val myAlarm = dailyAlarm(mine)
        database.insertAlarm(myAlarm, createdAtMillis = 1)
        database.insertAlarm(dailyAlarm(theirs), createdAtMillis = 2)

        assertEquals(listOf(myAlarm), alarms.list(mine))
    }

    private fun dailySpec(at: LocalTime = LocalTime.of(8, 0)) = AlarmSpec(
        enabled = true,
        station = PollenStation.BERN,
        species = setOf(PollenSpecies.ASH),
        minSeverity = PollenSeverity.MODERATE,
        days = setOf(DayOfWeek.SATURDAY),
        schedule = AlarmSchedule.Daily(at),
    )

    @Test
    fun `a created alarm is returned and listed for its device`() = runTest {
        val id = devices.registerDevice("token-1")

        val created = assertIs<CreateResult.Created>(alarms.create(id, dailySpec())).alarm

        assertEquals(dailySpec().toAlarm(created.id, id), created)
        assertEquals(listOf(created), alarms.list(id))
    }

    @Test
    fun `every created alarm gets its own id`() = runTest {
        val id = devices.registerDevice("token-1")

        val first = assertIs<CreateResult.Created>(alarms.create(id, dailySpec())).alarm
        val second = assertIs<CreateResult.Created>(alarms.create(id, dailySpec())).alarm

        assertNotEquals(first.id, second.id)
    }

    @Test
    fun `alarms created within the same millisecond keep their creation order`() = runTest {
        val id = devices.registerDevice("token-1")
        val frozen = ExposedAlarmStore(database, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))

        val created = (0 until 5).map { minute ->
            assertIs<CreateResult.Created>(frozen.create(id, dailySpec(LocalTime.of(8, minute)))).alarm
        }

        assertEquals(created, alarms.list(id))
    }

    @Test
    fun `two concurrent creates at nine alarms let exactly one through`() = runTest {
        // Several devices, so a missing lock has more than one chance to show.
        repeat(5) {
            val id = devices.registerDevice("token-1")
            repeat(9) { assertIs<CreateResult.Created>(alarms.create(id, dailySpec())) }
            val start = CompletableDeferred<Unit>()

            val results = List(2) {
                async(Dispatchers.IO) {
                    start.await()
                    alarms.create(id, dailySpec())
                }
            }.also { start.complete(Unit) }.awaitAll()

            assertEquals(1, results.count { it is CreateResult.Created }, "results: $results")
            assertEquals(1, results.count { it == CreateResult.LimitReached }, "results: $results")
            assertEquals(10, alarms.list(id).size)
        }
    }

    @Test
    fun `the tenth alarm is created and the eleventh reaches the limit`() = runTest {
        val id = devices.registerDevice("token-1")
        repeat(9) { assertIs<CreateResult.Created>(alarms.create(id, dailySpec())) }

        assertIs<CreateResult.Created>(alarms.create(id, dailySpec()))
        assertEquals(CreateResult.LimitReached, alarms.create(id, dailySpec()))

        assertEquals(10, alarms.list(id).size)
    }

    @Test
    fun `the limit counts only the device's own alarms`() = runTest {
        val full = devices.registerDevice("token-1")
        val other = devices.registerDevice("token-2")
        repeat(10) { alarms.create(full, dailySpec()) }

        assertIs<CreateResult.Created>(alarms.create(other, dailySpec()))
    }

    @Test
    fun `deleting an alarm at the limit makes room for a new one`() = runTest {
        val id = devices.registerDevice("token-1")
        val created = (0 until 10).map { assertIs<CreateResult.Created>(alarms.create(id, dailySpec())).alarm }

        alarms.delete(id, created.first().id)

        assertIs<CreateResult.Created>(alarms.create(id, dailySpec()))
    }

    @Test
    fun `an update replaces the settings and keeps the id and the list position`() = runTest {
        val id = devices.registerDevice("token-1")
        val first = assertIs<CreateResult.Created>(alarms.create(id, dailySpec(LocalTime.of(6, 0)))).alarm
        val second = assertIs<CreateResult.Created>(alarms.create(id, dailySpec(LocalTime.of(7, 0)))).alarm
        val changed = dailySpec(LocalTime.of(9, 30)).copy(enabled = false, station = PollenStation.LUGANO)

        val updated = alarms.update(id, first.id, changed)

        assertEquals(changed.toAlarm(first.id, id), updated)
        assertEquals(listOf(changed.toAlarm(first.id, id), second), alarms.list(id))
    }

    @Test
    fun `updating another device's alarm returns not found and changes nothing`() = runTest {
        val mine = devices.registerDevice("token-1")
        val theirs = devices.registerDevice("token-2")
        val theirAlarm = dailyAlarm(theirs)
        database.insertAlarm(theirAlarm, createdAtMillis = 1)

        assertNull(alarms.update(mine, theirAlarm.id, dailySpec()))
        assertEquals(listOf(theirAlarm), alarms.list(theirs))
    }

    @Test
    fun `updating an unknown alarm returns not found`() = runTest {
        val id = devices.registerDevice("token-1")

        assertNull(alarms.update(id, AlarmId("never-created"), dailySpec()))
        assertEquals(emptyList(), alarms.list(id))
    }

    @Test
    fun `an alarm id that is not a UUID is not found for update or delete`() = runTest {
        val id = devices.registerDevice("token-1")
        database.insertAlarm(dailyAlarm(id), createdAtMillis = 1)

        assertNull(alarms.update(id, AlarmId("not-a-uuid"), dailySpec()))
        assertFalse(alarms.delete(id, AlarmId("not-a-uuid")))
        assertEquals(1, alarms.list(id).size)
    }

    @Test
    fun `a deleted alarm is gone from the list`() = runTest {
        val id = devices.registerDevice("token-1")
        val alarm = dailyAlarm(id)
        database.insertAlarm(alarm, createdAtMillis = 1)

        assertTrue(alarms.delete(id, alarm.id))
        assertEquals(emptyList(), alarms.list(id))
    }

    @Test
    fun `deleting another device's alarm returns not found and keeps it`() = runTest {
        val mine = devices.registerDevice("token-1")
        val theirs = devices.registerDevice("token-2")
        val theirAlarm = dailyAlarm(theirs)
        database.insertAlarm(theirAlarm, createdAtMillis = 1)

        assertFalse(alarms.delete(mine, theirAlarm.id))
        assertEquals(listOf(theirAlarm), alarms.list(theirs))
    }

    @Test
    fun `deleting an unknown alarm returns not found`() = runTest {
        assertFalse(alarms.delete(devices.registerDevice("token-1"), AlarmId("never-created")))
    }

    @Test
    fun `an alarm for an unregistered device is refused because foreign keys are enforced`() {
        assertFailsWith<ExposedSQLException> {
            database.insertAlarm(dailyAlarm(unknownDeviceId()), createdAtMillis = 1)
        }
    }

    @Test
    fun `enabled alarms are deliverable with their device's token`() = runTest {
        val mine = devices.registerDevice("token-mine")
        val theirs = devices.registerDevice("token-theirs")
        val myAlarm = dailyAlarm(mine)
        val theirAlarm = dailyAlarm(theirs)
        database.insertAlarm(myAlarm, createdAtMillis = 1)
        database.insertAlarm(theirAlarm, createdAtMillis = 2)

        assertEquals(
            listOf(AlarmWithToken(myAlarm, "token-mine"), AlarmWithToken(theirAlarm, "token-theirs")),
            alarms.enabledWithDeliverableDevice(),
        )
    }

    @Test
    fun `a disabled alarm is not deliverable`() = runTest {
        val id = devices.registerDevice("token-1")
        database.insertAlarm(dailyAlarm(id).copy(enabled = false), createdAtMillis = 1)

        assertEquals(emptyList(), alarms.enabledWithDeliverableDevice())
    }

    @Test
    fun `an updated token is the one alarms are delivered to`() = runTest {
        val id = devices.registerDevice("token-old")
        val alarm = dailyAlarm(id)
        database.insertAlarm(alarm, createdAtMillis = 1)

        assertTrue(devices.updateFcmToken(id, "token-new"))

        assertEquals(listOf(AlarmWithToken(alarm, "token-new")), alarms.enabledWithDeliverableDevice())
    }

    @Test
    fun `a cleared token removes only that device's alarms from delivery and keeps the device`() = runTest {
        val mine = devices.registerDevice("token-mine")
        val theirs = devices.registerDevice("token-theirs")
        database.insertAlarm(dailyAlarm(mine), createdAtMillis = 1)
        val theirAlarm = dailyAlarm(theirs)
        database.insertAlarm(theirAlarm, createdAtMillis = 2)

        devices.clearFcmToken(mine, "token-mine")

        assertEquals(listOf(AlarmWithToken(theirAlarm, "token-theirs")), alarms.enabledWithDeliverableDevice())
        assertEquals(1, alarms.list(mine).size)
    }

    @Test
    fun `clearing a token that was already replaced keeps the new one`() = runTest {
        val id = devices.registerDevice("token-old")
        val alarm = dailyAlarm(id)
        database.insertAlarm(alarm, createdAtMillis = 1)
        devices.updateFcmToken(id, "token-new")

        devices.clearFcmToken(id, "token-old")

        assertEquals(listOf(AlarmWithToken(alarm, "token-new")), alarms.enabledWithDeliverableDevice())
    }

    @Test
    fun `a new token after a cleared one makes the device deliverable again`() = runTest {
        val id = devices.registerDevice("token-old")
        val alarm = dailyAlarm(id)
        database.insertAlarm(alarm, createdAtMillis = 1)
        devices.clearFcmToken(id, "token-old")

        devices.updateFcmToken(id, "token-new")

        assertEquals(listOf(AlarmWithToken(alarm, "token-new")), alarms.enabledWithDeliverableDevice())
    }

    @Test
    fun `data is read back through a new connection pool`() = runTest {
        val id = devices.registerDevice("token-1")
        val alarm = dailyAlarm(id)
        database.insertAlarm(alarm, createdAtMillis = 1)

        PollenInfoDatabase.connect(TestPostgres.sharedConfig()).use { reopened ->
            assertEquals(listOf(alarm), ExposedAlarmStore(reopened.database).list(id))
        }
    }

    // --- Notification log ---

    private val today = LocalDate.of(2026, 8, 3)

    private suspend fun storedThresholdAlarm(): AlarmId {
        val alarm = thresholdAlarm(devices.registerDevice("token-1"))
        database.insertAlarm(alarm, createdAtMillis = 1)
        return alarm.id
    }

    @Test
    fun `an alarm that never notified has no notified species`() = runTest {
        assertEquals(emptySet(), log.notifiedSpecies(storedThresholdAlarm(), today))
    }

    @Test
    fun `recorded species are notified on their day only`() = runTest {
        val id = storedThresholdAlarm()

        log.record(id, setOf(PollenSpecies.BIRCH, PollenSpecies.GRASSES), today)

        assertEquals(setOf(PollenSpecies.BIRCH, PollenSpecies.GRASSES), log.notifiedSpecies(id, today))
        assertEquals(emptySet(), log.notifiedSpecies(id, today.plusDays(1)))
        assertEquals(emptySet(), log.notifiedSpecies(id, today.minusDays(1)))
    }

    @Test
    fun `recording adds to what was recorded earlier that day`() = runTest {
        val id = storedThresholdAlarm()

        log.record(id, setOf(PollenSpecies.BIRCH), today)
        log.record(id, setOf(PollenSpecies.BIRCH, PollenSpecies.GRASSES), today)

        assertEquals(setOf(PollenSpecies.BIRCH, PollenSpecies.GRASSES), log.notifiedSpecies(id, today))
    }

    @Test
    fun `one alarm's record does not count for another`() = runTest {
        val mine = storedThresholdAlarm()
        val theirs = storedThresholdAlarm()

        log.record(mine, setOf(PollenSpecies.BIRCH), today)

        assertEquals(emptySet(), log.notifiedSpecies(theirs, today))
    }

    @Test
    fun `recording for an alarm that does not exist does nothing`() = runTest {
        val neverStored = AlarmId(UUID.randomUUID().toString())

        log.record(neverStored, setOf(PollenSpecies.BIRCH), today)

        assertEquals(emptySet(), log.notifiedSpecies(neverStored, today))
    }

    @Test
    fun `pruning forgets the days before the given one and keeps that day`() = runTest {
        val id = storedThresholdAlarm()
        log.record(id, setOf(PollenSpecies.BIRCH), today.minusDays(2))
        log.record(id, setOf(PollenSpecies.ASH), today.minusDays(1))
        log.record(id, setOf(PollenSpecies.GRASSES), today)

        log.pruneBefore(today)

        assertEquals(emptySet(), log.notifiedSpecies(id, today.minusDays(2)))
        assertEquals(emptySet(), log.notifiedSpecies(id, today.minusDays(1)))
        assertEquals(setOf(PollenSpecies.GRASSES), log.notifiedSpecies(id, today))
    }

    @Test
    fun `deleting an alarm removes its log rows because foreign keys are enforced`() = runTest {
        val id = storedThresholdAlarm()
        log.record(id, setOf(PollenSpecies.BIRCH), today)

        transaction(database) { AlarmsTable.deleteWhere { AlarmsTable.id eq UUID.fromString(id.value) } }

        assertEquals(0L, transaction(database) { NotificationLogTable.selectAll().count() })
    }

    @Test
    fun `deleting an alarm through the store removes its log rows`() = runTest {
        val device = devices.registerDevice("token-1")
        val alarm = thresholdAlarm(device)
        database.insertAlarm(alarm, createdAtMillis = 1)
        log.record(alarm.id, setOf(PollenSpecies.BIRCH), today)

        alarms.delete(device, alarm.id)

        assertEquals(0L, transaction(database) { NotificationLogTable.selectAll().count() })
    }

    @Test
    fun `updating an alarm keeps its log rows`() = runTest {
        val device = devices.registerDevice("token-1")
        val alarm = thresholdAlarm(device)
        database.insertAlarm(alarm, createdAtMillis = 1)
        log.record(alarm.id, setOf(PollenSpecies.BIRCH), today)
        val spec = AlarmSpec(
            enabled = true,
            station = alarm.station,
            species = setOf(PollenSpecies.BIRCH, PollenSpecies.GRASSES),
            minSeverity = alarm.minSeverity,
            days = alarm.days,
            schedule = alarm.schedule,
        )

        alarms.update(device, alarm.id, spec)

        assertEquals(setOf(PollenSpecies.BIRCH), log.notifiedSpecies(alarm.id, today))
    }

    @Test
    fun `the log is read back through a new connection pool`() = runTest {
        val alarmId = storedThresholdAlarm()
        log.record(alarmId, setOf(PollenSpecies.BIRCH), today)

        PollenInfoDatabase.connect(TestPostgres.sharedConfig()).use { reopened ->
            assertEquals(setOf(PollenSpecies.BIRCH), ExposedNotificationLog(reopened.database).notifiedSpecies(alarmId, today))
        }
    }
}
