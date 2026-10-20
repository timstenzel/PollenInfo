package ch.stenzel.tim.polleninfo.core.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.common_back
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.stringResource

/**
 * The frame of a screen that is not a tab, which has no top bar either: the Meadow background with
 * its washes, a round glass back button floating top-left when [onBack] is given, a scrim under the
 * status bar and the back button, and — when [actionBar] is given — the screen's main action on a
 * glass bar at the bottom, styled like the tab bar.
 *
 * [content] fills the whole screen, behind the back button and the action bar, and is handed the
 * padding that keeps it clear of them: the safe top and sides plus the back button's row, and at the
 * bottom the action bar's measured height or, without one, the safe bottom (keyboard included). A
 * scrolling screen applies it inside its scroll; the first thing it shows is a [ScreenHeader].
 */
@Composable
fun ScreenFrame(
    onBack: (() -> Unit)?,
    actionBar: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable (contentPadding: PaddingValues) -> Unit,
) {
    val hazeState = rememberHazeState()
    val safe = WindowInsets.safeDrawing.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val statusBar = safe.calculateTopPadding()
    val backRow = if (onBack != null) BACK_BUTTON_TOP + BACK_BUTTON_SIZE else 0.dp

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Measures the action bar first, so the content knows how far its end has to clear it in
        // the very first frame.
        SubcomposeLayout(Modifier.fillMaxSize()) { constraints ->
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val bar = actionBar?.let { slot ->
                subcompose(ActionBarSlot) { ActionBar(hazeState, slot) }.map { it.measure(loose) }
            }.orEmpty()
            val bottom = if (actionBar != null) {
                (bar.maxOfOrNull { it.height } ?: 0).toDp()
            } else {
                safe.calculateBottomPadding()
            }
            val contentPadding = PaddingValues(
                start = safe.calculateStartPadding(layoutDirection),
                top = statusBar + backRow,
                end = safe.calculateEndPadding(layoutDirection),
                bottom = bottom,
            )
            val body = subcompose(ContentSlot) {
                // The washes are part of the blur source, so the glass shows them softly as well.
                Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
                    Washes(Modifier.fillMaxSize())
                    content(contentPadding)
                }
            }.map { it.measure(constraints) }

            layout(constraints.maxWidth, constraints.maxHeight) {
                body.forEach { it.place(0, 0) }
                bar.forEach { it.place(0, constraints.maxHeight - it.height) }
            }
        }
        TopScrim(opaqueHeight = statusBar, fade = backRow + SCRIM_FADE)
        if (onBack != null) {
            BackButton(
                onClick = onBack,
                hazeState = hazeState,
                modifier = Modifier.padding(
                    start = safe.calculateStartPadding(layoutDirection) + 20.dp,
                    top = statusBar + BACK_BUTTON_TOP,
                ),
            )
        }
    }
}

private object ActionBarSlot
private object ContentSlot

/** The action bar: full-width buttons (and anything said about them) stacked on the glass. */
@Composable
private fun ActionBar(hazeState: HazeState, content: @Composable ColumnScope.() -> Unit) {
    GlassBar(hazeState = hazeState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 16.dp),
            content = content,
        )
    }
}

/**
 * A 44 dp glass circle with the back arrow, announced as "Back" (as the top bar's arrow was) and
 * touchable on 48 dp.
 */
@Composable
private fun BackButton(onClick: () -> Unit, hazeState: HazeState, modifier: Modifier) {
    Box(modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(BACK_BUTTON_SIZE)
                .glass(hazeState, CircleShape)
                .glassEdge(CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.common_back),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private val BACK_BUTTON_TOP = 6.dp
private val BACK_BUTTON_SIZE = 44.dp

/** How far below the back button (or the status bar) the top scrim fades out. */
private val SCRIM_FADE = 16.dp
