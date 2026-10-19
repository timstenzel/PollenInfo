package ch.stenzel.tim.polleninfo.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * The Meadow theme, the same on Android and iOS: its own light or dark scheme following the system
 * setting — never the wallpaper's dynamic colours — the bundled fonts, the Meadow shapes and the
 * extended [MeadowColors].
 */
@Composable
fun PollenInfoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalMeadowColors provides if (darkTheme) MeadowDarkColors else MeadowLightColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) MeadowDarkColorScheme else MeadowLightColorScheme,
            typography = pollenInfoTypography(),
            shapes = PollenInfoShapes,
            content = content,
        )
    }
}
