package ch.stenzel.tim.polleninfo.core.preferences

/**
 * The measuring station the user picked during onboarding.
 *
 * [abbr] is the identity every backend call uses. [name] is denormalized alongside it purely so any
 * screen can label the selection without a network round-trip. The trade-off is that a stored name
 * goes stale if MeteoSwiss ever renames a station — refreshing it whenever the station list is
 * fetched is an obvious later improvement.
 */
data class SelectedStation(
    val abbr: String,
    val name: String,
)
