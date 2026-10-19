package ch.stenzel.tim.polleninfo.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The Meadow corner radii: chips and small tiles 12 dp, fields and tiles 20 dp, cards 28 dp,
 * dialogs and the glass bars 36 dp. Pills use `CircleShape`.
 */
val PollenInfoShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)
