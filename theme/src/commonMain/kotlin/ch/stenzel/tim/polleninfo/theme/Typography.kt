package ch.stenzel.tim.polleninfo.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ch.stenzel.tim.polleninfo.theme.resources.Res
import ch.stenzel.tim.polleninfo.theme.resources.fraunces_semibold
import ch.stenzel.tim.polleninfo.theme.resources.plus_jakarta_sans_bold
import ch.stenzel.tim.polleninfo.theme.resources.plus_jakarta_sans_medium
import ch.stenzel.tim.polleninfo.theme.resources.plus_jakarta_sans_regular
import ch.stenzel.tim.polleninfo.theme.resources.plus_jakarta_sans_semibold
import org.jetbrains.compose.resources.Font

/**
 * Fraunces (SemiBold only, the 72 pt optical size) for screen titles and the big severity word —
 * the `display*` and `headline*` styles. Bundled, so Android and iOS look alike.
 */
@Composable
fun frauncesFontFamily(): FontFamily = FontFamily(
    Font(Res.font.fraunces_semibold, FontWeight.SemiBold),
)

/** Plus Jakarta Sans for all other text, in the four weights the type scale uses. Bundled. */
@Composable
fun plusJakartaSansFontFamily(): FontFamily = FontFamily(
    Font(Res.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(Res.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(Res.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(Res.font.plus_jakarta_sans_bold, FontWeight.Bold),
)

/**
 * The Meadow type scale, from the Figma text styles (named beside each role). Composable because a
 * resource font is. Roles Figma has no style for are filled in between their neighbours.
 *
 * `labelSmall` is the overline: the caller upper-cases its text (a `TextStyle` cannot).
 */
@Composable
fun pollenInfoTypography(): Typography {
    val serif = frauncesFontFamily()
    val sans = plusJakartaSansFontFamily()
    fun serif(size: Int, lineHeight: Int, letterSpacing: Double) = TextStyle(
        fontFamily = serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacing.sp,
    )
    fun sans(weight: FontWeight, size: Int, lineHeight: Int, letterSpacing: Double = 0.0) = TextStyle(
        fontFamily = sans,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacing.sp,
    )
    return Typography(
        displayLarge = serif(56, 60, -1.5), // Display/Hero
        displayMedium = serif(36, 42, -0.8), // Display/Large, the screen title
        displaySmall = serif(32, 38, -0.6),
        headlineLarge = serif(30, 36, -0.5),
        headlineMedium = serif(28, 34, -0.4), // Headline/Title
        headlineSmall = serif(24, 30, -0.2),
        titleLarge = sans(FontWeight.Bold, 20, 24), // Number/Value
        titleMedium = sans(FontWeight.Bold, 18, 24), // Headline/Card
        titleSmall = sans(FontWeight.SemiBold, 14, 20),
        bodyLarge = sans(FontWeight.Medium, 16, 24), // Body/Large
        bodyMedium = sans(FontWeight.Normal, 14, 20), // Body/Default
        bodySmall = sans(FontWeight.Normal, 12, 16),
        labelLarge = sans(FontWeight.SemiBold, 14, 18), // Label/Strong
        labelMedium = sans(FontWeight.SemiBold, 12, 16), // Label/Small
        labelSmall = sans(FontWeight.Bold, 11, 14, letterSpacing = 1.2), // Label/Overline
    )
}
