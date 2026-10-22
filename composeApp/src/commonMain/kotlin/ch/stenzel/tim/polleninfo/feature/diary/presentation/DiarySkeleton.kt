package ch.stenzel.tim.polleninfo.feature.diary.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.ui.layout.MeadowCard
import ch.stenzel.tim.polleninfo.core.ui.status.ShimmerBox
import ch.stenzel.tim.polleninfo.core.ui.status.SkeletonBlock

/**
 * The Diary while its first history loads (Figma "Diary · Loading"): the station and range controls,
 * the chart card as a row of bars, the heading and the chips, as grey blocks under one shimmer. The
 * range is a block too — nothing is selectable before the first load.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DiarySkeleton(modifier: Modifier = Modifier) {
    ShimmerBox(modifier) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBlock(110.dp, 38.dp)
                SkeletonBlock(210.dp, 48.dp)
            }
            Spacer(Modifier.height(DIARY_SECTION_GAP))
            MeadowCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    SkeletonBlock(120.dp, 18.dp, shape = BAR_SHAPE)
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(190.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        BAR_HEIGHTS.forEach { SkeletonBlock(36.dp, it.dp, shape = BAR_SHAPE) }
                    }
                }
            }
            Spacer(Modifier.height(DIARY_SECTION_GAP))
            SkeletonBlock(150.dp, 26.dp, shape = RoundedCornerShape(10.dp))
            Spacer(Modifier.height(DIARY_SECTION_GAP))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CHIP_WIDTHS.forEach { SkeletonBlock(it.dp, 38.dp) }
            }
        }
    }
}

private val BAR_SHAPE = RoundedCornerShape(8.dp)
private val BAR_HEIGHTS = listOf(70, 110, 120, 170, 120, 100, 80)
private val CHIP_WIDTHS = listOf(80, 84, 80, 90, 70, 74, 96)
