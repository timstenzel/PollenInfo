package ch.stenzel.tim.polleninfo.server.pollen.domain

import kotlinx.serialization.Serializable

/**
 * The seven taxa measured by the MeteoSwiss automatic pollen monitoring network (SwissPollen,
 * automatic method since 2023-01-01).
 *
 * [hourlyCode] and [dailyCode] are the MeteoSwiss parameter short names used as CSV column
 * headers. The daily code is the `d0` variant (06 UTC to 06 UTC of the following day); the `d1`
 * variant (00-00 UTC) exists as well but is not used here.
 *
 * See `ogd-pollen_meta_parameters.csv`.
 */
@Serializable
enum class PollenSpecies(
    val displayName: String,
    val latinName: String,
    val hourlyCode: String,
    val dailyCode: String,
) {
    ALDER("Alder", "Alnus", "kaalnuh0", "kaalnud0"),
    BIRCH("Birch", "Betula", "kabetuh0", "kabetud0"),
    HAZEL("Hazel", "Corylus", "kacoryh0", "kacoryd0"),
    BEECH("Beech", "Fagus", "kafaguh0", "kafagud0"),
    ASH("Ash", "Fraxinus", "kafraxh0", "kafraxd0"),
    OAK("Oak", "Quercus", "kaquerh0", "kaquerd0"),
    GRASSES("Grasses", "Poaceae", "khpoach0", "khpoacd0"),
    ;

    companion object {
        private val byHourlyCode = entries.associateBy { it.hourlyCode }
        private val byDailyCode = entries.associateBy { it.dailyCode }

        /** Resolves a CSV column header to a species, or `null` for non-pollen columns. */
        fun fromHourlyCode(code: String): PollenSpecies? = byHourlyCode[code]

        fun fromDailyCode(code: String): PollenSpecies? = byDailyCode[code]
    }
}
