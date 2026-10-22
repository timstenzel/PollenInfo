package ch.stenzel.tim.polleninfo.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The Meadow colours Material 3 has no role for, provided by [PollenInfoTheme] next to the
 * `ColorScheme` and read through [MaterialTheme.meadowColors].
 *
 * @property wash the soft sage of the decorative background washes and pale icon tiles.
 * @property glass the translucent fill of the glass bars (`surface` at 72 % / 70 %).
 * @property accent pollen gold, for the single main action of a screen.
 * @property onAccent text and icons on [accent].
 * @property warningContainer the background of warnings that are not errors (an outdated reading).
 * @property onWarningContainer text and icons on [warningContainer].
 * @property mapLand the fill of Switzerland on the All stations map.
 */
@Immutable
data class MeadowColors(
    val wash: Color,
    val glass: Color,
    val accent: Color,
    val onAccent: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val mapLand: Color,
)

// Ratios: WCAG 2.1 contrast of the "on" colour against its container.
val MeadowLightColors = MeadowColors(
    wash = Color(0xFFD6E6C2),
    glass = surfaceLight.copy(alpha = 0.72f),
    accent = Color(0xFFE2B93B),
    onAccent = Color(0xFF2A2207), // 8.45:1 on accent
    warningContainer = Color(0xFFF6E3C4),
    onWarningContainer = Color(0xFF5B3A06), // 8.14:1 on warningContainer
    mapLand = Color(0xFFE3EBD2),
)

val MeadowDarkColors = MeadowColors(
    wash = Color(0xFF1C3022),
    glass = surfaceDark.copy(alpha = 0.70f),
    accent = Color(0xFFF0CB5C),
    onAccent = Color(0xFF2A2207), // 10.07:1 on accent
    warningContainer = Color(0xFF3B2A10),
    onWarningContainer = Color(0xFFF5D9A8), // 10.08:1 on warningContainer
    mapLand = Color(0xFF1A281E),
)

/** The [MeadowColors] in effect; the light set outside a [PollenInfoTheme] (previews, tests). */
val LocalMeadowColors = staticCompositionLocalOf { MeadowLightColors }

/** The Meadow colours of the theme in effect, beside [MaterialTheme.colorScheme]. */
val MaterialTheme.meadowColors: MeadowColors
    @Composable
    @ReadOnlyComposable
    get() = LocalMeadowColors.current
