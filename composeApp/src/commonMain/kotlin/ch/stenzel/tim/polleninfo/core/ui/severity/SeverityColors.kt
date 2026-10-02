package ch.stenzel.tim.polleninfo.core.ui.severity

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.theme.severityHighDark
import ch.stenzel.tim.polleninfo.theme.severityHighLight
import ch.stenzel.tim.polleninfo.theme.severityLowDark
import ch.stenzel.tim.polleninfo.theme.severityLowLight
import ch.stenzel.tim.polleninfo.theme.severityModerateDark
import ch.stenzel.tim.polleninfo.theme.severityModerateLight
import ch.stenzel.tim.polleninfo.theme.severityNoneDark
import ch.stenzel.tim.polleninfo.theme.severityNoneLight
import ch.stenzel.tim.polleninfo.theme.severityVeryHighDark
import ch.stenzel.tim.polleninfo.theme.severityVeryHighLight

/**
 * The colour of [severity] in a light or dark scheme. Lives here rather than in `:theme` because
 * the theme module cannot depend on the app and so cannot see [PollenSeverity].
 *
 * Colour never carries meaning alone — every bar painted with one of these sits beside its
 * severity word.
 */
fun severityColor(severity: PollenSeverity, darkTheme: Boolean): Color = when (severity) {
    PollenSeverity.NONE -> if (darkTheme) severityNoneDark else severityNoneLight
    PollenSeverity.LOW -> if (darkTheme) severityLowDark else severityLowLight
    PollenSeverity.MODERATE -> if (darkTheme) severityModerateDark else severityModerateLight
    PollenSeverity.HIGH -> if (darkTheme) severityHighDark else severityHighLight
    PollenSeverity.VERY_HIGH -> if (darkTheme) severityVeryHighDark else severityVeryHighLight
}

/**
 * [severityColor] for the scheme actually in effect.
 *
 * Dark is read off the applied surface rather than `isSystemInDarkTheme()`, so it follows whatever
 * `PollenInfoTheme` was given — including a `darkTheme` override and Android's dynamic schemes.
 */
@Composable
fun PollenSeverity.color(): Color =
    severityColor(this, darkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f)
