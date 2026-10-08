package ch.stenzel.tim.polleninfo.core.ui.feeling

import androidx.compose.runtime.Composable
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.feeling_bad
import ch.stenzel.tim.polleninfo.resources.feeling_good
import ch.stenzel.tim.polleninfo.resources.feeling_very_bad
import ch.stenzel.tim.polleninfo.resources.feeling_very_good
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A feeling's word — on Home's prompt buttons (also what a screen reader announces for them) and on
 * the Diary chart's feeling axis, so the two cannot drift apart.
 */
@Composable
fun Feeling.label(): String = stringResource(labelResource())

/** The resource behind [label], for code outside Compose (`getString`). */
fun Feeling.labelResource(): StringResource = when (this) {
    Feeling.VERY_BAD -> Res.string.feeling_very_bad
    Feeling.BAD -> Res.string.feeling_bad
    Feeling.GOOD -> Res.string.feeling_good
    Feeling.VERY_GOOD -> Res.string.feeling_very_good
}
