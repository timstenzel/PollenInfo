package ch.stenzel.tim.polleninfo.feature.home.domain.model

/**
 * What the home screen shows: a station's reading plus the values derived from it.
 *
 * [overallSeverity] is derived rather than fetched. The server deliberately does not send it — it
 * is a pure function of [species], and a second copy on the wire would only give the two sides two
 * answers that can disagree.
 */
data class StationPollenOverview(
    val unit: String,
    val overallSeverity: PollenSeverity,
    val species: List<SpeciesReading>,
)
