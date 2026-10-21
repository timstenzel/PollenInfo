package ch.stenzel.tim.polleninfo.core.ui.password

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.ui.field.MeadowTextField
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.account_password_hidden
import ch.stenzel.tim.polleninfo.resources.account_password_shown
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A password field with its own show/hide button. The button is named for what it does ("Show
 * password") and states what the field does now ("Password hidden"), so a screen reader user knows
 * whether the field's text will be read out. A [MeadowTextField] with the eye at its end; shared by
 * the account screen and Settings' account deletion.
 *
 * [contentType] tells password managers what the field holds: [ContentType.Password] to fill in a
 * saved password, [ContentType.NewPassword] where one is chosen, which is where they suggest a
 * strong one.
 */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: StringResource,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    showLabel: StringResource,
    hideLabel: StringResource,
    enabled: Boolean,
    isError: Boolean = false,
    supportingText: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    contentType: ContentType = ContentType.Password,
) {
    val state = stringResource(if (visible) Res.string.account_password_shown else Res.string.account_password_hidden)
    MeadowTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(label),
        enabled = enabled,
        isError = isError,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
            autoCorrectEnabled = false,
        ),
        trailingIcon = {
            IconButton(onClick = onToggleVisible, modifier = Modifier.semantics { stateDescription = state }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = stringResource(if (visible) hideLabel else showLabel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        supportingText = supportingText,
        modifier = Modifier.semantics { this.contentType = contentType },
    )
}
