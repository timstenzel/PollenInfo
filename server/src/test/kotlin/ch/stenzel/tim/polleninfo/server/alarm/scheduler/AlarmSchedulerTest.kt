package ch.stenzel.tim.polleninfo.server.alarm.scheduler

import ch.stenzel.tim.polleninfo.server.alarm.domain.ALARM_ZONE
import ch.stenzel.tim.polleninfo.server.alarm.domain.Alarm
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSchedule
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSpec
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import ch.stenzel.tim.polleninfo.server.alarm.domain.PushChannel
import ch.stenzel.tim.polleninfo.server.alarm.domain.PushKind
import ch.stenzel.tim.polleninfo.server.alarm.push.PushResult
import ch.stenzel.tim.polleninfo.server.alarm.push.FakePushSender
import ch.stenzel.tim.polleninfo.server.alarm.store.DeviceStore
import ch.stenzel.tim.polleninfo.server.alarm.store.DevicesTable
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedAlarmStore
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedDeviceStore
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedNotificationLog
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedSchedulerState
import ch.stenzel.tim.polleninfo.server.alarm.store.TestPostgres
import ch.stenzel.tim.polleninfo.server.alarm.store.dailyAlarm
import ch.stenzel.tim.polleninfo.server.alarm.store.insertAlarm
import ch.stenzel.tim.polleninfo.server.alarm.store.registerDevice
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenThresholds
import ch.stenzel.tim.polleninfo.server.pollen.measurement.MeasurementService
import ch.stenzel.tim.polleninfo.server.pollen.measurement.MutableClock
import ch.stenzel.tim.polleninfo.server.pollen.measurement.TtlCache
import ch.stenzel.tim.polleninfo.server.pollen.upstream.FakePollenService
import ch.stenzel.tim.polleninfo.server.pollen.upstream.hourlyCsv
import java.io.IOException
import java.time.Duration
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class AlarmSchedulerTest {

    /** Monday 3 August 2026, 08:00 in Zürich. */
    private val clock = MutableClock(Instant.parse("2026-08-03T06:00:00Z"))

    private val database = TestPostgres.cleanDatabase()
    private val devices = ExposedDeviceStore(database, clock)
    private val alarms = ExposedAlarmStore(database)
    private val log = ExposedNotificationLog(database)
    private val state = ExposedSchedulerState(database)

    /** One reading from 07:00 Swiss time: Birch MODERATE, Grasses HIGH. */
    private val pollen = FakePollenService(
        bytes = hourlyCsv(
            rows = listOf("03.08.2026 05:00" to mapOf(PollenSpecies.BIRCH to 42, PollenSpecies.GRASSES to 20)),
        ),
    )
    private val push = FakePushSender()

    private fun measurementService() = MeasurementService(
        pollenService = pollen,
        thresholds = PollenThresholds(),
        cache = TtlCache(MeasurementService.CACHE_TTL, clock),
    )

    /** A fresh scheduler with a fresh cache, as after a restart; only the database is shared. */
    private fun scheduler(
        measurements: MeasurementService = measurementService(),
        devices: DeviceStore = this.devices,
    ) = AlarmScheduler(
        alarms = alarms,
        devices = devices,
        log = log,
        state = state,
        measurements = measurements,
        push = push,
        clock = clock,
    )

    private var nextCreatedAt = 1L

    /** Registers a device with [token] and gives it a Mon–Fri "Any" daily report. */
    private suspend fun dailyReport(
        token: String,
        at: LocalTime = LocalTime.of(8, 0),
        station: PollenStation = PollenStation.ZUERICH,
        minSeverity: PollenSeverity = PollenSeverity.NONE,
    ): DeviceId {
        val device = devices.registerDevice(token)
        val alarm = dailyAlarm(device, station = station, at = at).copy(minSeverity = minSeverity)
        database.insertAlarm(alarm, createdAtMillis = nextCreatedAt++)
        return device
    }

    @Test
    fun `a daily report is sent once at its minute and not at the next tick`() = runTest {
        dailyReport("token-1")
        val scheduler = scheduler()

        scheduler.tick()
        scheduler.tick()
        clock.advanceBy(Duration.ofMinutes(1))
        scheduler.tick()

        val delivery = push.sent.single()
        assertEquals("token-1", delivery.token)
        assertEquals(PushKind.REPORT, delivery.message.kind)
        assertEquals(PollenStation.ZUERICH, delivery.message.station)
        assertEquals(PushChannel.DAILY_REPORT, delivery.message.channel)
        assertEquals(listOf(PollenSpecies.GRASSES to PollenSeverity.HIGH, PollenSpecies.BIRCH to PollenSeverity.MODERATE), delivery.message.levels)
    }

    @Test
    fun `an unregistered token is cleared and the device's alarms are skipped on the next tick`() = runTest {
        dailyReport("token-gone")
        thresholdAlert("token-gone-too")
        push.results["token-gone"] = PushResult.Unregistered
        push.results["token-gone-too"] = PushResult.Unregistered
        val scheduler = scheduler()

        scheduler.tick()
        clock.advanceBy(Duration.ofMinutes(1))
        scheduler.tick()

        assertEquals(listOf("token-gone", "token-gone-too"), push.sent.map { it.token }.sorted())
        assertEquals(emptyList(), alarms.enabledWithDeliverableDevice())
    }

    @Test
    fun `a device that sends a new token after being dropped is delivered to again`() = runTest {
        val alarm = thresholdAlert("token-gone")
        push.results["token-gone"] = PushResult.Unregistered
        val scheduler = scheduler()
        scheduler.tick()

        devices.updateFcmToken(alarm.deviceId, "token-new")
        clock.advanceBy(Duration.ofMinutes(1))
        scheduler.tick()

        assertEquals(listOf("token-gone", "token-new"), push.sent.map { it.token })
        assertEquals(setOf(PollenSpecies.GRASSES), log.notifiedSpecies(alarm.id, monday))
    }

    @Test
    fun `nothing is sent before the report's minute`() = runTest {
        dailyReport("token-1")
        clock.advanceBy(Duration.ofSeconds(-1))

        scheduler().tick()

        assertEquals(emptyList(), push.sent)
        assertEquals(emptyList(), pollen.requested)
    }

    @Test
    fun `a station without a due alarm is never requested`() = runTest {
        dailyReport("token-zh", station = PollenStation.ZUERICH)
        dailyReport("token-be", station = PollenStation.BERN, at = LocalTime.of(9, 0))

        scheduler().tick()

        assertEquals(listOf(PollenStation.ZUERICH), pollen.requested)
    }

    @Test
    fun `many ticks within the cache period fetch each station once`() = runTest {
        listOf(0, 10, 20, 29).forEach { minute -> dailyReport("token-zh-$minute", at = LocalTime.of(8, minute)) }
        listOf(0, 15).forEach { minute ->
            dailyReport("token-be-$minute", at = LocalTime.of(8, minute), station = PollenStation.BERN)
        }
        val scheduler = scheduler()

        repeat(30) {
            scheduler.tick()
            clock.advanceBy(Duration.ofMinutes(1))
        }

        assertEquals(6, push.sent.size)
        assertEquals(1, pollen.requested.count { it == PollenStation.ZUERICH })
        assertEquals(1, pollen.requested.count { it == PollenStation.BERN })
    }

    @Test
    fun `nothing is sent after the clock jumps past the report's time`() = runTest {
        dailyReport("token-1")
        val scheduler = scheduler()
        clock.advanceBy(Duration.ofMinutes(-1))
        scheduler.tick()

        clock.advanceBy(Duration.ofMinutes(6))
        scheduler.tick()

        assertEquals(emptyList(), push.sent)
    }

    @Test
    fun `a backend started after the report's time does not catch up`() = runTest {
        dailyReport("token-1")
        clock.advanceBy(Duration.ofMinutes(40))

        scheduler().tick()

        assertEquals(emptyList(), push.sent)
    }

    // --- catch-up after a restart ---

    /** [hhmm] on Monday 3 August 2026 in Zürich, as an instant. */
    private fun swiss(hhmm: String): Instant =
        LocalTime.parse(hhmm).atDate(monday).atZone(ALARM_ZONE).toInstant()

    @Test
    fun `a restart catches up the minute it missed and then runs the current one`() = runTest {
        dailyReport("token-0800", at = LocalTime.of(8, 0))
        dailyReport("token-0801", at = LocalTime.of(8, 1))
        state.recordMinute(swiss("07:59"))
        clock.now = swiss("08:01").plusSeconds(10)
        val scheduler = scheduler()

        scheduler.tick()
        clock.advanceBy(Duration.ofSeconds(20))
        scheduler.tick()

        assertEquals(listOf("token-0800", "token-0801"), push.sent.map { it.token })
        assertEquals(swiss("08:01"), state.lastMinute())
    }

    @Test
    fun `a restart does not catch up minutes older than two minutes`() = runTest {
        listOf(56, 57, 58, 59).forEach { minute -> dailyReport("token-07$minute", at = LocalTime.of(7, minute)) }
        dailyReport("token-0800", at = LocalTime.of(8, 0))
        state.recordMinute(swiss("07:55"))
        clock.now = swiss("08:01").plusSeconds(10)

        scheduler().tick()

        assertEquals(listOf("token-0759", "token-0800"), push.sent.map { it.token })
    }

    @Test
    fun `a minute already processed is not sent again by a scheduler built on the same database`() = runTest {
        dailyReport("token-1")
        scheduler().tick()

        clock.advanceBy(Duration.ofSeconds(30))
        scheduler().tick()

        assertEquals(listOf("token-1"), push.sent.map { it.token })
    }

    @Test
    fun `the first start ever catches up nothing`() = runTest {
        dailyReport("token-1")
        clock.now = swiss("08:01").plusSeconds(10)

        scheduler().tick()

        assertEquals(emptyList(), push.sent)
        assertEquals(swiss("08:01"), state.lastMinute())
    }

    @Test
    fun `catching up does not repeat a threshold alert already sent that day`() = runTest {
        thresholdAlert("token-1")
        clock.now = swiss("07:58")
        scheduler().tick()
        assertEquals(1, push.sent.size)

        // Down for 07:59 and 08:00; back at 08:01:10 — both missed minutes are caught up.
        clock.now = swiss("08:01").plusSeconds(10)
        scheduler().tick()

        assertEquals(1, push.sent.size)
        assertEquals(swiss("08:01"), state.lastMinute())
    }

    @Test
    fun `one station's upstream failure does not stop another station's report`() = runTest {
        pollen.failures[PollenStation.ZUERICH] = IOException("Zürich is down")
        dailyReport("token-zh-any", station = PollenStation.ZUERICH)
        dailyReport("token-zh-high", station = PollenStation.ZUERICH, minSeverity = PollenSeverity.HIGH)
        dailyReport("token-be", station = PollenStation.BERN)

        scheduler().tick()

        val byToken = push.sent.associate { it.token to it.message.kind }
        assertEquals(mapOf("token-zh-any" to PushKind.UNAVAILABLE, "token-be" to PushKind.REPORT), byToken)
    }

    @Test
    fun `a delivery that throws for one station does not stop another station's report`() = runTest {
        push.throwingTokens += "token-zh"
        dailyReport("token-zh", station = PollenStation.ZUERICH)
        dailyReport("token-be", station = PollenStation.BERN)

        scheduler().tick()

        assertEquals(listOf("token-be"), push.sent.map { it.token })
    }

    // --- threshold alerts ---

    /** Registers a device with [token] and gives it an every-day threshold alert. */
    private suspend fun thresholdAlert(
        token: String,
        species: Set<PollenSpecies> = setOf(PollenSpecies.GRASSES),
        minSeverity: PollenSeverity = PollenSeverity.HIGH,
        from: LocalTime = LocalTime.of(7, 0),
        until: LocalTime = LocalTime.of(21, 0),
    ): Alarm {
        val device = devices.registerDevice(token)
        val alarm = dailyAlarm(device).copy(
            species = species,
            minSeverity = minSeverity,
            days = DayOfWeek.entries.toSet(),
            schedule = AlarmSchedule.Threshold(from, until),
        )
        database.insertAlarm(alarm, createdAtMillis = nextCreatedAt++)
        return alarm
    }

    /** Publishes one row at [utc] (`dd.MM.yyyy HH:mm`, as upstream). */
    private fun publish(utc: String, values: Map<PollenSpecies, Int>) {
        pollen.bytes = hourlyCsv(rows = listOf(utc to values))
    }

    private val monday = LocalDate.of(2026, 8, 3)

    @Test
    fun `a threshold alert fires once per species per day and again the next day`() = runTest {
        thresholdAlert("token-1")
        val scheduler = scheduler()

        repeat(30) {
            scheduler.tick()
            clock.advanceBy(Duration.ofMinutes(1))
        }
        val first = push.sent.single()
        assertEquals(PushKind.ALERT, first.message.kind)
        assertEquals(PollenStation.ZUERICH, first.message.station)
        assertEquals(listOf(PollenSpecies.GRASSES to PollenSeverity.HIGH), first.message.levels)
        assertEquals(PushChannel.THRESHOLD_ALERT, first.message.channel)

        // Tuesday 08:00 in Zürich, with Tuesday's reading.
        clock.advanceBy(Duration.ofHours(24).minusMinutes(30))
        publish("04.08.2026 05:00", mapOf(PollenSpecies.GRASSES to 20))
        scheduler.tick()

        assertEquals(2, push.sent.size)
        assertEquals(listOf(PollenSpecies.GRASSES to PollenSeverity.HIGH), push.sent.last().message.levels)
    }

    @Test
    fun `a second species qualifying later gets its own message`() = runTest {
        thresholdAlert("token-1", species = setOf(PollenSpecies.BIRCH, PollenSpecies.GRASSES))
        val scheduler = scheduler()
        scheduler.tick()

        // Half an hour later the cache has expired and the 08:00 row shows Birch at High too.
        clock.advanceBy(Duration.ofMinutes(30))
        publish("03.08.2026 06:00", mapOf(PollenSpecies.BIRCH to 100, PollenSpecies.GRASSES to 25))
        scheduler.tick()

        assertEquals(listOf(listOf(PollenSpecies.GRASSES to PollenSeverity.HIGH), listOf(PollenSpecies.BIRCH to PollenSeverity.HIGH)), push.sent.map { it.message.levels })
    }

    @Test
    fun `species qualifying on the same reading share one message`() = runTest {
        publish("03.08.2026 05:00", mapOf(PollenSpecies.BIRCH to 100, PollenSpecies.GRASSES to 25))
        thresholdAlert("token-1", species = setOf(PollenSpecies.BIRCH, PollenSpecies.GRASSES))

        scheduler().tick()

        assertEquals(listOf(PollenSpecies.BIRCH to PollenSeverity.HIGH, PollenSpecies.GRASSES to PollenSeverity.HIGH), push.sent.single().message.levels)
    }

    @Test
    fun `a failed send is retried on the next tick`() = runTest {
        val alarm = thresholdAlert("token-1")
        push.results["token-1"] = PushResult.Failed(IOException("FCM is down"))
        val scheduler = scheduler()
        scheduler.tick()

        push.results.remove("token-1")
        clock.advanceBy(Duration.ofMinutes(1))
        scheduler.tick()
        clock.advanceBy(Duration.ofMinutes(1))
        scheduler.tick()

        assertEquals(2, push.sent.size)
        assertEquals(setOf(PollenSpecies.GRASSES), log.notifiedSpecies(alarm.id, monday))
    }

    @Test
    fun `a species over the threshold before the window opens fires at the first tick inside it`() = runTest {
        thresholdAlert("token-1", from = LocalTime.of(8, 30))
        val scheduler = scheduler()
        scheduler.tick()
        assertEquals(emptyList(), push.sent)
        assertEquals(emptyList(), pollen.requested)

        clock.advanceBy(Duration.ofMinutes(30))
        scheduler.tick()

        assertEquals(listOf(PollenSpecies.GRASSES to PollenSeverity.HIGH), push.sent.single().message.levels)
    }

    @Test
    fun `an upstream failure with only a stale reading sends nothing`() = runTest {
        val measurements = measurementService()
        // Someone looks at Zürich at 08:00, so the 07:00 reading is cached.
        measurements.measurementFor(PollenStation.ZUERICH)
        thresholdAlert("token-1")
        pollen.failure = IOException("MeteoSwiss is down")

        // At 10:00 the retained 07:00 reading is three hours old.
        clock.advanceBy(Duration.ofHours(2))
        scheduler(measurements).tick()

        assertEquals(emptyList(), push.sent)
    }

    @Test
    fun `a scheduler recreated on the same database does not resend that day`() = runTest {
        thresholdAlert("token-1")
        scheduler().tick()

        clock.advanceBy(Duration.ofMinutes(1))
        scheduler().tick()

        assertEquals(1, push.sent.size)
    }

    @Test
    fun `the log is pruned when the Swiss date changes`() = runTest {
        val alarm = thresholdAlert("token-1")
        val scheduler = scheduler()
        scheduler.tick()
        assertEquals(setOf(PollenSpecies.GRASSES), log.notifiedSpecies(alarm.id, monday))

        // 23:59 on Monday in Zürich is still Monday: nothing is pruned.
        clock.advanceBy(Duration.ofHours(15).plusMinutes(59))
        scheduler.tick()
        assertEquals(setOf(PollenSpecies.GRASSES), log.notifiedSpecies(alarm.id, monday))

        // Midnight in Zürich, though still Monday in UTC.
        clock.advanceBy(Duration.ofMinutes(1))
        scheduler.tick()
        assertEquals(emptySet(), log.notifiedSpecies(alarm.id, monday))
    }

    @Test
    fun `a daily report and a threshold alert for one station share one fetch`() = runTest {
        dailyReport("token-daily")
        thresholdAlert("token-threshold")

        scheduler().tick()

        assertEquals(setOf("token-daily", "token-threshold"), push.sent.map { it.token }.toSet())
        assertEquals(listOf(PollenStation.ZUERICH), pollen.requested)
    }

    // --- edits through the store ---

    /** [alarm] as the spec a `PUT` would carry, with [change] applied. */
    private fun Alarm.asSpec(change: AlarmSpec.() -> AlarmSpec = { this }) =
        AlarmSpec(enabled, station, species, minSeverity, days, schedule).change()

    @Test
    fun `a paused alarm sends nothing`() = runTest {
        val device = dailyReport("token-daily")
        val daily = alarms.list(device).single()
        val threshold = thresholdAlert("token-threshold")
        alarms.update(device, daily.id, daily.asSpec { copy(enabled = false) })
        alarms.update(threshold.deviceId, threshold.id, threshold.asSpec { copy(enabled = false) })

        scheduler().tick()

        assertEquals(emptyList(), push.sent)
        assertEquals(emptyList(), pollen.requested)
    }

    @Test
    fun `an edit keeps today's record and a newly added qualifying species still fires`() = runTest {
        publish("03.08.2026 05:00", mapOf(PollenSpecies.BIRCH to 100, PollenSpecies.GRASSES to 25))
        val alarm = thresholdAlert("token-1", species = setOf(PollenSpecies.BIRCH))
        val scheduler = scheduler()
        scheduler.tick()
        assertEquals(listOf(PollenSpecies.BIRCH to PollenSeverity.HIGH), push.sent.single().message.levels)

        alarms.update(alarm.deviceId, alarm.id, alarm.asSpec { copy(species = setOf(PollenSpecies.BIRCH, PollenSpecies.GRASSES)) })
        clock.advanceBy(Duration.ofMinutes(1))
        scheduler.tick()
        clock.advanceBy(Duration.ofMinutes(1))
        scheduler.tick()

        assertEquals(listOf(listOf(PollenSpecies.BIRCH to PollenSeverity.HIGH), listOf(PollenSpecies.GRASSES to PollenSeverity.HIGH)), push.sent.map { it.message.levels })
    }

    // --- Inactive devices ---

    private fun isStored(id: DeviceId): Boolean = transaction(database) {
        DevicesTable.selectAll().where { DevicesTable.id eq id.value }.count() > 0
    }

    /** Registers a device at [at], without alarms, and drops its push token as FCM would. */
    private suspend fun unreachableDevice(at: Instant): DeviceId {
        val store = ExposedDeviceStore(database, MutableClock(at))
        val id = store.registerDevice("token-$at")
        store.clearFcmToken(id, "token-$at")
        return id
    }

    /** Counts [pruneInactive] calls and fails the first [failures] of them. */
    private class FlakyPrune(private val store: DeviceStore, var failures: Int = 0) : DeviceStore by store {
        var attempts = 0

        override suspend fun pruneInactive(now: Instant): Int {
            attempts++
            if (failures-- > 0) throw IOException("database unreachable")
            return store.pruneInactive(now)
        }
    }

    @Test
    fun `the first tick of a Swiss day prunes inactive devices and a later tick that day does not`() = runTest {
        // Sunday 1 November 2026, 07:01 in Zürich: 90 days and a minute after the first device's
        // last call, 90 days less two hours after the second's.
        val first = unreachableDevice(Instant.parse("2026-08-03T06:00:00Z"))
        val second = unreachableDevice(Instant.parse("2026-08-03T08:00:00Z"))
        clock.now = Instant.parse("2026-11-01T06:01:00Z")
        val scheduler = scheduler()

        scheduler.tick()
        assertFalse(isStored(first))
        assertTrue(isStored(second))

        // The second is past 90 days too now, but the day's prune has run.
        clock.now = Instant.parse("2026-11-01T22:59:00Z")
        scheduler.tick()
        assertTrue(isStored(second))

        // Midnight in Zürich.
        clock.advanceBy(Duration.ofMinutes(1))
        scheduler.tick()
        assertFalse(isStored(second))
    }

    @Test
    fun `a failing prune still sends the minute's alarms and is retried on the next tick`() = runTest {
        dailyReport("token-1")
        val inactive = unreachableDevice(Instant.parse("2026-05-01T06:00:00Z"))
        val flaky = FlakyPrune(devices, failures = 1)
        val scheduler = scheduler(devices = flaky)

        scheduler.tick()
        assertEquals(listOf("token-1"), push.sent.map { it.token })
        assertTrue(isStored(inactive))

        clock.advanceBy(Duration.ofMinutes(1))
        scheduler.tick()
        assertFalse(isStored(inactive))

        clock.advanceBy(Duration.ofMinutes(1))
        scheduler.tick()
        assertEquals(2, flaky.attempts)
    }

    @Test
    fun `a device whose token was dropped is kept for 90 days after its last call and pruned the day after`() = runTest {
        val device = dailyReport("token-gone")
        push.results["token-gone"] = PushResult.Unregistered
        scheduler().tick()
        assertEquals(emptyList(), alarms.enabledWithDeliverableDevice())

        // Exactly 90 days after the registration, its last call: kept.
        clock.now = Instant.parse("2026-11-01T06:00:00Z")
        val scheduler = scheduler()
        scheduler.tick()
        assertTrue(isStored(device))

        // Past 90 days later that day, but the day's prune has run.
        clock.advanceBy(Duration.ofMinutes(1))
        scheduler.tick()
        assertTrue(isStored(device))

        // Midnight in Zürich: the next day's first tick removes the device and its alarm.
        clock.now = Instant.parse("2026-11-01T23:00:00Z")
        scheduler.tick()
        assertFalse(isStored(device))
        assertEquals(emptyList(), alarms.list(device))
    }
}
