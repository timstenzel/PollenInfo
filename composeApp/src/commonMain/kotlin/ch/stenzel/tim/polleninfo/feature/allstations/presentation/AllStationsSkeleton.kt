package ch.stenzel.tim.polleninfo.feature.allstations.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.map.SwissMapProjection
import ch.stenzel.tim.polleninfo.core.ui.layout.MeadowCard
import ch.stenzel.tim.polleninfo.core.ui.status.ShimmerBox
import ch.stenzel.tim.polleninfo.core.ui.status.SkeletonBlock
import ch.stenzel.tim.polleninfo.core.ui.status.SkeletonCanvas

/**
 * All stations while the station list loads (Figma "All stations · Loading"): the map card, the
 * list heading and the first rows as grey blocks under one shimmer. The screen's title above it is
 * real.
 */
@Composable
internal fun AllStationsSkeleton(modifier: Modifier = Modifier) {
    ShimmerBox(modifier) {
        Column {
            MeadowCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val sunken = MaterialTheme.colorScheme.surfaceContainerHighest
                    val shape = MaterialTheme.shapes.small
                    SkeletonCanvas(Modifier.fillMaxWidth().aspectRatio(SwissMapProjection.aspectRatio)) {
                        drawOutline(shape.createOutline(size, layoutDirection, this), sunken)
                    }
                    SkeletonBlock(0.dp, 12.dp, Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(20.dp))
            SkeletonBlock(130.dp, 26.dp, shape = MaterialTheme.shapes.small)
            Spacer(Modifier.height(20.dp))
            SKELETON_NAME_WIDTHS.forEachIndexed { index, nameWidth ->
                if (index > 0) Spacer(Modifier.height(8.dp))
                SkeletonRow(nameWidth.dp)
            }
        }
    }
}

/** A station row: its dot, the name and the level line. */
@Composable
private fun SkeletonRow(nameWidth: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBlock(14.dp, 14.dp, shape = CircleShape)
        Spacer(Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBlock(nameWidth, 14.dp)
            SkeletonBlock(70.dp, 10.dp)
        }
    }
}

/** Varied like real station names, so the rows read as a list rather than a pattern. */
private val SKELETON_NAME_WIDTHS = listOf(110, 90, 130, 100, 80, 120)
