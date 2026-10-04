package ch.stenzel.tim.polleninfo.core.diary.domain.model

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity

/**
 * How the user said they felt on a day, declared from worst to best — the order the Home prompt
 * shows its buttons in, left to right.
 *
 * [level] puts a feeling on the pollen scale, so the Diary can draw both on one axis: higher is
 * always worse, "Very bad" sits level with "Very high" pollen. `NONE` has no feeling counterpart.
 */
enum class Feeling(val level: PollenSeverity) {
    VERY_BAD(PollenSeverity.VERY_HIGH),
    BAD(PollenSeverity.HIGH),
    GOOD(PollenSeverity.MODERATE),
    VERY_GOOD(PollenSeverity.LOW),
}
