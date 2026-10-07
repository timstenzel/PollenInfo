package ch.stenzel.tim.polleninfo.core.ui.feeling

import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling

/**
 * A feeling's word — on Home's prompt buttons (also what a screen reader announces for them) and on
 * the Diary chart's feeling axis, so the two cannot drift apart.
 */
fun Feeling.label(): String = when (this) {
    Feeling.VERY_BAD -> "Very bad"
    Feeling.BAD -> "Bad"
    Feeling.GOOD -> "Good"
    Feeling.VERY_GOOD -> "Very good"
}
