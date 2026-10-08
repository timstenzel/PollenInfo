package ch.stenzel.tim.polleninfo.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.ui.feeling.label
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.home_feeling_dismiss
import ch.stenzel.tim.polleninfo.resources.home_feeling_question
import ch.stenzel.tim.polleninfo.resources.home_feeling_save_error
import org.jetbrains.compose.resources.stringResource

/**
 * The once-a-day question floated over Home's list: four full-word answers from Very bad to Very
 * good, left to right, and a close button announced as "Not today".
 */
@Composable
fun FeelingPrompt(
    saveError: Boolean,
    onFeelingSelected: (Feeling) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.home_feeling_question),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.home_feeling_dismiss))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Feeling.entries.forEach { feeling ->
                    FilledTonalButton(
                        onClick = { onFeelingSelected(feeling) },
                        modifier = Modifier.weight(1f),
                        // Four buttons share a phone's width; the default padding would leave
                        // "Very good" too little room.
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    ) {
                        Text(feeling.label(), textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
            }
            if (saveError) {
                Text(
                    text = stringResource(Res.string.home_feeling_save_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp, end = 12.dp),
                )
            }
        }
    }
}
