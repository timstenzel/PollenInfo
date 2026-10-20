package ch.stenzel.tim.polleninfo.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.ui.error.ErrorContext
import ch.stenzel.tim.polleninfo.core.ui.error.message
import ch.stenzel.tim.polleninfo.core.ui.feeling.icon
import ch.stenzel.tim.polleninfo.core.ui.feeling.label
import ch.stenzel.tim.polleninfo.core.ui.status.Banner
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.home_feeling_dismiss
import ch.stenzel.tim.polleninfo.resources.home_feeling_question
import org.jetbrains.compose.resources.stringResource

/**
 * The once-a-day question as Home's green card, between the gauge and the tiles: four face buttons
 * from Very good to Very bad, left to right, each named by its word, and "Not today". [saveError]
 * is why the last answer was not saved, shown under the faces until the next attempt.
 */
@Composable
fun FeelingPrompt(
    saveError: AppError?,
    onFeelingSelected: (Feeling) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .background(colors.primary, MaterialTheme.shapes.large)
            .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.home_feeling_question),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onPrimary,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Spacer(Modifier.width(8.dp))
            // Small as drawn, but with a full-size touch target around it.
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onDismiss)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.home_feeling_dismiss),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onPrimary.copy(alpha = DISMISS_ALPHA),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().padding(end = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // Drawn best first; the enum is declared worst first for the Diary's axis.
            Feeling.entries.reversed().forEach { feeling ->
                FaceButton(feeling, onClick = { onFeelingSelected(feeling) }, modifier = Modifier.weight(1f))
            }
        }
        if (saveError != null) {
            Banner(
                text = saveError.message(ErrorContext.SAVE_FEELING),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, end = 12.dp),
            )
        }
    }
}

/** A face on a tinted circle above its word; the word is what a screen reader announces. */
@Composable
private fun FaceButton(feeling: Feeling, onClick: () -> Unit, modifier: Modifier) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(60.dp).background(onPrimary.copy(alpha = FACE_CIRCLE_ALPHA), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(feeling.icon(), contentDescription = null, tint = onPrimary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = feeling.label(),
            style = MaterialTheme.typography.labelMedium,
            color = onPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

/** "Not today" is the quiet way out, drawn at 75 % of the card's text colour. */
private const val DISMISS_ALPHA = 0.75f

/** The face circles are the card's text colour at 14 % over the green. */
private const val FACE_CIRCLE_ALPHA = 0.14f
