package ch.stenzel.tim.polleninfo.server.pollen.domain

import java.time.ZoneId

/**
 * Swiss time, the zone in which every "which day is it" question on this server is answered —
 * the publisher's daily rows, the history window and every alarm's days and times. Daylight saving
 * is `java.time`'s job.
 *
 * It lives here rather than with the alarms because `pollen/` must not import from `alarm/`;
 * `ALARM_ZONE` is an alias of it.
 */
val SWISS_ZONE: ZoneId = ZoneId.of("Europe/Zurich")
