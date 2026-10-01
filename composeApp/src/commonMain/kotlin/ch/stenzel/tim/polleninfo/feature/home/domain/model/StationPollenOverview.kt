package ch.stenzel.tim.polleninfo.feature.home.domain.model

/**
 * What the home screen shows: a station's reading plus the values derived from it.
 *
 * [overallSeverity], [drivenBy] and the order of [species] are derived rather than fetched. The
 * server deliberately sends none of them — each is a pure function of the taxa, and a second copy on
 * the wire would only give the two sides two answers that can disagree.
 *
 * [species] is ordered worst first, taxa with no reading last, alphabetically within equal severity.
 * [drivenBy] is the taxon responsible for [overallSeverity] — the first of [species] — and is `null`
 * only when no taxon has a reading at all.
 */
data class StationPollenOverview(
    val unit: String,
    val overallSeverity: PollenSeverity,
    val drivenBy: SpeciesReading?,
    val species: List<SpeciesReading>,
)
