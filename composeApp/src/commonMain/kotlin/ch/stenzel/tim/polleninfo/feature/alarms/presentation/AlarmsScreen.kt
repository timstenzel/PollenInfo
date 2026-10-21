package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.core.notifications.NotificationPermissionState
import ch.stenzel.tim.polleninfo.core.notifications.rememberNotificationPermissionController
import ch.stenzel.tim.polleninfo.core.result.AppError
import ch.stenzel.tim.polleninfo.core.ui.button.PillButton
import ch.stenzel.tim.polleninfo.core.ui.button.PillButtonStyle
import ch.stenzel.tim.polleninfo.core.ui.error.ErrorContext
import ch.stenzel.tim.polleninfo.core.ui.error.loadMessage
import ch.stenzel.tim.polleninfo.core.ui.error.message
import ch.stenzel.tim.polleninfo.core.ui.format.rememberDateWording
import ch.stenzel.tim.polleninfo.core.ui.layout.LocalBottomBarPadding
import ch.stenzel.tim.polleninfo.core.ui.layout.ScreenHeader
import ch.stenzel.tim.polleninfo.core.ui.layout.TabPullToRefreshBox
import ch.stenzel.tim.polleninfo.core.ui.layout.TabScreenFrame
import ch.stenzel.tim.polleninfo.core.ui.severity.label
import ch.stenzel.tim.polleninfo.core.ui.species.speciesName
import ch.stenzel.tim.polleninfo.core.ui.status.StatusCard
import ch.stenzel.tim.polleninfo.core.ui.status.StatusTone
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.MAX_ALARMS
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.alarm_count
import ch.stenzel.tim.polleninfo.resources.alarm_create
import ch.stenzel.tim.polleninfo.resources.alarm_empty_body
import ch.stenzel.tim.polleninfo.resources.alarm_empty_title
import ch.stenzel.tim.polleninfo.resources.alarm_limit_hint
import ch.stenzel.tim.polleninfo.resources.alarm_load_failed
import ch.stenzel.tim.polleninfo.resources.alarm_overline
import ch.stenzel.tim.polleninfo.resources.alarm_permission_allow
import ch.stenzel.tim.polleninfo.resources.alarm_permission_body_request
import ch.stenzel.tim.polleninfo.resources.alarm_permission_body_settings
import ch.stenzel.tim.polleninfo.resources.alarm_permission_open_settings
import ch.stenzel.tim.polleninfo.resources.alarm_permission_title
import ch.stenzel.tim.polleninfo.resources.alarm_push_unavailable_body
import ch.stenzel.tim.polleninfo.resources.alarm_push_unavailable_title
import ch.stenzel.tim.polleninfo.resources.alarm_tag_any
import ch.stenzel.tim.polleninfo.resources.alarm_tag_minimum
import ch.stenzel.tim.polleninfo.resources.common_retry
import ch.stenzel.tim.polleninfo.resources.nav_alarms
import ch.stenzel.tim.polleninfo.theme.meadowColors
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


/**
 * Every state starts with the header; the create button and the snackbar float above the glass tab
 * bar, the button above the snackbar while one shows.
 */
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
    TabScreenFrame { contentPadding ->
        val alarms = (uiState as? AlarmsUiState.Content)?.alarms.orEmpty()
        val header = @Composable { AlarmsHeader(count = alarms.size.takeIf { it > 0 }) }
        Box(Modifier.fillMaxSize()) {
            when (uiState) {
                // Reading the permission is near-instant; a skeleton would only flash.
                AlarmsUiState.CheckingPermission -> AlarmsColumn(contentPadding, header) {}

                is AlarmsUiState.PermissionRequired -> AlarmsColumn(contentPadding, header) {
                    PermissionCard(uiState.state, onRequestPermission, onOpenSettings)
                }

                AlarmsUiState.Loading -> AlarmsColumn(contentPadding, header) { AlarmsSkeleton() }

                is AlarmsUiState.Content -> AlarmsContentView(
                    content = uiState,
                    contentPadding = contentPadding,
                    header = header,
                    onRefresh = onRefresh,
                    onCreateAlarm = onCreateAlarm,
                    onEditAlarm = onEditAlarm,
                    onEnabledToggled = onEnabledToggled,
                )

                is AlarmsUiState.Error -> AlarmsColumn(contentPadding, header) {
                    if (uiState.error == AppError.PushUnavailable) {
                        StatusCard(
                            title = stringResource(Res.string.alarm_push_unavailable_title),
                            body = stringResource(Res.string.alarm_push_unavailable_body),
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.NotificationsOff,
                        )
                    } else {
                        StatusCard(
                            title = stringResource(Res.string.alarm_load_failed),
                            body = uiState.error.message(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            PillButton(onClick = onRetry, style = PillButtonStyle.Primary, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(Res.string.common_retry))
                            }
                        }
                    }
                }
            }
            FloatingControls(
                // The empty state has its own, more prominent button.
                showCreate = uiState is AlarmsUiState.Content && alarms.isNotEmpty(),
                createEnabled = !(uiState is AlarmsUiState.Content && uiState.limitReached),
                onCreateAlarm = onCreateAlarm,
                snackbarHostState = snackbarHostState,
                contentPadding = contentPadding,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** The overline and title, with the "3 of 10" count beside the title once there are alarms. */
@Composable
private fun AlarmsHeader(count: Int?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        ScreenHeader(
            overline = stringResource(Res.string.alarm_overline),
            title = stringResource(Res.string.nav_alarms),
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Text(
                text = stringResource(Res.string.alarm_count, count, MAX_ALARMS),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    // Centred on the title's line, above the header's own bottom padding.
                    .padding(bottom = 28.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

/** A state without a list, scrolling as a whole, clear of the status bar and the tab bar. */
@Composable
private fun AlarmsColumn(
    contentPadding: PaddingValues,
    header: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(start = SIDE_MARGIN, end = SIDE_MARGIN, bottom = 24.dp),
    ) {
        header()
        content()
    }
}

/**
 * The header stays above the list with the limit hint (see [AlarmList]), so only the alarms scroll
 * — on behind the glass tab bar, with their end padded clear of it and of the create button.
 */
@Composable
private fun AlarmsContentView(
    content: AlarmsUiState.Content,
    contentPadding: PaddingValues,
    header: @Composable () -> Unit,
    onRefresh: () -> Unit,
    onCreateAlarm: () -> Unit,
    onEditAlarm: (alarmId: String) -> Unit,
    onEnabledToggled: (alarmId: String, enabled: Boolean) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val bottomBar = contentPadding.calculateBottomPadding()
    Column(
        Modifier
            .fillMaxSize()
            .padding(
                start = contentPadding.calculateStartPadding(layoutDirection),
                top = contentPadding.calculateTopPadding(),
                end = contentPadding.calculateEndPadding(layoutDirection),
            ),
    ) {
        Box(Modifier.padding(horizontal = SIDE_MARGIN)) { header() }
        // The alarms stay composed while a refresh runs; only the indicator is added.
        TabPullToRefreshBox(
            isRefreshing = content.isRefreshing,
            onRefresh = onRefresh,
            topInset = 0.dp,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            if (content.alarms.isEmpty()) {
                // Scrollable so the pull gesture has something to drag.
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = SIDE_MARGIN, end = SIDE_MARGIN, bottom = bottomBar + 24.dp),
                ) {
                    StatusCard(
                        title = stringResource(Res.string.alarm_empty_title),
                        body = stringResource(Res.string.alarm_empty_body),
                        modifier = Modifier.fillMaxWidth(),
                        tone = StatusTone.Neutral,
                        icon = Icons.Default.Notifications,
                    ) {
                        PillButton(onClick = onCreateAlarm, style = PillButtonStyle.Accent, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(Res.string.alarm_create))
                        }
                    }
                }
            } else {
                AlarmList(
                    alarms = content.alarms,
                    limitReached = content.limitReached,
                    bottomPadding = bottomBar,
                    onEditAlarm = onEditAlarm,
                    onEnabledToggled = onEnabledToggled,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** Why the alarms are hidden, and the one button that lets them in: the prompt or the settings. */
@Composable
private fun PermissionCard(
    state: NotificationPermissionState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val mustOpenSettings = state == NotificationPermissionState.MUST_OPEN_SETTINGS
    StatusCard(
        title = stringResource(Res.string.alarm_permission_title),
        body = stringResource(
            if (mustOpenSettings) Res.string.alarm_permission_body_settings else Res.string.alarm_permission_body_request,
        ),
        modifier = Modifier.fillMaxWidth(),
        icon = Icons.Default.NotificationsOff,
        grains = true,
    ) {
        PillButton(
            onClick = if (mustOpenSettings) onOpenSettings else onRequestPermission,
            style = PillButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (mustOpenSettings) Res.string.alarm_permission_open_settings else Res.string.alarm_permission_allow,
                ),
            )
        }
    }
}

/**
 * What floats at the bottom end, above the glass tab bar: the gold create button and, below it
 * while one shows, the snackbar — so a snackbar pushes the button up rather than covering it.
 */
@Composable
private fun FloatingControls(
    showCreate: Boolean,
    createEnabled: Boolean,
    onCreateAlarm: () -> Unit,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    modifier: Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = contentPadding.calculateStartPadding(layoutDirection),
                end = contentPadding.calculateEndPadding(layoutDirection),
                bottom = contentPadding.calculateBottomPadding() + FLOATING_GAP,
            ),
        horizontalAlignment = Alignment.End,
    ) {
        if (showCreate) {
            Box(Modifier.padding(end = SIDE_MARGIN)) { CreateAlarmButton(enabled = createEnabled, onClick = onCreateAlarm) }
        }
        // Material's snackbar keeps a margin of its own around it, which also parts it from the button.
        SnackbarHost(snackbarHostState, Modifier.fillMaxWidth().padding(horizontal = SIDE_MARGIN - SNACKBAR_MARGIN)) { data ->
            Snackbar(
                snackbarData = data,
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}

/**
 * The gold extended button. Material's extended FAB has no disabled state, so at the limit it takes
 * the disabled colours of the Material 3 spec, ignores clicks and is marked disabled for screen
 * readers.
 */
@Composable
private fun CreateAlarmButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val label = stringResource(Res.string.alarm_create)
    ExtendedFloatingActionButton(
        onClick = { if (enabled) onClick() },
        // Material clears the label's semantics and names the button by its icon, so the icon
        // carries the label; without it a screen reader announces an unnamed button.
        icon = { Icon(Icons.Default.Add, contentDescription = label, modifier = Modifier.size(22.dp)) },
        text = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = CircleShape,
        // Composited, because a translucent button floating over the list would show the cards through it.
        containerColor = if (enabled) {
            MaterialTheme.meadowColors.accent
        } else {
            colors.onSurface.copy(alpha = 0.12f).compositeOver(colors.surface)
        },
        contentColor = if (enabled) MaterialTheme.meadowColors.onAccent else colors.onSurface.copy(alpha = 0.38f),
        elevation = if (enabled) {
            FloatingActionButtonDefaults.elevation(2.dp, 2.dp, 2.dp, 4.dp)
        } else {
            FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
        },
        modifier = if (enabled) Modifier else Modifier.semantics { disabled() },
    )
}

/** The cards, with the limit hint fixed above them while the device holds [MAX_ALARMS]. */
@Composable
private fun AlarmList(
    alarms: List<AlarmListItem>,
    limitReached: Boolean,
    bottomPadding: Dp,
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
                modifier = Modifier.padding(start = SIDE_MARGIN, end = SIDE_MARGIN, bottom = 12.dp),
            )
        }
        AlarmCards(alarms, bottomPadding, onEditAlarm, onEnabledToggled, Modifier.weight(1f))
    }
}

@Composable
private fun AlarmCards(
    alarms: List<AlarmListItem>,
    bottomPadding: Dp,
    onEditAlarm: (alarmId: String) -> Unit,
    onEnabledToggled: (alarmId: String, enabled: Boolean) -> Unit,
    modifier: Modifier,
) {
    val dates = rememberDateWording()
    val severityLabels = PollenSeverity.entries.associateWith { it.label() }
    val wording = rememberAlarmSummaryWording()
    val any = stringResource(Res.string.alarm_tag_any)
    LazyColumn(
        modifier = modifier,
        // Room below the last card, so neither the create button nor the glass tab bar covers it;
        // a little above the first, so the list's edge does not cut its shadow.
        contentPadding = PaddingValues(start = SIDE_MARGIN, end = SIDE_MARGIN, top = 4.dp, bottom = 96.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(alarms, key = { it.alarm.id }) { item ->
            val alarm = item.alarm
            val speciesNames = item.speciesNames.mapValues { (id, name) -> speciesName(id, name) }
            val minimum = if (alarm.minSeverity == PollenSeverity.NONE) {
                any
            } else {
                stringResource(Res.string.alarm_tag_minimum, severityLabels.getValue(alarm.minSeverity))
            }
            AlarmCard(
                item = item,
                text = AlarmCardText(
                    type = typeSummary(alarm, wording),
                    days = daysSummary(alarm.days, dates, wording),
                    species = speciesSummary(alarm.species, speciesNames, wording),
                    minimum = minimum,
                ),
                dates = dates,
                onEdit = { onEditAlarm(alarm.id) },
                onEnabledToggled = { onEnabledToggled(alarm.id, it) },
            )
        }
    }
}

private val SIDE_MARGIN = 20.dp

/** Between the tab bar and what floats above it. */
private val FLOATING_GAP = 16.dp

/** The margin Material's `Snackbar` adds around itself. */
private val SNACKBAR_MARGIN = 12.dp
