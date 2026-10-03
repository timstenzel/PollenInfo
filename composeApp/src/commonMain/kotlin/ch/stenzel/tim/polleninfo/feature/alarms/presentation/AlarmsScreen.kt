package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.notifications.NotificationPermissionState
import ch.stenzel.tim.polleninfo.core.notifications.rememberNotificationPermissionController
import kotlinx.coroutines.flow.filter
import org.koin.compose.viewmodel.koinViewModel

/**
 * The user's pollen alarms, behind the notification permission.
 *
 * The permission is re-read every time the screen resumes, which is what makes returning from the
 * system settings update the screen on its own, and what brings the explanation back after the user
 * revokes the permission elsewhere.
 */
@Composable
fun AlarmsScreen(viewModel: AlarmsViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val askedBefore by viewModel.askedBefore.collectAsStateWithLifecycle()
    val controller = rememberNotificationPermissionController()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // Bumped when the prompt is answered. The answer usually arrives with a resume anyway, but iOS
    // shows its prompt as an alert without leaving the screen, so the re-read cannot rely on that.
    var promptAnswers by remember { mutableIntStateOf(0) }

    LaunchedEffect(lifecycle, controller, askedBefore, promptAnswers) {
        // Not classified against a guess: an unread flag would show "Allow notifications" for a
        // moment to someone who has to go to settings.
        val asked = askedBefore ?: return@LaunchedEffect
        lifecycle.currentStateFlow
            .filter { it == Lifecycle.State.RESUMED }
            .collect { viewModel.onPermissionState(controller.currentStatus(asked)) }
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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmsContent(
    uiState: AlarmsUiState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Alarms") }) },
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)
        when (uiState) {
            // Reading the permission is near-instant; a spinner would only flash.
            AlarmsUiState.Loading -> Unit

            is AlarmsUiState.PermissionRequired -> PermissionRequiredView(
                state = uiState.state,
                onRequestPermission = onRequestPermission,
                onOpenSettings = onOpenSettings,
                modifier = modifier,
            )

            AlarmsUiState.Content -> EmptyAlarmsView(modifier)
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
        title = "Notifications are disabled",
        body = if (mustOpenSettings) {
            "Pollen alarms arrive as notifications. Turn on notifications for PollenInfo in the " +
                "system settings to set alarms."
        } else {
            "Pollen alarms arrive as notifications. Allow PollenInfo to send notifications to set alarms."
        },
        modifier = modifier,
    ) {
        if (mustOpenSettings) {
            Button(onClick = onOpenSettings) { Text("Open settings") }
        } else {
            Button(onClick = onRequestPermission) { Text("Allow notifications") }
        }
    }
}

@Composable
private fun EmptyAlarmsView(modifier: Modifier) {
    CenteredMessage(
        icon = Icons.Default.Notifications,
        title = "No alarms yet",
        body = "Create an alarm to get a daily pollen report, or a warning when a pollen type " +
            "reaches a level you choose.",
        modifier = modifier,
    ) {
        // Creating alarms needs the backend's alarm store, which does not exist yet.
        Button(onClick = {}, enabled = false) { Text("Create alarm") }
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
