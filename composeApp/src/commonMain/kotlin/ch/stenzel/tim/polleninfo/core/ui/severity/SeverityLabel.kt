package ch.stenzel.tim.polleninfo.core.ui.severity

import androidx.compose.runtime.Composable
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.severity_high
import ch.stenzel.tim.polleninfo.resources.severity_low
import ch.stenzel.tim.polleninfo.resources.severity_moderate
import ch.stenzel.tim.polleninfo.resources.severity_none
import ch.stenzel.tim.polleninfo.resources.severity_very_high
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The single home of the severity wording. Every severity is spelled out as a word — the screen
 * must stay fully readable to someone who cannot tell its colours apart.
 */
@Composable
fun PollenSeverity.label(): String = stringResource(labelResource())

/** The resource behind [label], for code outside Compose (`getString`). */
fun PollenSeverity.labelResource(): StringResource = when (this) {
    PollenSeverity.NONE -> Res.string.severity_none
    PollenSeverity.LOW -> Res.string.severity_low
    PollenSeverity.MODERATE -> Res.string.severity_moderate
    PollenSeverity.HIGH -> Res.string.severity_high
    PollenSeverity.VERY_HIGH -> Res.string.severity_very_high
}
