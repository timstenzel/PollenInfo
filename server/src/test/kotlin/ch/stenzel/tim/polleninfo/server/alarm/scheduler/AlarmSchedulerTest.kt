package ch.stenzel.tim.polleninfo.server.alarm.scheduler

import ch.stenzel.tim.polleninfo.server.alarm.domain.Alarm
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSchedule
import ch.stenzel.tim.polleninfo.server.alarm.domain.AlarmSpec
import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import ch.stenzel.tim.polleninfo.server.alarm.domain.PushChannel
import ch.stenzel.tim.polleninfo.server.alarm.push.PushResult
import ch.stenzel.tim.polleninfo.server.alarm.push.FakePushSender
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedAlarmStore
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedDeviceStore
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedNotificationLog
import ch.stenzel.tim.polleninfo.server.alarm.store.PollenInfoDatabase
import ch.stenzel.tim.polleninfo.server.alarm.store.dailyAlarm
import ch.stenzel.tim.polleninfo.server.alarm.store.insertAlarm
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
import kotlinx.coroutines.test.runTest

class AlarmSchedulerTest {

    /** Monday 3 August 2026, 08:00 in Zürich. */
    private val clock = MutableClock(Instant.parse("2026-08-03T06:00:00Z"))

    private val database = PollenInfoDatabase.inMemory()
    private val devices = ExposedDeviceStore(database)
    private val alarms = ExposedAlarmStore(database)
    private val log = ExposedNotificationLog(database)

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
    private fun scheduler(measurements: MeasurementService = measurementService()) = AlarmScheduler(
        alarms = alarms,
        log = log,
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
        val device = devices.register(token)
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
        assertEquals("Pollen in Zürich", delivery.message.title)
        assertEquals("Grasses: High · Birch: Moderate", delivery.message.body)
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

    @Test
    fun `one station's upstream failure does not stop another station's report`() = runTest {
        pollen.failures[PollenStation.ZUERICH] = IOException("Zürich is down")
        dailyReport("token-zh-any", station = PollenStation.ZUERICH)
        dailyReport("token-zh-high", station = PollenStation.ZUERICH, minSeverity = PollenSeverity.HIGH)
        dailyReport("token-be", station = PollenStation.BERN)

        scheduler().tick()

        val byToken = push.sent.associate { it.token to it.message.body }
        assertEquals(
            mapOf(
                "token-zh-any" to "No current reading for Zürich. Readings are currently unavailable.",
                "token-be" to "Grasses: High · Birch: Moderate",
            ),
            byToken,
        )
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
        val device = devices.register(token)
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
        assertEquals("Pollen in Zürich", first.message.title)
        assertEquals("Grasses: High", first.message.body)
        assertEquals(PushChannel.THRESHOLD_ALERT, first.message.channel)

        // Tuesday 08:00 in Zürich, with Tuesday's reading.
        clock.advanceBy(Duration.ofHours(24).minusMinutes(30))
        publish("04.08.2026 05:00", mapOf(PollenSpecies.GRASSES to 20))
        scheduler.tick()

        assertEquals(2, push.sent.size)
        assertEquals("Grasses: High", push.sent.last().message.body)
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

        assertEquals(listOf("Grasses: High", "Birch: High"), push.sent.map { it.message.body })
    }

    @Test
    fun `species qualifying on the same reading share one message`() = runTest {
        publish("03.08.2026 05:00", mapOf(PollenSpecies.BIRCH to 100, PollenSpecies.GRASSES to 25))
        thresholdAlert("token-1", species = setOf(PollenSpecies.BIRCH, PollenSpecies.GRASSES))

        scheduler().tick()

        assertEquals("Birch: High · Grasses: High", push.sent.single().message.body)
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

        assertEquals("Grasses: High", push.sent.single().message.body)
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
        val daily = alarms.list(device)!!.single()
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
        assertEquals("Birch: High", push.sent.single().message.body)

        alarms.update(alarm.deviceId, alarm.id, alarm.asSpec { copy(species = setOf(PollenSpecies.BIRCH, PollenSpecies.GRASSES)) })
        clock.advanceBy(Duration.ofMinutes(1))
        scheduler.tick()
        clock.advanceBy(Duration.ofMinutes(1))
        scheduler.tick()

        assertEquals(listOf("Birch: High", "Grasses: High"), push.sent.map { it.message.body })
    }
}
