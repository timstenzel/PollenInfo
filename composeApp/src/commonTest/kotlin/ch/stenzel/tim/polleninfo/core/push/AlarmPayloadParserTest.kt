package ch.stenzel.tim.polleninfo.core.push

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Instant

class AlarmPayloadParserTest {

    private fun payload(vararg extra: Pair<String, String>) = mapOf(
        "channel" to "daily_report",
        "stationAbbr" to "PZH",
        "stationName" to "Zürich",
        "alarmId" to "alarm-1",
    ) + extra

    private val generic = AlarmNotificationContent.Generic("Zürich", AlarmChannel.DAILY_REPORT)

    @Test
    fun `a report keeps its levels in the order sent`() {
        val content = parseAlarmPayload(payload("kind" to "report", "levels" to "GRASSES:HIGH,BIRCH:MODERATE,ALDER:NONE"))

        assertEquals(
            AlarmNotificationContent.Levels(
                stationName = "Zürich",
                channel = AlarmChannel.DAILY_REPORT,
                levels = listOf(
                    "GRASSES" to PollenSeverity.HIGH,
                    "BIRCH" to PollenSeverity.MODERATE,
                    "ALDER" to PollenSeverity.NONE,
                ),
                isAlert = false,
            ),
            content,
        )
    }

    @Test
    fun `an alert is levels on the threshold alert channel`() {
        val content = parseAlarmPayload(
            payload("kind" to "alert", "channel" to "threshold_alert", "levels" to "BIRCH:VERY_HIGH"),
        )

        assertEquals(
            AlarmNotificationContent.Levels(
                stationName = "Zürich",
                channel = AlarmChannel.THRESHOLD_ALERT,
                levels = listOf("BIRCH" to PollenSeverity.VERY_HIGH),
                isAlert = true,
            ),
            content,
        )
    }

    @Test
    fun `no pollen and not reported and unavailable need nothing more`() {
        assertEquals(
            AlarmNotificationContent.NoPollen("Zürich", AlarmChannel.DAILY_REPORT),
            parseAlarmPayload(payload("kind" to "no_pollen")),
        )
        assertEquals(
            AlarmNotificationContent.NotReported("Zürich", AlarmChannel.DAILY_REPORT),
            parseAlarmPayload(payload("kind" to "not_reported")),
        )
        assertEquals(
            AlarmNotificationContent.Unavailable("Zürich", AlarmChannel.DAILY_REPORT),
            parseAlarmPayload(payload("kind" to "unavailable")),
        )
    }

    @Test
    fun `no current reading carries its measuredAt`() {
        val content = parseAlarmPayload(payload("kind" to "no_current_reading", "measuredAt" to "2026-08-02T20:00:00Z"))

        assertEquals(
            AlarmNotificationContent.NoCurrentReading(
                "Zürich",
                AlarmChannel.DAILY_REPORT,
                Instant.parse("2026-08-02T20:00:00Z"),
            ),
            content,
        )
    }

    @Test
    fun `no current reading without measuredAt is generic`() {
        assertEquals(generic, parseAlarmPayload(payload("kind" to "no_current_reading")))
    }

    @Test
    fun `no current reading with an unparseable measuredAt is generic`() {
        assertEquals(generic, parseAlarmPayload(payload("kind" to "no_current_reading", "measuredAt" to "yesterday")))
    }

    @Test
    fun `an unknown kind is generic`() {
        assertEquals(generic, parseAlarmPayload(payload("kind" to "forecast", "levels" to "BIRCH:HIGH")))
    }

    @Test
    fun `a missing kind is generic`() {
        assertEquals(generic, parseAlarmPayload(payload()))
    }

    @Test
    fun `an unknown species makes the whole report generic`() {
        assertEquals(generic, parseAlarmPayload(payload("kind" to "report", "levels" to "BIRCH:HIGH,MUGWORT:HIGH")))
    }

    @Test
    fun `an unknown severity makes the whole alert generic`() {
        assertEquals(generic, parseAlarmPayload(payload("kind" to "alert", "levels" to "BIRCH:EXTREME")))
    }

    @Test
    fun `malformed levels are generic`() {
        listOf("BIRCH", "BIRCH:HIGH:LOW", "BIRCH:HIGH,", ",BIRCH:HIGH", "birch:high", "").forEach { levels ->
            assertEquals(generic, parseAlarmPayload(payload("kind" to "report", "levels" to levels)), levels)
        }
    }

    @Test
    fun `a report without levels is generic`() {
        assertEquals(generic, parseAlarmPayload(payload("kind" to "report")))
    }

    @Test
    fun `a missing station name falls back to the abbreviation`() {
        val content = parseAlarmPayload(payload("kind" to "no_pollen") - "stationName")

        assertEquals(AlarmNotificationContent.NoPollen("PZH", AlarmChannel.DAILY_REPORT), content)
    }

    @Test
    fun `a payload without any station is generic with an empty station`() {
        val content = parseAlarmPayload(mapOf("kind" to "mystery"))

        assertEquals(AlarmNotificationContent.Generic("", AlarmChannel.DAILY_REPORT), content)
    }

    @Test
    fun `a missing or unknown channel falls back to the daily report channel`() {
        val missing = parseAlarmPayload(payload("kind" to "unavailable") - "channel")
        val unknown = parseAlarmPayload(payload("kind" to "unavailable", "channel" to "weekly"))

        assertEquals(AlarmChannel.DAILY_REPORT, missing.channel)
        assertEquals(AlarmChannel.DAILY_REPORT, unknown.channel)
    }
}
