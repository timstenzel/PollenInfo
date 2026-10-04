package ch.stenzel.tim.polleninfo.server.pollen.domain

import kotlinx.serialization.Serializable

/**
 * The 15 MeteoSwiss pollen measuring stations.
 *
 * [abbr] is the official station abbreviation used both in the CSV payloads (`station_abbr`) and,
 * lowercased, in the download paths: `<abbr>/ogd-pollen_<abbr>_h_now.csv`.
 *
 * Values taken from `ogd-pollen_meta_stations.csv` (WGS84 coordinates, altitude in m a.s.l.).
 */
@Serializable
enum class PollenStation(
    val abbr: String,
    val displayName: String,
    val canton: String,
    val latitude: Double,
    val longitude: Double,
    val altitudeMasl: Int,
) {
    BERN("PBE", "Bern", "BE", 46.950342, 7.424661, 546),
    BASEL("PBS", "Basel", "BS", 47.561800, 7.583931, 256),
    BUCHS_SG("PBU", "Buchs, SG", "SG", 47.173267, 9.472614, 446),
    LA_CHAUX_DE_FONDS("PCF", "La Chaux-de-Fonds", "NE", 47.113514, 6.832000, 1037),
    DAVOS("PDS", "Davos / Wolfgang", "GR", 46.829092, 9.855489, 1591),
    GENEVE("PGE", "Genève", "GE", 46.191969, 6.147544, 379),
    LOCARNO("PLO", "Locarno / Monti", "TI", 46.172547, 8.787389, 376),
    LAUSANNE("PLS", "Lausanne", "VD", 46.524103, 6.644825, 576),
    LUGANO("PLU", "Lugano", "TI", 46.004231, 8.960631, 275),
    LUZERN("PLZ", "Luzern", "LU", 47.057678, 8.296803, 463),
    MUENSTERLINGEN("PMU", "Münsterlingen", "TG", 47.630206, 9.236878, 415),
    NEUCHATEL("PNE", "Neuchâtel", "NE", 47.000269, 6.949828, 490),
    PAYERNE("PPY", "Payerne", "VD", 46.813403, 6.942939, 490),
    SION("PSN", "Sion", "VS", 46.235403, 7.384606, 494),
    ZUERICH("PZH", "Zürich", "ZH", 47.378225, 8.565644, 559),
    ;

    /** Path of this station's current-day hourly CSV, relative to the OGD pollen base URL. */
    val hourlyNowPath: String get() = pathFor(granularity = "h", frequency = "now")

    /** Path of this station's year-to-date hourly CSV, relative to the OGD pollen base URL. */
    val hourlyRecentPath: String get() = pathFor(granularity = "h", frequency = "recent")

    /** Path of this station's year-to-date daily CSV, relative to the OGD pollen base URL. */
    val dailyRecentPath: String get() = pathFor(granularity = "d", frequency = "recent")

    /**
     * Path of this station's daily CSV for every earlier year, ending 31 December of last year,
     * relative to the OGD pollen base URL.
     */
    val dailyHistoricalPath: String get() = pathFor(granularity = "d", frequency = "historical")

    private fun pathFor(granularity: String, frequency: String): String {
        val id = abbr.lowercase()
        return "$id/ogd-pollen_${id}_${granularity}_$frequency.csv"
    }

    companion object {
        private val byAbbr = entries.associateBy { it.abbr }

        /** Resolves an official station abbreviation, case-insensitively. */
        fun fromAbbr(abbr: String): PollenStation? = byAbbr[abbr.uppercase()]
    }
}
