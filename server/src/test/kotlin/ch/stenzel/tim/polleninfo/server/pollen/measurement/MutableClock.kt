package ch.stenzel.tim.polleninfo.server.pollen.measurement

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** A clock that only moves when told to, so time-dependent behaviour is tested without waiting. */
class MutableClock(var now: Instant = Instant.parse("2026-08-01T09:00:00Z")) : Clock() {

    fun advanceBy(duration: Duration) {
        now += duration
    }

    override fun instant(): Instant = now

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this
}
