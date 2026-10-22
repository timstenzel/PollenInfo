package ch.stenzel.tim.polleninfo.core.ui.field

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * The Meadow text field (Figma "TextField"): [label] small above the typed value on the raised colour
 * with a hairline edge — the same box as [SelectField] — that turns `primary` and 2 dp while focused
 * and red with [isError]. [supportingText] (a hint or the error) sits below the box, red with
 * [isError]; [trailingIcon] (e.g. the password eye) at the end of the box.
 *
 * The label and the supporting text belong to the field's own focus stop, so a screen reader reads
 * them with it; an error is also reported as the field's error state, said once.
 */
@Composable
fun MeadowTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val accent = when {
        isError -> colors.error
        focused -> colors.primary
        else -> null
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
        cursorBrush = SolidColor(if (isError) colors.error else colors.primary),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (isError && supportingText != null) error(supportingText) },
        decorationBox = { innerTextField ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = HEIGHT)
                        .background(colors.surfaceContainerLowest, MaterialTheme.shapes.medium)
                        .border(
                            width = if (accent != null) 2.dp else 1.dp,
                            color = accent ?: colors.outlineVariant,
                            shape = MaterialTheme.shapes.medium,
                        )
                        .padding(start = 16.dp, end = if (trailingIcon != null) 2.dp else 14.dp)
                        .alpha(if (enabled) 1f else DISABLED_ALPHA),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = accent ?: colors.onSurfaceVariant)
                        innerTextField()
                    }
                    trailingIcon?.invoke()
                }
                supportingText?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isError) colors.error else colors.onSurfaceVariant,
                        // An error is already said as the field's error state; a hint is read as text.
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .then(if (isError) Modifier.clearAndSetSemantics {} else Modifier),
                    )
                }
            }
        },
    )
}

private val HEIGHT = 64.dp
private const val DISABLED_ALPHA = 0.38f
