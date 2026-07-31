package ch.stenzel.tim.polleninfo.server.pollen.domain

import kotlinx.serialization.Serializable

/**
 * Severity band of a pollen concentration.
 *
 * Declared from least to most severe, so [ordinal] comparisons are meaningful and a user's
 * "only notify me above X" threshold can be expressed as `severity >= X`.
 */
@Serializable
enum class PollenSeverity {
    NONE,
    LOW,
    MODERATE,
    HIGH,
    VERY_HIGH,
    ;

    /** True when this severity is at least as severe as [other]. */
    fun atLeast(other: PollenSeverity): Boolean = this >= other
}
