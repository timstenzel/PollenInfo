package ch.stenzel.tim.polleninfo.core.ui.species

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The icon of the species with [id]: grass blades for grasses, a leaf for every tree — and for an
 * id the app does not know, since all but one of the taxa are trees. Always decorative; the name
 * beside it says which species it is.
 */
fun speciesIcon(id: String): ImageVector = if (id == "GRASSES") GrassIcon else Icons.Default.Eco

/** Figma's "icon/grass": four stroked blades, tinted like any icon. */
private val GrassIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Grass",
        defaultWidth = 22.dp,
        defaultHeight = 22.dp,
        viewportWidth = 22f,
        viewportHeight = 22f,
    ).apply {
        path(
            // Black only as the mask: an `Icon` tints the whole vector with its own colour.
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(3.667f, 19.25f)
            curveTo(4.125f, 15.125f, 5.5f, 11.917f, 7.792f, 9.625f)
            moveTo(8.708f, 19.25f)
            curveTo(8.8f, 13.75f, 9.625f, 8.708f, 11f, 3.667f)
            moveTo(13.75f, 19.25f)
            curveTo(13.648f, 16.69f, 13.831f, 14.416f, 14.396f, 12.349f)
            curveTo(14.843f, 10.714f, 15.529f, 9.208f, 16.5f, 7.792f)
            moveTo(14.396f, 12.349f)
            curveTo(16.538f, 13.82f, 17.791f, 16.179f, 18.333f, 19.25f)
        }
    }.build()
}
