package ch.stenzel.tim.polleninfo.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.ui.layout.GlassBar
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.resources.stringResource

/**
 * The tab bar: icon-only [TopLevelDestination]s on a [GlassBar], the current tab on a green pill.
 * The tab content behind it is [hazeState]'s source.
 *
 * Each tab is a `Role.Tab` selectable announced by its `contentDescription`, as with Material's
 * `NavigationBarItem` before.
 */
@Composable
fun GlassNavigationBar(
    currentTab: TopLevelDestination,
    onTabSelected: (TopLevelDestination) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    GlassBar(hazeState = hazeState, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopLevelDestination.entries.forEach { tab ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    TabItem(
                        tab = tab,
                        selected = tab == currentTab,
                        onClick = { onTabSelected(tab) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabItem(tab: TopLevelDestination, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val pill by animateColorAsState(if (selected) colors.primary else Color.Transparent)
    val tint by animateColorAsState(if (selected) colors.onPrimary else colors.onSurfaceVariant)
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(width = 60.dp, height = 44.dp)
            .clip(CircleShape)
            .background(pill)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
        contentAlignment = Alignment.Center,
    ) {
        Icon(tab.icon, contentDescription = stringResource(tab.contentDescription), tint = tint)
    }
}
