package ch.stenzel.tim.polleninfo.feature.onboarding.domain.model

/**
 * A MeteoSwiss pollen measuring station, as the app needs it.
 *
 * [abbr] is the official station abbreviation and the identity every backend call uses; [name] is
 * the local spelling shown to the user (Genève, Zürich, Neuchâtel). The coordinates are carried so
 * the nearest station can be computed on the device — the user's position never leaves it.
 *
 * Canton and altitude are deliberately dropped from the wire shape: nothing in this feature shows
 * them, and a domain model that carries unused fields invites screens to start displaying them.
 */
data class Station(
    val abbr: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)
