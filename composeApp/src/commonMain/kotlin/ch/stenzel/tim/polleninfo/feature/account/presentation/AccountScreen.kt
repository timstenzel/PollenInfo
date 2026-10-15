package ch.stenzel.tim.polleninfo.feature.account.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.ui.error.ErrorContext
import ch.stenzel.tim.polleninfo.core.ui.error.message
import ch.stenzel.tim.polleninfo.core.ui.password.PasswordField
import ch.stenzel.tim.polleninfo.feature.account.domain.model.AccountMode
import ch.stenzel.tim.polleninfo.feature.account.domain.model.MAX_PASSWORD_LENGTH
import ch.stenzel.tim.polleninfo.feature.account.domain.model.MIN_PASSWORD_LENGTH
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.account_confirmation_label
import ch.stenzel.tim.polleninfo.resources.account_consent
import ch.stenzel.tim.polleninfo.resources.account_email_label
import ch.stenzel.tim.polleninfo.resources.account_hide_confirmation
import ch.stenzel.tim.polleninfo.resources.account_hide_password
import ch.stenzel.tim.polleninfo.resources.account_no_recovery
import ch.stenzel.tim.polleninfo.resources.account_password_hint
import ch.stenzel.tim.polleninfo.resources.account_password_label
import ch.stenzel.tim.polleninfo.resources.account_password_too_long
import ch.stenzel.tim.polleninfo.resources.account_passwords_mismatch
import ch.stenzel.tim.polleninfo.resources.account_show_confirmation
import ch.stenzel.tim.polleninfo.resources.account_show_password
import ch.stenzel.tim.polleninfo.resources.account_switch_to_login
import ch.stenzel.tim.polleninfo.resources.account_switch_to_register
import ch.stenzel.tim.polleninfo.resources.account_title_login
import ch.stenzel.tim.polleninfo.resources.account_title_register
import ch.stenzel.tim.polleninfo.resources.common_back
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Creating an account or logging in, opened from the Diary's gate in either mode. Not a tab, so the
 * bottom bar is hidden. Back — the arrow or the gesture — leaves without logging in; a success
 * leaves logged in ([onDone] for both).
 *
 * Logging in asks for the email and the password. Registration asks for the email, the password
 * twice (each field with its own show/hide button), the consent to storing the diary, and says
 * plainly that a forgotten password cannot be recovered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    initialMode: AccountMode,
    onDone: () -> Unit,
    viewModel: AccountViewModel = koinViewModel(parameters = { parametersOf(initialMode) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AccountEvent.Done -> onDone()
            }
        }
    }

    val mode = uiState.form.mode
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(mode.titleResource())) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.common_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            AccountForm(uiState, viewModel)
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    viewModel.onModeChanged(if (mode == AccountMode.LOGIN) AccountMode.REGISTER else AccountMode.LOGIN)
                },
                enabled = !uiState.isSubmitting,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    stringResource(
                        if (mode == AccountMode.LOGIN) Res.string.account_switch_to_register else Res.string.account_switch_to_login,
                    ),
                )
            }
        }
    }
}

@Composable
private fun AccountForm(uiState: AccountUiState, viewModel: AccountViewModel) {
    val form = uiState.form
    val enabled = !uiState.isSubmitting
    val registering = form.mode == AccountMode.REGISTER

    OutlinedTextField(
        value = form.email,
        onValueChange = viewModel::onEmailChanged,
        label = { Text(stringResource(Res.string.account_email_label)) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            autoCorrectEnabled = false,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    PasswordField(
        value = form.password,
        onValueChange = viewModel::onPasswordChanged,
        label = Res.string.account_password_label,
        visible = form.passwordVisible,
        onToggleVisible = viewModel::onPasswordVisibilityToggled,
        showLabel = Res.string.account_show_password,
        hideLabel = Res.string.account_hide_password,
        enabled = enabled,
        // The limits are the registration's; a login takes whatever password the account has.
        isError = registering && form.passwordTooLong,
        supportingText = when {
            !registering -> null
            form.passwordTooLong -> stringResource(Res.string.account_password_too_long, MAX_PASSWORD_LENGTH)
            else -> stringResource(Res.string.account_password_hint, MIN_PASSWORD_LENGTH)
        },
        imeAction = if (registering) ImeAction.Next else ImeAction.Done,
    )
    if (registering) RegisterFields(uiState, viewModel)
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = viewModel::submit,
        enabled = uiState.canSubmit,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (uiState.isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(stringResource(form.mode.titleResource()))
    }
    uiState.error?.let { error ->
        Spacer(Modifier.height(8.dp))
        Text(
            text = error.message(if (registering) ErrorContext.REGISTER else ErrorContext.LOGIN),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** What only registration asks for: the password again, the warning and the consent. */
@Composable
private fun RegisterFields(uiState: AccountUiState, viewModel: AccountViewModel) {
    val form = uiState.form
    val enabled = !uiState.isSubmitting
    Spacer(Modifier.height(8.dp))
    PasswordField(
        value = form.confirmation,
        onValueChange = viewModel::onConfirmationChanged,
        label = Res.string.account_confirmation_label,
        visible = form.confirmationVisible,
        onToggleVisible = viewModel::onConfirmationVisibilityToggled,
        showLabel = Res.string.account_show_confirmation,
        hideLabel = Res.string.account_hide_confirmation,
        enabled = enabled,
        isError = form.passwordsMismatch,
        supportingText = if (form.passwordsMismatch) stringResource(Res.string.account_passwords_mismatch) else null,
        imeAction = ImeAction.Done,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(Res.string.account_no_recovery),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    // One toggleable focus stop: the checkbox and its sentence are read and switched together.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = form.consent,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = viewModel::onConsentChanged,
            ),
    ) {
        Checkbox(checked = form.consent, onCheckedChange = null, enabled = enabled)
        Spacer(Modifier.width(12.dp))
        Text(text = stringResource(Res.string.account_consent), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun AccountMode.titleResource(): StringResource = when (this) {
    AccountMode.LOGIN -> Res.string.account_title_login
    AccountMode.REGISTER -> Res.string.account_title_register
}
