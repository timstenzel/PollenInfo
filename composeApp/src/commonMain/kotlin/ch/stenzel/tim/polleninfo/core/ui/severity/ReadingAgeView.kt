package ch.stenzel.tim.polleninfo.core.ui.severity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.ReadingAge
import ch.stenzel.tim.polleninfo.core.ui.format.rememberDateWording
import ch.stenzel.tim.polleninfo.core.ui.format.resolve
import ch.stenzel.tim.polleninfo.core.ui.status.Banner
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.reading_not_current
import ch.stenzel.tim.polleninfo.theme.meadowColors
import org.jetbrains.compose.resources.stringResource

/**
 * A fresh reading gets an unobtrusive caption. A stale one gets a warning in the error container
 * colours with an icon, so it differs in weight and shape rather than only in wording — the backend
 * keeps serving its last reading through an upstream outage, and this is what stops that reading
 * passing for today's.
 */
@Composable
fun ReadingAgeView(age: ReadingAge) {
    val text = age.label(rememberDateWording()).resolve()
    when (age) {
        is ReadingAge.Fresh -> Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        is ReadingAge.Stale -> Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Decorative: the text beside it carries the whole message.
                Icon(imageVector = Icons.Default.Warning, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(text = text, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = stringResource(Res.string.reading_not_current),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

/**
 * The reading's time as a pill with a clock (Home's gauge card): sage for a fresh reading, the
 * warning colours for a stale one. A stale reading also gets a [Banner] saying it is not current —
 * the pill alone would make the difference a matter of colour.
 */
@Composable
fun ReadingAgePill(age: ReadingAge, modifier: Modifier = Modifier) {
    val (container, content) = when (age) {
        is ReadingAge.Fresh -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        is ReadingAge.Stale -> MaterialTheme.meadowColors.warningContainer to MaterialTheme.meadowColors.onWarningContainer
    }
    Row(
        modifier = modifier
            .background(container, CircleShape)
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Decorative: the time beside it says what it is.
        Icon(Icons.Default.Schedule, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = age.label(rememberDateWording()).resolve(), style = MaterialTheme.typography.labelMedium, color = content)
    }
}
