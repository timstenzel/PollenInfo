package ch.stenzel.tim.polleninfo.core.ui.status

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.ui.layout.MeadowCard
import ch.stenzel.tim.polleninfo.theme.meadowColors

/** The emblem's colours: warm for something that went wrong, sage for an empty or gated screen. */
enum class StatusTone {
    Warning,
    Neutral,
}

/**
 * A whole-screen state as an illustrated card (Figma "ErrorState"): an [icon] in two soft rings, the
 * serif [title] as a heading, the [body] and the [actions] full width below. Used for load errors —
 * with the existing wording and "Retry" — and for empty and gated states.
 *
 * Centred by default; a gate that goes on to list points (the Diary's) sets [centered] to `false`
 * and reads from the start edge, emblem included. [grains] adds the pollen-gold grains around the
 * emblem — by default on neutral cards only; a gate that asks for something (Alarms' notifications)
 * has them on its warning emblem too.
 */
@Composable
fun StatusCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.Warning,
    icon: ImageVector = Icons.Default.CloudOff,
    centered: Boolean = true,
    grains: Boolean = tone == StatusTone.Neutral,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start
    MeadowCard(modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 24.dp),
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            Emblem(icon, tone, grains)
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = textAlign,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign,
            )
            Spacer(Modifier.height(22.dp))
            actions()
        }
    }
}

/**
 * The 140 dp illustration: a pale outer ring, a fuller inner disc and the icon, with [grains] three
 * pollen-gold grains around it. Decorative.
 */
@Composable
private fun Emblem(icon: ImageVector, tone: StatusTone, grains: Boolean) {
    val (disc, ink) = when (tone) {
        StatusTone.Warning -> MaterialTheme.meadowColors.warningContainer to MaterialTheme.meadowColors.onWarningContainer
        StatusTone.Neutral -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    val grain = MaterialTheme.meadowColors.accent
    Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(disc.copy(alpha = OUTER_RING_ALPHA))
            drawCircle(disc, radius = 48.dp.toPx())
            if (grains) {
                drawCircle(grain, radius = 6.dp.toPx(), center = Offset(124.dp.toPx(), 20.dp.toPx()))
                drawCircle(grain, radius = 4.5.dp.toPx(), center = Offset(12.5.dp.toPx(), 104.5.dp.toPx()))
                drawCircle(grain, radius = 3.5.dp.toPx(), center = Offset(131.5.dp.toPx(), 111.5.dp.toPx()))
            }
        }
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(44.dp))
    }
}

private const val OUTER_RING_ALPHA = 0.55f
