package ch.stenzel.tim.polleninfo.server.pollen.measurement

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSeverity
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenSpecies
import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import java.time.Instant

/**
 * One taxon's reading at a station.
 *
 * [concentration] and [severity] are `null` **together** and mean "no reading" — the station does
 * not report this taxon, or its cell was empty. A station that does not measure ash is not a
 * station reporting no ash, so this is deliberately distinct from a [concentration] of `0`, which
 * classifies as [PollenSeverity.NONE].
 */
data class SpeciesMeasurement(
    val species: PollenSpecies,
    val concentration: Int?,
    val severity: PollenSeverity?,
)

/**
 * A station's most recent usable reading, classified.
 *
 * [species] carries **all seven** taxa in [PollenSpecies] declaration order, so a client never has
 * to distinguish "absent from the list" from "no reading". Neither the maximum severity nor a
 * display order is computed here: both are pure functions of this list and are presentation rules,
 * and a derived value on the wire only invites the two sides to disagree.
 */
data class StationMeasurement(
    val station: PollenStation,
    val measuredAt: Instant,
    val species: List<SpeciesMeasurement>,
)
