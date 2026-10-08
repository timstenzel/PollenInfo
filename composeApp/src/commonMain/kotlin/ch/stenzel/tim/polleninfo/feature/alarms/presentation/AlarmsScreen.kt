package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.notifications.NotificationPermissionState
import ch.stenzel.tim.polleninfo.core.notifications.rememberNotificationPermissionController
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.ui.error.ErrorContext
import ch.stenzel.tim.polleninfo.core.ui.error.loadMessage
import ch.stenzel.tim.polleninfo.core.ui.error.message
import ch.stenzel.tim.polleninfo.core.ui.format.rememberDateWording
import ch.stenzel.tim.polleninfo.core.ui.severity.label
import ch.stenzel.tim.polleninfo.core.ui.species.speciesName
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.MAX_ALARMS
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.alarm_create
import ch.stenzel.tim.polleninfo.resources.alarm_edit_action
import ch.stenzel.tim.polleninfo.resources.alarm_empty_body
import ch.stenzel.tim.polleninfo.resources.alarm_empty_title
import ch.stenzel.tim.polleninfo.resources.alarm_limit_hint
import ch.stenzel.tim.polleninfo.resources.alarm_load_failed
import ch.stenzel.tim.polleninfo.resources.alarm_permission_allow
import ch.stenzel.tim.polleninfo.resources.alarm_permission_body_request
import ch.stenzel.tim.polleninfo.resources.alarm_permission_body_settings
import ch.stenzel.tim.polleninfo.resources.alarm_permission_open_settings
import ch.stenzel.tim.polleninfo.resources.alarm_permission_title
import ch.stenzel.tim.polleninfo.resources.alarm_push_unavailable_body
import ch.stenzel.tim.polleninfo.resources.alarm_push_unavailable_title
import ch.stenzel.tim.polleninfo.resources.alarm_switch_description
import ch.stenzel.tim.polleninfo.resources.common_retry
import ch.stenzel.tim.polleninfo.resources.nav_alarms
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The user's pollen alarms, behind the notification permission.
 *
 * The permission is re-read every time the screen resumes, which is what makes returning from the
 * system settings update the screen on its own, and what brings the explanation back after the user
 * revokes the permission elsewhere.
 */
@Composable
fun AlarmsScreen(
    onCreateAlarm: () -> Unit,
    onEditAlarm: (alarmId: String) -> Unit,
    viewModel: AlarmsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val askedBefore by viewModel.askedBefore.collectAsStateWithLifecycle()
    val controller = rememberNotificationPermissionController()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // Bumped when the prompt is answered. The answer usually arrives with a resume anyway, but iOS
    // shows its prompt as an alert without leaving the screen, so the re-read cannot rely on that.
    var promptAnswers by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AlarmsEvent.ToggleFailed -> {
                    val context = if (event.enabling) ErrorContext.RESUME_ALARM else ErrorContext.PAUSE_ALARM
                    snackbarHostState.showSnackbar(event.error.loadMessage(context))
                }
            }
        }
    }

    LaunchedEffect(lifecycle, controller, askedBefore, promptAnswers) {
        // Not classified against a guess: an unread flag would show "Allow notifications" for a
        // moment to someone who has to go to settings.
        val asked = askedBefore ?: return@LaunchedEffect
        lifecycle.currentStateFlow
            .filter { it == Lifecycle.State.RESUMED }
            .collect {
                viewModel.onPermissionState(controller.currentStatus(asked))
                // Back from the editor among others: a list already shown is reloaded quietly.
                viewModel.onResume()
            }
    }

    AlarmsContent(
        uiState = uiState,
        onRequestPermission = {
            controller.request {
                viewModel.onPermissionRequested()
                promptAnswers++
            }
        },
        onOpenSettings = controller::openSettings,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onCreateAlarm = onCreateAlarm,
        onEditAlarm = onEditAlarm,
        onEnabledToggled = viewModel::onEnabledToggled,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmsContent(
    uiState: AlarmsUiState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onCreateAlarm: () -> Unit,
    onEditAlarm: (alarmId: String) -> Unit,
    onEnabledToggled: (alarmId: String, enabled: Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.nav_alarms)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // The empty state has its own, more prominent button.
            if (uiState is AlarmsUiState.Content && uiState.alarms.isNotEmpty()) {
                CreateAlarmButton(enabled = !uiState.limitReached, onClick = onCreateAlarm)
            }
        },
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        val messageModifier = modifier.padding(24.dp)
        when (uiState) {
            // Reading the permission is near-instant; a spinner would only flash.
            AlarmsUiState.CheckingPermission -> Unit

            is AlarmsUiState.PermissionRequired -> PermissionRequiredView(
                state = uiState.state,
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenSettings,
                modifier = messageModifier,
            )

            AlarmsUiState.Loading -> Box(messageModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            // The alarms stay composed while a refresh runs; only the indicator is added.
            is AlarmsUiState.Content -> PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = modifier,
            ) {
                if (uiState.alarms.isEmpty()) {
                    // Scrollable so the pull gesture has something to drag.
                    EmptyAlarmsView(
                        onCreateAlarm = onCreateAlarm,
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    )
                } else {
                    AlarmList(
                        alarms = uiState.alarms,
                        limitReached = uiState.limitReached,
                        onEditAlarm = onEditAlarm,
                        onEnabledToggled = onEnabledToggled,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            is AlarmsUiState.Error -> if (uiState.error == AppError.PushUnavailable) {
                PushUnavailableView(messageModifier)
            } else {
                ErrorView(uiState.error.message(), onRetry, messageModifier)
            }
        }
    }
}

@Composable
private fun PermissionRequiredView(
    state: NotificationPermissionState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier,
) {
    val mustOpenSettings = state == NotificationPermissionState.MUST_OPEN_SETTINGS
    CenteredMessage(
        icon = Icons.Default.NotificationsOff,
        title = stringResource(Res.string.alarm_permission_title),
        body = stringResource(
            if (mustOpenSettings) Res.string.alarm_permission_body_settings else Res.string.alarm_permission_body_request,
        ),
        modifier = modifier,
    ) {
        if (mustOpenSettings) {
            Button(onClick = onOpenSettings) { Text(stringResource(Res.string.alarm_permission_open_settings)) }
        } else {
            Button(onClick = onRequestPermission) { Text(stringResource(Res.string.alarm_permission_allow)) }
        }
    }
}

@Composable
private fun EmptyAlarmsView(onCreateAlarm: () -> Unit, modifier: Modifier) {
    CenteredMessage(
        icon = Icons.Default.Notifications,
        title = stringResource(Res.string.alarm_empty_title),
        body = stringResource(Res.string.alarm_empty_body),
        modifier = modifier,
    ) {
        Button(onClick = onCreateAlarm) { Text(stringResource(Res.string.alarm_create)) }
    }
}

/**
 * Material's extended FAB has no disabled state, so at the limit it takes the disabled colours of the
 * Material 3 spec, ignores clicks and is marked disabled for screen readers.
 */
@Composable
private fun CreateAlarmButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    ExtendedFloatingActionButton(
        onClick = { if (enabled) onClick() },
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text(stringResource(Res.string.alarm_create)) },
        // Composited, because a translucent button floating over the list would show the rows through it.
        containerColor = if (enabled) {
            colors.primaryContainer
        } else {
            colors.onSurface.copy(alpha = 0.12f).compositeOver(colors.surface)
        },
        contentColor = if (enabled) colors.onPrimaryContainer else colors.onSurface.copy(alpha = 0.38f),
        elevation = if (enabled) {
            FloatingActionButtonDefaults.elevation()
        } else {
            FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
        },
        modifier = if (enabled) Modifier else Modifier.semantics { disabled() },
    )
}

/**
 * Tapping a row opens it in the editor; its switch pauses or resumes it. The switch is a focus stop
 * of its own, named after the row's station so a screen reader says what it switches.
 */
@Composable
private fun AlarmList(
    alarms: List<AlarmListItem>,
    limitReached: Boolean,
    onEditAlarm: (alarmId: String) -> Unit,
    onEnabledToggled: (alarmId: String, enabled: Boolean) -> Unit,
    modifier: Modifier,
) {
    Column(modifier) {
        // Fixed above the list rather than its first item: a list reloaded into the limit keeps its
        // scroll anchored on the first alarm, which would leave a new first item scrolled out of view.
        if (limitReached) {
            Text(
                text = pluralStringResource(Res.plurals.alarm_limit_hint, MAX_ALARMS, MAX_ALARMS),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            HorizontalDivider()
        }
        AlarmRows(alarms, onEditAlarm, onEnabledToggled, Modifier.weight(1f))
    }
}

@Composable
private fun AlarmRows(
    alarms: List<AlarmListItem>,
    onEditAlarm: (alarmId: String) -> Unit,
    onEnabledToggled: (alarmId: String, enabled: Boolean) -> Unit,
    modifier: Modifier,
) {
    val dates = rememberDateWording()
    val severityLabels = PollenSeverity.entries.associateWith { it.label() }
    val wording = rememberAlarmSummaryWording()
    // Room below the last row, so the create button never covers it.
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 88.dp)) {
        items(alarms, key = { it.alarm.id }) { item ->
            val enabled = item.alarm.enabled
            val speciesNames = item.speciesNames.mapValues { (id, name) -> speciesName(id, name) }
            val summary = summaryOf(item.alarm, speciesNames, severityLabels, dates, wording)
            val switchDescription = stringResource(Res.string.alarm_switch_description, item.stationName)
            ListItem(
                headlineContent = { Text(item.stationName) },
                supportingContent = { Text(summary) },
                trailingContent = {
                    Switch(
                        checked = enabled,
                        onCheckedChange = { onEnabledToggled(item.alarm.id, it) },
                        modifier = Modifier.semantics { contentDescription = switchDescription },
                    )
                },
                modifier = Modifier.clickable(onClickLabel = stringResource(Res.string.alarm_edit_action)) { onEditAlarm(item.alarm.id) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun PushUnavailableView(modifier: Modifier) {
    CenteredMessage(
        icon = Icons.Default.NotificationsOff,
        title = stringResource(Res.string.alarm_push_unavailable_title),
        body = stringResource(Res.string.alarm_push_unavailable_body),
        modifier = modifier,
    ) {}
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier) {
    CenteredMessage(
        icon = Icons.Default.ErrorOutline,
        title = stringResource(Res.string.alarm_load_failed),
        body = message,
        modifier = modifier,
    ) {
        Button(onClick = onRetry) { Text(stringResource(Res.string.common_retry)) }
    }
}

@Composable
private fun CenteredMessage(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier,
    action: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            // Decorative: the title beside it says the same.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action()
    }
}
