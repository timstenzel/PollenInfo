package ch.stenzel.tim.polleninfo.core.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.theme.meadowColors

/**
 * A warning that is not an error (Figma "Banner"): [text] beside a decorative [icon] in the warning
 * container colours, for a partial problem the screen keeps working through — an outdated reading,
 * an answer that could not be saved.
 */
@Composable
fun Banner(text: String, modifier: Modifier = Modifier, icon: ImageVector = Icons.Default.Warning) {
    val meadow = MaterialTheme.meadowColors
    Row(
        modifier = modifier
            .background(meadow.warningContainer, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Decorative: the text carries the whole message.
        Icon(imageVector = icon, contentDescription = null, tint = meadow.onWarningContainer, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = meadow.onWarningContainer,
            modifier = Modifier.weight(1f),
        )
    }
}
