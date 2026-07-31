package ch.stenzel.tim.polleninfo.server.pollen.domain

import kotlinx.serialization.Serializable

/**
 * Lower bounds, in grains/m³, of the [PollenSeverity] bands for a single species.
 *
 * Each field is the *inclusive* lower bound of that band:
 *
 * ```
 * 0                 1..moderate-1   moderate..high-1   high..veryHigh-1   veryHigh..
 * NONE              LOW             MODERATE           HIGH               VERY_HIGH
 * ```
 */
@Serializable
data class SpeciesThresholds(
    val moderate: Int,
    val high: Int,
    val veryHigh: Int,
) {
    init {
        require(moderate >= 1) { "moderate lower bound must be >= 1, was $moderate" }
        require(moderate < high) { "moderate ($moderate) must be below high ($high)" }
        require(high < veryHigh) { "high ($high) must be below veryHigh ($veryHigh)" }
    }

    /** Classifies a concentration in grains/m³ into a severity band. */
    fun severityOf(concentration: Int): PollenSeverity = when {
        concentration <= 0 -> PollenSeverity.NONE
        concentration < moderate -> PollenSeverity.LOW
        concentration < high -> PollenSeverity.MODERATE
        concentration < veryHigh -> PollenSeverity.HIGH
        else -> PollenSeverity.VERY_HIGH
    }
}

/**
 * Per-species severity thresholds. **The server is the single source of truth for these** — the
 * apps never classify concentrations themselves, they render the severity the server sends and can
 * read the table from `GET /pollen/thresholds` to label their own UI.
 *
 * Thresholds are held per species rather than per species-group so any single taxon can be
 * retuned without touching the others.
 *
 * The seeded values follow the Swiss exposure classes (Gehrig et al. 2018), which currently assign
 * one set of bounds to the six tree taxa and a distinct, much lower set to grasses.
 */
class PollenThresholds(
    private val bySpecies: Map<PollenSpecies, SpeciesThresholds> = DEFAULTS,
) {
    init {
        val missing = PollenSpecies.entries - bySpecies.keys
        require(missing.isEmpty()) { "no thresholds configured for: $missing" }
    }

    fun forSpecies(species: PollenSpecies): SpeciesThresholds = bySpecies.getValue(species)

    fun severityOf(species: PollenSpecies, concentration: Int): PollenSeverity =
        forSpecies(species).severityOf(concentration)

    fun asMap(): Map<PollenSpecies, SpeciesThresholds> = bySpecies.toMap()

    companion object {
        /** Bounds shared by the six tree taxa: LOW from 1, MODERATE from 15, HIGH from 90. */
        val TREE = SpeciesThresholds(moderate = 15, high = 90, veryHigh = 1500)

        /** Grasses react at far lower concentrations than trees. */
        val GRASS = SpeciesThresholds(moderate = 5, high = 20, veryHigh = 200)

        val DEFAULTS: Map<PollenSpecies, SpeciesThresholds> = mapOf(
            PollenSpecies.ALDER to TREE,
            PollenSpecies.BIRCH to TREE,
            PollenSpecies.HAZEL to TREE,
            PollenSpecies.BEECH to TREE,
            PollenSpecies.ASH to TREE,
            PollenSpecies.OAK to TREE,
            PollenSpecies.GRASSES to GRASS,
        )
    }
}
