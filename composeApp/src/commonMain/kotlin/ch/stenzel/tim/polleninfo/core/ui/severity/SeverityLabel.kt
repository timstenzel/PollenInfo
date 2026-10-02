package ch.stenzel.tim.polleninfo.core.ui.severity

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity

/**
 * The single home of the severity wording. Every severity is spelled out as a word — the screen
 * must stay fully readable to someone who cannot tell its colours apart.
 */
fun PollenSeverity.label(): String = when (this) {
    PollenSeverity.NONE -> "None"
    PollenSeverity.LOW -> "Low"
    PollenSeverity.MODERATE -> "Moderate"
    PollenSeverity.HIGH -> "High"
    PollenSeverity.VERY_HIGH -> "Very high"
}
