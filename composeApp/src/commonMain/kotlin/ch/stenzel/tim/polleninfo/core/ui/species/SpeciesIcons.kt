package ch.stenzel.tim.polleninfo.core.ui.species

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The icon of the species with [id]: grass blades for grasses, the leaf of its tree for each tree
 * species, and a generic leaf for an id the app does not know. Always decorative; the name beside
 * it says which species it is.
 */
fun speciesIcon(id: String): ImageVector = when (id) {
    "GRASSES" -> GrassIcon
    "ALDER" -> AlderIcon
    "BIRCH" -> BirchIcon
    "HAZEL" -> HazelIcon
    "BEECH" -> BeechIcon
    "ASH" -> AshIcon
    "OAK" -> OakIcon
    else -> Icons.Default.Eco
}

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

/** Figma's "icon/alder". Alder: obovate with a notched tip and fine teeth. */
private val AlderIcon: ImageVector by lazy {
    leafIcon(
        name = "Alder",
        fillType = PathFillType.EvenOdd,
        pathData =
            "M 10.86 16.54 L 10.2 16.13 L 9.17 15.18 L 7.01 12.58 L 6.19 12.08 L 6.41 11.75 " +
            "L 5.45 11.01 L 5.67 10.69 L 4.74 9.93 L 4.97 9.62 L 4.07 8.86 L 4.29 8.55 " +
            "L 3.89 8.11 L 3.66 7.31 L 4.07 7.25 L 4.06 6.04 L 4.45 6.07 L 4.75 4.92 L 5.12 5 " +
            "L 5.7 3.92 L 6.06 4.05 L 6.61 3.4 L 6.92 3.47 L 8.2 2.87 L 9 2.75 L 9.58 2.9 " +
            "L 10.49 3.62 L 10.84 3.37 L 11.04 3.74 L 11 3.81 L 11.23 3.46 L 11.71 3.88 " +
            "L 11.85 3.84 L 11.71 3.45 L 12.42 2.9 L 13 2.75 L 13.44 2.77 L 15.08 3.47 " +
            "L 15.45 3.31 L 16.09 4.17 L 16.47 4.01 L 16.99 5.11 L 17.38 5.01 L 17.65 6.3 " +
            "L 18.04 6.29 L 17.98 7.61 L 18.34 7.7 L 18.2 8.14 L 17.56 8.79 L 17.77 9.09 " +
            "L 16.88 9.86 L 17.09 10.17 L 16.17 10.92 L 16.38 11.24 L 15.42 11.99 L 15.63 12.31 " +
            "L 15.22 12.57 L 14.99 12.58 L 13.47 14.47 L 12.09 15.89 L 11.28 16.48 L 11 16.6 Z " +
            "M 11.45 15.4 L 11.12 5 L 10.88 5 L 10.55 15.4 Z " +
            "M 10.25 16.6 L 11.75 16.6 L 11.75 20.8 L 10.25 20.8 Z",
    )
}

/** Figma's "icon/birch". Birch: triangular with a long tip and a toothed edge. */
private val BirchIcon: ImageVector by lazy {
    leafIcon(
        name = "Birch",
        fillType = PathFillType.EvenOdd,
        pathData =
            "M 10.87 15.99 L 8.51 15.75 L 5.88 15.12 L 4.87 14.75 L 4.17 14.38 L 3.75 14 " +
            "L 3.54 13.61 L 3.72 12.77 L 3.78 11.87 L 4.57 12.24 L 4.79 11.46 L 4.86 10.41 " +
            "L 5.63 10.75 L 5.69 10.61 L 5.87 9.82 L 5.92 8.77 L 6.71 9.12 L 6.92 8.19 " +
            "L 6.93 7.15 L 7.75 7.49 L 7.92 6.57 L 7.9 5.55 L 8.73 5.87 L 8.87 4.96 L 8.81 3.97 " +
            "L 9.63 4.25 L 9.74 3.52 L 10.02 3.5 L 10.69 2 L 11 1 L 11.31 2 L 11.98 3.5 " +
            "L 12.42 3.44 L 12.43 4.05 L 12.56 4.62 L 13.34 4.36 L 13.32 5.51 L 13.48 6.25 " +
            "L 14.27 5.95 L 14.28 7.12 L 14.47 7.87 L 15.25 7.56 L 15.3 8.75 L 15.52 9.5 " +
            "L 16.28 9.18 L 16.34 10.22 L 16.55 10.99 L 17.34 10.63 L 17.41 11.7 L 17.62 12.49 " +
            "L 17.69 12.62 L 18.43 12.28 L 18.59 13.59 L 18.41 13.92 L 17.83 14.38 L 17.13 14.75 " +
            "L 16.12 15.12 L 13.49 15.75 Z " +
            "M 11.45 14.6 L 11.12 4.6 L 10.88 4.6 L 10.55 14.6 Z " +
            "M 10.25 16 L 11.75 16 L 11.75 20.8 L 10.25 20.8 Z",
    )
}

/** Figma's "icon/hazel". Hazel: round with a heart-shaped base, a short tip and a toothed edge. */
private val HazelIcon: ImageVector by lazy {
    leafIcon(
        name = "Hazel",
        fillType = PathFillType.EvenOdd,
        pathData =
            "M 11 0.6 L 12.8 3.09 L 13.31 2.8 L 13.6 3.45 L 14.21 4 L 14.59 3.59 L 15.52 4.92 " +
            "L 15.98 4.61 L 16.58 6.11 L 17.1 5.92 L 17.33 7.52 L 17.89 7.46 L 17.74 9.07 " +
            "L 18.29 9.14 L 17.77 10.67 L 18.29 10.86 L 17.42 12.23 L 17.88 12.54 L 16.73 13.67 " +
            "L 17.1 14.08 L 16.69 14.31 L 16.34 14.21 L 15.62 14.99 L 15 15.5 L 14.09 16.06 " +
            "L 13.33 16.31 L 12.5 16.26 L 11.96 16.05 L 11.45 15.69 L 11 15.2 L 10.3 15.89 " +
            "L 9.77 16.17 L 9.22 16.31 L 8.41 16.26 L 7.45 15.8 L 6.58 15.17 L 5.66 14.21 " +
            "L 5.39 14.25 L 4.72 13.85 L 5.13 13.44 L 3.99 12.28 L 4.49 11.98 L 3.65 10.58 " +
            "L 4.21 10.4 L 3.72 8.85 L 4.3 8.8 L 4.19 7.18 L 4.76 7.27 L 5.03 5.67 L 5.57 5.89 " +
            "L 6.21 4.39 L 6.68 4.74 L 7.65 3.43 L 8.03 3.88 L 8.52 3.41 L 8.76 2.88 L 9.2 3.09 " +
            "Z " +
            "M 11.45 13.4 L 11.12 3.6 L 10.88 3.6 L 10.55 13.4 Z " +
            "M 10.25 15.58 L 11.75 15.58 L 11.75 20.8 L 10.25 20.8 Z",
    )
}

/** Figma's "icon/beech". Beech: a smooth oval. */
private val BeechIcon: ImageVector by lazy {
    leafIcon(
        name = "Beech",
        fillType = PathFillType.EvenOdd,
        pathData =
            "M 10.18 17.1 L 8.84 16.1 L 7.79 15.1 L 6.97 14.1 L 6.34 13.1 L 5.9 12.1 L 5.64 11.1 " +
            "L 5.56 10.1 L 5.66 9.1 L 5.92 8.1 L 6.32 7.1 L 6.99 5.9 L 7.82 4.7 L 8.99 3.3 " +
            "L 10.18 2.1 L 11 1.5 L 11.82 2.1 L 13.01 3.3 L 14.18 4.7 L 15.01 5.9 L 15.68 7.1 " +
            "L 16.08 8.1 L 16.34 9.1 L 16.44 10.1 L 16.36 11.1 L 16.1 12.1 L 15.66 13.1 " +
            "L 15.03 14.1 L 14.21 15.1 L 13.16 16.1 L 11.82 17.1 L 11 17.5 Z " +
            "M 11.45 15.8 L 11.12 4.2 L 10.88 4.2 L 10.55 15.8 Z " +
            "M 10.25 17.5 L 11.75 17.5 L 11.75 20.8 L 10.25 20.8 Z",
    )
}

/** Figma's "icon/ash". Ash: pinnate, three pairs of leaflets and one at the tip. */
private val AshIcon: ImageVector by lazy {
    leafIcon(
        name = "Ash",
        fillType = PathFillType.NonZero,
        pathData =
            "M 10.3 4.6 L 11.7 4.6 L 11.7 20.8 L 10.3 20.8 Z " +
            "M 11 6.4 L 10.74 6.3 L 10.41 6.02 L 10.09 5.58 L 9.83 5 L 9.66 4.32 L 9.6 3.6 " +
            "L 9.66 2.88 L 9.83 2.2 L 10.09 1.62 L 10.41 1.18 L 10.74 0.9 L 11 0.8 L 11.26 0.9 " +
            "L 11.59 1.18 L 11.91 1.62 L 12.17 2.2 L 12.34 2.88 L 12.4 3.6 L 12.34 4.32 " +
            "L 12.17 5 L 11.91 5.58 L 11.59 6.02 L 11.26 6.3 Z " +
            "M 10.08 5.33 L 10.11 5.58 L 10.01 5.97 L 9.77 6.42 L 9.38 6.89 L 8.88 7.33 " +
            "L 8.29 7.7 L 7.64 7.98 L 7 8.16 L 6.39 8.21 L 5.88 8.16 L 5.51 8.03 L 5.32 7.87 " +
            "L 5.29 7.62 L 5.39 7.23 L 5.63 6.78 L 6.02 6.31 L 6.52 5.87 L 7.11 5.5 L 7.76 5.22 " +
            "L 8.4 5.04 L 9.01 4.99 L 9.52 5.04 L 9.89 5.17 Z " +
            "M 16.68 7.87 L 16.49 8.03 L 16.12 8.16 L 15.61 8.21 L 15 8.16 L 14.36 7.98 " +
            "L 13.71 7.7 L 13.12 7.33 L 12.62 6.89 L 12.23 6.42 L 11.99 5.97 L 11.89 5.58 " +
            "L 11.92 5.33 L 12.11 5.17 L 12.48 5.04 L 12.99 4.99 L 13.6 5.04 L 14.24 5.22 " +
            "L 14.89 5.5 L 15.48 5.87 L 15.98 6.31 L 16.37 6.78 L 16.61 7.23 L 16.71 7.62 Z " +
            "M 10.08 9.33 L 10.11 9.58 L 10.01 9.97 L 9.77 10.42 L 9.38 10.89 L 8.88 11.33 " +
            "L 8.29 11.7 L 7.64 11.98 L 7 12.16 L 6.39 12.21 L 5.88 12.16 L 5.51 12.03 " +
            "L 5.32 11.87 L 5.29 11.62 L 5.39 11.23 L 5.63 10.78 L 6.02 10.31 L 6.52 9.87 " +
            "L 7.11 9.5 L 7.76 9.22 L 8.4 9.04 L 9.01 8.99 L 9.52 9.04 L 9.89 9.17 Z " +
            "M 16.68 11.87 L 16.49 12.03 L 16.12 12.16 L 15.61 12.21 L 15 12.16 L 14.36 11.98 " +
            "L 13.71 11.7 L 13.12 11.33 L 12.62 10.89 L 12.23 10.42 L 11.99 9.97 L 11.89 9.58 " +
            "L 11.92 9.33 L 12.11 9.17 L 12.48 9.04 L 12.99 8.99 L 13.6 9.04 L 14.24 9.22 " +
            "L 14.89 9.5 L 15.48 9.87 L 15.98 10.31 L 16.37 10.78 L 16.61 11.23 L 16.71 11.62 Z " +
            "M 10.08 13.33 L 10.11 13.58 L 10.01 13.97 L 9.77 14.42 L 9.38 14.89 L 8.88 15.33 " +
            "L 8.29 15.7 L 7.64 15.98 L 7 16.16 L 6.39 16.21 L 5.88 16.16 L 5.51 16.03 " +
            "L 5.32 15.87 L 5.29 15.62 L 5.39 15.23 L 5.63 14.78 L 6.02 14.31 L 6.52 13.87 " +
            "L 7.11 13.5 L 7.76 13.22 L 8.4 13.04 L 9.01 12.99 L 9.52 13.04 L 9.89 13.17 Z " +
            "M 16.68 15.87 L 16.49 16.03 L 16.12 16.16 L 15.61 16.21 L 15 16.16 L 14.36 15.98 " +
            "L 13.71 15.7 L 13.12 15.33 L 12.62 14.89 L 12.23 14.42 L 11.99 13.97 L 11.89 13.58 " +
            "L 11.92 13.33 L 12.11 13.17 L 12.48 13.04 L 12.99 12.99 L 13.6 13.04 L 14.24 13.22 " +
            "L 14.89 13.5 L 15.48 13.87 L 15.98 14.31 L 16.37 14.78 L 16.61 15.23 L 16.71 15.62 " +
            "Z",
    )
}

/** Figma's "icon/oak". Oak: rounded lobes, alternating left and right. */
private val OakIcon: ImageVector by lazy {
    leafIcon(
        name = "Oak",
        fillType = PathFillType.EvenOdd,
        pathData =
            "M 10.57 17.66 L 10.14 17.19 L 9.98 16.43 L 10.08 16.16 L 10.29 16.02 L 9.84 15.75 " +
            "L 9.34 15.2 L 8.93 14.59 L 8.68 13.97 L 8.62 13.43 L 8.77 12.95 L 9.12 12.61 " +
            "L 9.51 12.47 L 8.84 12.2 L 8.18 11.72 L 7.63 11.1 L 7.34 10.49 L 7.34 9.94 " +
            "L 7.59 9.46 L 8.05 9.12 L 8.73 8.92 L 7.58 8.51 L 6.8 8.03 L 6.15 7.34 L 5.95 6.73 " +
            "L 6.09 6.25 L 6.5 5.84 L 8.28 5.29 L 7.46 5.09 L 6.86 4.61 L 6.77 4.06 L 7.14 3.45 " +
            "L 8.43 2.49 L 10.13 1.74 L 10.06 1.67 L 10.2 1.54 L 11 1.4 L 12.37 1.6 L 13.42 2.15 " +
            "L 13.79 2.56 L 13.87 2.97 L 13.62 3.31 L 13.02 3.52 L 14.13 3.86 L 15.29 4.47 " +
            "L 16.06 5.16 L 16.32 5.77 L 16.01 6.11 L 15.24 6.59 L 14.36 6.93 L 13.68 7.07 " +
            "L 13.69 7.14 L 14.45 7.34 L 14.99 7.69 L 15.31 8.17 L 15.34 8.71 L 15.08 9.26 " +
            "L 14.5 9.87 L 13.77 10.35 L 12.87 10.69 L 13.46 10.9 L 13.83 11.24 L 14.02 11.72 " +
            "L 13.98 12.27 L 13.71 12.88 L 13.23 13.5 L 12.68 13.97 L 12.1 14.25 L 12.42 14.38 " +
            "L 12.66 14.72 L 12.72 15.13 L 12.62 15.68 L 12.31 16.37 L 11.79 17.12 L 11.86 17.19 " +
            "L 11.77 17.32 L 11.43 17.66 L 11 17.8 Z " +
            "M 11.5 16.2 L 11.12 3.6 L 10.88 3.6 L 10.5 16.2 Z " +
            "M 10.25 17.8 L 11.75 17.8 L 11.75 20.8 L 10.25 20.8 Z",
    )
}

/**
 * A filled leaf on the 22 × 22 grid of the grass icon. The outline, the stalk and (with
 * [PathFillType.EvenOdd]) the midrib cut out of the blade are one path, drawn in Figma.
 */
private fun leafIcon(name: String, fillType: PathFillType, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 22.dp,
        defaultHeight = 22.dp,
        viewportWidth = 22f,
        viewportHeight = 22f,
    ).addPath(
        pathData = addPathNodes(pathData),
        pathFillType = fillType,
        // Black only as the mask, as for the grass.
        fill = SolidColor(Color.Black),
    ).build()
