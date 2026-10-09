package ch.stenzel.tim.polleninfo.core.measurement.domain.model

import kotlin.time.Instant

/**
 * One taxon's reading at the selected station.
 *
 * [concentration] and [severity] are `null` together and mean **no reading** — the station does not
 * report this taxon. That is a different fact from a concentration of `0`, which is a measurement
 * of no pollen, and the screen says so in different words.
 */
data class SpeciesReading(
    val id: String,
    val name: String,
    val concentration: Int?,
    val severity: PollenSeverity?,
)

/**
 * A station's latest reading as the app holds it: all seven taxa, in the server's order.
 *
 * [measuredAt] is when the station took the reading — not when the backend fetched it, which after
 * an upstream outage can be much later. The wire response also carries each taxon's latin name;
 * that is dropped here because nothing renders it yet, the same way `Station` drops the canton and
 * altitude.
 */
data class StationMeasurement(
    val stationAbbr: String,
    val measuredAt: Instant,
    val unit: String,
    val species: List<SpeciesReading>,
)
