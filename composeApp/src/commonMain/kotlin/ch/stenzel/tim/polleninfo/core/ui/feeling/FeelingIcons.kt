package ch.stenzel.tim.polleninfo.core.ui.feeling

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling

/**
 * The face drawn for a feeling (Figma "icon/vgood" … "icon/vbad"): a ring, two eyes and a mouth that
 * curves up for the good feelings and down for the bad ones, deeper for "very". Decorative — the
 * feeling's word is always beside it.
 */
fun Feeling.icon(): ImageVector = when (this) {
    Feeling.VERY_GOOD -> VeryGoodFace
    Feeling.GOOD -> GoodFace
    Feeling.BAD -> BadFace
    Feeling.VERY_BAD -> VeryBadFace
}

private val VeryGoodFace by lazy {
    face("VeryGood") {
        moveTo(9.375f, 17.25f)
        curveTo(10.625f, 20.25f, 12.625f, 21.625f, 15f, 21.625f)
        curveTo(17.375f, 21.625f, 19.375f, 20.25f, 20.625f, 17.25f)
    }
}

private val GoodFace by lazy {
    face("Good") {
        moveTo(10.625f, 18.25f)
        curveTo(11.75f, 19.75f, 13.25f, 20.5f, 15f, 20.5f)
        curveTo(16.75f, 20.5f, 18.25f, 19.75f, 19.375f, 18.25f)
    }
}

private val BadFace by lazy {
    face("Bad") {
        moveTo(10.625f, 20.75f)
        curveTo(11.75f, 19.375f, 13.25f, 18.75f, 15f, 18.75f)
        curveTo(16.75f, 18.75f, 18.25f, 19.375f, 19.375f, 20.75f)
    }
}

private val VeryBadFace by lazy {
    face("VeryBad") {
        moveTo(9.375f, 22f)
        curveTo(10.625f, 19.375f, 12.625f, 18.125f, 15f, 18.125f)
        curveTo(17.375f, 18.125f, 19.375f, 19.375f, 20.625f, 22f)
    }
}

/** A 30 dp face with the given [mouth]; black only as the mask an `Icon` tints. */
private fun face(name: String, mouth: PathBuilder.() -> Unit): ImageVector {
    val ink = SolidColor(Color.Black)
    return ImageVector.Builder(
        name = "Face$name",
        defaultWidth = 30.dp,
        defaultHeight = 30.dp,
        viewportWidth = 30f,
        viewportHeight = 30f,
    ).apply {
        // The ring: a circle of radius 11.875 around the centre.
        path(stroke = ink, strokeLineWidth = STROKE) {
            moveTo(26.875f, 15f)
            arcTo(11.875f, 11.875f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 3.125f, y1 = 15f)
            arcTo(11.875f, 11.875f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 26.875f, y1 = 15f)
            close()
        }
        // The eyes: dots of radius 1.5625.
        listOf(11.25f, 18.75f).forEach { x ->
            path(fill = ink) {
                moveTo(x + EYE, 12.5f)
                arcTo(EYE, EYE, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = x - EYE, y1 = 12.5f)
                arcTo(EYE, EYE, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = x + EYE, y1 = 12.5f)
                close()
            }
        }
        path(stroke = ink, strokeLineWidth = STROKE, strokeLineCap = StrokeCap.Round, pathBuilder = mouth)
    }.build()
}

private const val STROKE = 1.7f
private const val EYE = 1.5625f
