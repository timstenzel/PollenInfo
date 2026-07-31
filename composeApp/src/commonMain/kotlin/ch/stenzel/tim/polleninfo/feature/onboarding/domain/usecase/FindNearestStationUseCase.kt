package ch.stenzel.tim.polleninfo.feature.onboarding.domain.usecase

import ch.stenzel.tim.polleninfo.feature.onboarding.domain.model.Station
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The station closest to a position, as the crow flies.
 *
 * A pure function with no collaborators: the station list is already in hand when this runs, so
 * asking the backend would cost a request, a new failure state mid-flow, and the user's coordinates
 * leaving the device. This is geometry, not the domain policy `:server` deliberately owns.
 *
 * **No distance cap.** A position in Germany still yields the nearest Swiss station — the user
 * confirms the proposal, so a far-away answer is visible and overridable rather than wrong.
 */
class FindNearestStationUseCase {

    /**
     * Returns `null` only for an empty [stations] list, which cannot happen in the real flow — the
     * list is non-empty by the time the location button exists — but keeps the signature honest.
     *
     * Ties go to the first entry, which `minByOrNull` guarantees. Since the list arrives sorted
     * alphabetically, that makes the outcome deterministic and assertable rather than arbitrary.
     */
    operator fun invoke(latitude: Double, longitude: Double, stations: List<Station>): Station? =
        stations.minByOrNull { station ->
            // Comparing central angles is enough — Earth's radius is a positive constant, so
            // multiplying by it cannot reorder anything.
            centralAngle(latitude, longitude, station.latitude, station.longitude)
        }
}

/**
 * Haversine central angle between two WGS84 points, in radians.
 *
 * `kotlin.math` only: `java.*` would compile for Android and then break the iOS build.
 */
private fun centralAngle(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = (lat2 - lat1).toRadians()
    val dLon = (lon2 - lon1).toRadians()
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1.toRadians()) * cos(lat2.toRadians()) * sin(dLon / 2) * sin(dLon / 2)
    // min(1.0, …) guards asin against a value a hair above 1 from floating-point rounding.
    return 2 * asin(min(1.0, sqrt(a)))
}

private fun Double.toRadians(): Double = this * PI / 180
