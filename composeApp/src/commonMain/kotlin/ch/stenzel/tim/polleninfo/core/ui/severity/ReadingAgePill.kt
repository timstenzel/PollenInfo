package ch.stenzel.tim.polleninfo.core.ui.severity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.ReadingAge
import ch.stenzel.tim.polleninfo.core.ui.format.rememberDateWording
import ch.stenzel.tim.polleninfo.core.ui.format.resolve
import ch.stenzel.tim.polleninfo.core.ui.status.Banner
import ch.stenzel.tim.polleninfo.theme.meadowColors

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
