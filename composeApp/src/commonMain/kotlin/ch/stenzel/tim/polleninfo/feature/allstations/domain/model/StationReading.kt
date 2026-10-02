package ch.stenzel.tim.polleninfo.feature.allstations.domain.model

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.StationPollenOverview
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station

/**
 * Where one station's reading stands on the All stations screen.
 *
 * Each station is fetched on its own, so each carries its own outcome: one slow or failing station
 * must not hold up or blank the other fourteen.
 */
sealed interface StationReading {

    val station: Station

    /** Requested, not yet answered. */
    data class Pending(override val station: Station) : StationReading

    /** The reading arrived; [overview] already carries the worst severity and the display order. */
    data class Available(
        override val station: Station,
        val overview: StationPollenOverview,
    ) : StationReading

    /**
     * The request failed — no reading published, upstream down or no connection. The screen does
     * not tell these apart: every one of them ends in "No reading" for this station.
     */
    data class Unavailable(override val station: Station) : StationReading
}
