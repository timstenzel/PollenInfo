package ch.stenzel.tim.polleninfo.core.ui.layout

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How much of the bottom of the screen the glass tab bar covers, its navigation-bar inset included;
 * zero where no bar is shown.
 *
 * Tab screens draw behind the bar, so the content can be seen blurred through it. Everything that
 * must stay reachable at the bottom of a tab — a list's end, Home's feeling card, the Alarms create
 * button, a snackbar — adds this to its bottom padding. `AppNavigation` provides it.
 */
val LocalBottomBarPadding = compositionLocalOf<Dp> { 0.dp }
