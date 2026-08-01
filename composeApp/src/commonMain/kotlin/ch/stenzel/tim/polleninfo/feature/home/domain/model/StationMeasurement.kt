package ch.stenzel.tim.polleninfo.feature.home.domain.model

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
 * The wire response also carries `measuredAt` and each taxon's latin name; both are dropped here
 * because nothing renders them yet, the same way `Station` drops the canton and altitude. The
 * timestamp arrives in the domain model when the reading-age line does.
 */
data class StationMeasurement(
    val stationAbbr: String,
    val unit: String,
    val species: List<SpeciesReading>,
)
