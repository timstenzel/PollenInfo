package ch.stenzel.tim.polleninfo.core.ui.severity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
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

/**
 * A fresh reading gets an unobtrusive caption. A stale one gets a warning in the error container
 * colours with an icon, so it differs in weight and shape rather than only in wording — the backend
 * keeps serving its last reading through an upstream outage, and this is what stops that reading
 * passing for today's.
 */
@Composable
fun ReadingAgeView(age: ReadingAge) {
    when (age) {
        is ReadingAge.Fresh -> Text(
            text = age.label(),
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
                    Text(text = age.label(), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "These readings are not current.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
