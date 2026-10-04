package ch.stenzel.tim.polleninfo.server.alarm.scheduler

import ch.stenzel.tim.polleninfo.server.alarm.domain.DeviceId
import ch.stenzel.tim.polleninfo.server.alarm.push.FakePushSender
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedAlarmStore
import ch.stenzel.tim.polleninfo.server.alarm.store.ExposedDeviceStore
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
import java.time.Instant
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

    /** One reading from 07:00 Swiss time: Birch MODERATE, Grasses HIGH. */
    private val pollen = FakePollenService(
        bytes = hourlyCsv(
            rows = listOf("03.08.2026 05:00" to mapOf(PollenSpecies.BIRCH to 42, PollenSpecies.GRASSES to 20)),
        ),
    )
    private val push = FakePushSender()

    private fun scheduler() = AlarmScheduler(
        alarms = alarms,
        measurements = MeasurementService(
            pollenService = pollen,
            thresholds = PollenThresholds(),
            cache = TtlCache(MeasurementService.CACHE_TTL, clock),
        ),
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
}
