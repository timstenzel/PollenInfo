package ch.stenzel.tim.polleninfo.feature.allstations.map

/**
 * The station a tap on the map means: the abbreviation of the dot nearest to [tap] whose distance is
 * at most [radiusPx], or `null` when every dot is farther away — a tap on empty map does nothing.
 *
 * Nearest rather than first-within-radius, so two close dots (Locarno and Lugano) are never
 * ambiguous: each owns its side of the midpoint. An exact tie goes to the alphabetically first
 * abbreviation, so the answer never depends on map iteration order.
 *
 * Pure Kotlin on purpose: [dots] are already projected, in the same units as [tap] and [radiusPx].
 */
fun nearestStation(tap: MapPoint, dots: Map<String, MapPoint>, radiusPx: Float): String? =
    dots.entries
        .map { (abbr, dot) -> abbr to squaredDistance(tap, dot) }
        .filter { (_, distance) -> distance <= radiusPx * radiusPx }
        .minWithOrNull(compareBy<Pair<String, Float>> { it.second }.thenBy { it.first })
        ?.first

private fun squaredDistance(a: MapPoint, b: MapPoint): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return dx * dx + dy * dy
}
