package ch.stenzel.tim.polleninfo.feature.account.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.ui.button.PillButton
import ch.stenzel.tim.polleninfo.core.ui.button.PillButtonStyle
import ch.stenzel.tim.polleninfo.core.ui.error.ErrorContext
import ch.stenzel.tim.polleninfo.core.ui.error.message
import ch.stenzel.tim.polleninfo.core.ui.field.MeadowTextField
import ch.stenzel.tim.polleninfo.core.ui.layout.ScreenFrame
import ch.stenzel.tim.polleninfo.core.ui.layout.ScreenHeader
import ch.stenzel.tim.polleninfo.core.ui.password.PasswordField
import ch.stenzel.tim.polleninfo.core.ui.selection.MeadowCheckbox
import ch.stenzel.tim.polleninfo.core.ui.status.Banner
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
import ch.stenzel.tim.polleninfo.resources.settings_section_account
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Creating an account or logging in, opened from the Diary's gate in either mode. Not a tab, so the
 * bottom bar is hidden. Back — the back button or the gesture — leaves without logging in; a success
 * leaves logged in ([onDone] for both).
 *
 * Logging in asks for the email and the password. Registration asks for the email, the password
 * twice (each field with its own show/hide button), the consent to storing the diary, and says
 * plainly that a forgotten password cannot be recovered.
 */
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
    ScreenFrame(onBack = onDone) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            if (mode == AccountMode.LOGIN) KeyBadge()
            // The overline names the place, the title what is done here.
            ScreenHeader(
                overline = stringResource(Res.string.settings_section_account),
                title = stringResource(mode.titleResource()),
            )
            AccountForm(uiState, viewModel)
            Spacer(Modifier.height(8.dp))
            PillButton(
                onClick = {
                    viewModel.onModeChanged(if (mode == AccountMode.LOGIN) AccountMode.REGISTER else AccountMode.LOGIN)
                },
                style = PillButtonStyle.Text,
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

/** The log-in form's emblem above its title, as in Figma: a key on a sage tile. Decorative. */
@Composable
private fun KeyBadge() {
    Box(
        modifier = Modifier
            .padding(top = 12.dp)
            .size(64.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.large),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Key,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(32.dp),
        )
    }
}

/**
 * The fields, then — once an attempt failed — its error as a banner under them, then the button.
 */
@Composable
private fun AccountForm(uiState: AccountUiState, viewModel: AccountViewModel) {
    val form = uiState.form
    val enabled = !uiState.isSubmitting
    val registering = form.mode == AccountMode.REGISTER

    Column(verticalArrangement = Arrangement.spacedBy(FIELD_GAP)) {
        MeadowTextField(
            value = form.email,
            onValueChange = viewModel::onEmailChanged,
            label = stringResource(Res.string.account_email_label),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                autoCorrectEnabled = false,
            ),
            // The login's user name is the email: password managers save and fill it as both.
            modifier = Modifier.semantics { contentType = ContentType.EmailAddress + ContentType.Username },
        )
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
            contentType = if (registering) ContentType.NewPassword else ContentType.Password,
        )
        if (registering) RegisterFields(uiState, viewModel)
        uiState.error?.let { error ->
            Banner(
                text = error.message(if (registering) ErrorContext.REGISTER else ErrorContext.LOGIN),
                icon = Icons.Default.ErrorOutline,
                iconTint = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
    Spacer(Modifier.height(24.dp))
    PillButton(
        onClick = viewModel::submit,
        style = PillButtonStyle.Primary,
        enabled = uiState.canSubmit,
        busy = uiState.isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(form.mode.titleResource()))
    }
}

/** What only registration asks for: the password again, the warning and the consent. */
@Composable
private fun RegisterFields(uiState: AccountUiState, viewModel: AccountViewModel) {
    val form = uiState.form
    val enabled = !uiState.isSubmitting
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
        contentType = ContentType.NewPassword,
    )
    // A caution rather than a problem, so without the warning sign.
    Banner(text = stringResource(Res.string.account_no_recovery), icon = null, modifier = Modifier.fillMaxWidth())
    // One toggleable focus stop: the checkbox and its sentence are read and switched together.
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .toggleable(
                value = form.consent,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = viewModel::onConsentChanged,
            )
            .heightIn(min = 48.dp)
            .padding(14.dp),
    ) {
        MeadowCheckbox(checked = form.consent, enabled = enabled)
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(Res.string.account_consent),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun AccountMode.titleResource(): StringResource = when (this) {
    AccountMode.LOGIN -> Res.string.account_title_login
    AccountMode.REGISTER -> Res.string.account_title_register
}

/** Between the form's fields, banners and the consent card, as in Figma. */
private val FIELD_GAP = 14.dp
