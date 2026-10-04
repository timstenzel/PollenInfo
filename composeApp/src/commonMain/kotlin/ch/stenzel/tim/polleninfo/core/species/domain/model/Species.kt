package ch.stenzel.tim.polleninfo.core.species.domain.model

/**
 * One of the pollen types the backend measures, as the app needs it without a reading.
 *
 * [id] is the backend's species id (`"BIRCH"`) — the identity alarms store and every reading
 * carries; [name] is what the user sees.
 *
 * Loaded from `GET /pollen/species` rather than declared here as an enum: `:server`'s
 * `PollenSpecies` stays the one authoritative vocabulary, and the app has no use for the ids beyond
 * echoing them back.
 */
data class Species(
    val id: String,
    val name: String,
)
