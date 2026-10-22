package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.ui.status.ShimmerBox
import ch.stenzel.tim.polleninfo.core.ui.status.SkeletonBlock

/**
 * The alarm editor while it loads (Figma "Alarm editor · Loading"): the two type cards, the
 * description line, the station field, two rows of chips twice and the weekday circles, as grey
 * blocks under one shimmer.
 */
@Composable
internal fun AlarmEditorSkeleton(modifier: Modifier = Modifier) {
    ShimmerBox(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    SkeletonBlock(0.dp, 104.dp, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large)
                }
            }
            SkeletonBlock(0.dp, 14.dp, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(7.dp))
            Section(90.dp) {
                SkeletonBlock(0.dp, 60.dp, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
            }
            repeat(2) { index ->
                Section(if (index == 0) 120.dp else 130.dp) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CHIP_WIDTHS.forEach { width -> SkeletonBlock(width, 36.dp) }
                    }
                }
            }
            Section(60.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(DAYS) { SkeletonBlock(42.dp, 42.dp, shape = CircleShape) }
                }
            }
        }
    }
}

/** A section's heading block and, under it, its controls. */
@Composable
private fun Section(headingWidth: Dp, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SkeletonBlock(headingWidth, 18.dp, shape = RoundedCornerShape(9.dp))
        content()
    }
}

private val CHIP_WIDTHS = listOf(64.dp, 64.dp, 64.dp, 72.dp, 52.dp, 52.dp, 80.dp)
private const val DAYS = 7
