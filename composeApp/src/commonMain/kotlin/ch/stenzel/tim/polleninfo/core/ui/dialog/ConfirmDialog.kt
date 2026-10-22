package ch.stenzel.tim.polleninfo.core.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.ui.button.PillButton
import ch.stenzel.tim.polleninfo.core.ui.button.PillButtonStyle
import ch.stenzel.tim.polleninfo.theme.meadowColors

/** The colour of a [ConfirmDialog]'s icon badge. */
enum class DialogTone {
    /** Sage badge: a question without loss (discard edits, log out). */
    Neutral,

    /** Warm badge with a red icon: something is deleted for good. */
    Destructive,
}

/**
 * The Meadow confirmation dialog (Figma "Dialog"): a centred [icon] badge, the serif [title], the
 * centred [text], optional [extraContent] (e.g. a password field), and the confirm and dismiss
 * buttons stacked full width — confirm on top in [confirmStyle], dismiss as a text button.
 *
 * The icon is decorative; the title is announced as a heading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDialog(
    icon: ImageVector,
    tone: DialogTone,
    title: String,
    text: String,
    confirmLabel: String,
    confirmStyle: PillButtonStyle,
    onConfirm: () -> Unit,
    dismissLabel: String,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    confirmBusy: Boolean = false,
    dismissEnabled: Boolean = true,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = 16.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, top = 28.dp, end = 24.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IconBadge(icon, tone)
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                extraContent?.invoke(this)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Spacer(Modifier.height(2.dp))
                    PillButton(
                        onClick = onConfirm,
                        style = confirmStyle,
                        enabled = confirmEnabled,
                        busy = confirmBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(confirmLabel) }
                    PillButton(
                        onClick = onDismiss,
                        style = PillButtonStyle.Text,
                        enabled = dismissEnabled,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(dismissLabel) }
                }
            }
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector, tone: DialogTone) {
    val (container, tint) = when (tone) {
        DialogTone.Neutral -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        DialogTone.Destructive -> MaterialTheme.meadowColors.warningContainer to MaterialTheme.colorScheme.error
    }
    Box(
        modifier = Modifier.size(56.dp).background(container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // Decorative: the title says what is asked.
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
    }
}
