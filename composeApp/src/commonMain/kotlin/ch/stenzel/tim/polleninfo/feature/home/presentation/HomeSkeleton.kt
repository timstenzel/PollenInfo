package ch.stenzel.tim.polleninfo.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.ui.layout.MeadowCard
import ch.stenzel.tim.polleninfo.core.ui.severity.GAUGE_HEIGHT
import ch.stenzel.tim.polleninfo.core.ui.severity.GAUGE_WIDTH
import ch.stenzel.tim.polleninfo.core.ui.severity.SPECIES_TILE_GAP
import ch.stenzel.tim.polleninfo.core.ui.severity.drawGaugeSegments
import ch.stenzel.tim.polleninfo.core.ui.status.ShimmerBox
import ch.stenzel.tim.polleninfo.core.ui.status.SkeletonBlock
import ch.stenzel.tim.polleninfo.core.ui.status.SkeletonCanvas

/**
 * Home while its reading loads (Figma "Home · Loading"): the gauge card and the first tiles as grey
 * blocks under one shimmer. The station name above it is real, so the screen is identifiable.
 */
@Composable
internal fun HomeSkeleton(modifier: Modifier = Modifier) {
    ShimmerBox(modifier) {
        Column {
            MeadowCard(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SkeletonBlock(120.dp, 12.dp)
                    val sunken = MaterialTheme.colorScheme.surfaceContainerHighest
                    Box(contentAlignment = Alignment.BottomCenter) {
                        SkeletonCanvas(Modifier.size(GAUGE_WIDTH, GAUGE_HEIGHT)) { drawGaugeSegments { sunken } }
                        SkeletonBlock(130.dp, 44.dp, Modifier.padding(bottom = 16.dp), MaterialTheme.shapes.small)
                    }
                    SkeletonBlock(150.dp, 14.dp)
                    SkeletonBlock(200.dp, 24.dp)
                }
            }
            Spacer(Modifier.height(20.dp))
            SkeletonBlock(150.dp, 26.dp, shape = MaterialTheme.shapes.small)
            Spacer(Modifier.height(20.dp))
            repeat(SKELETON_TILE_ROWS) { row ->
                if (row > 0) Spacer(Modifier.height(SPECIES_TILE_GAP))
                Row(horizontalArrangement = Arrangement.spacedBy(SPECIES_TILE_GAP)) {
                    repeat(2) { SkeletonTile(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun SkeletonTile(modifier: Modifier) {
    MeadowCard(modifier, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SkeletonBlock(40.dp, 40.dp, shape = MaterialTheme.shapes.small)
                SkeletonBlock(44.dp, 20.dp)
            }
            Spacer(Modifier.height(12.dp))
            SkeletonBlock(80.dp, 16.dp)
            Spacer(Modifier.height(12.dp))
            SkeletonBlock(54.dp, 12.dp)
            Spacer(Modifier.height(12.dp))
            SkeletonBlock(0.dp, 8.dp, Modifier.fillMaxWidth())
        }
    }
}

/** Two rows of tiles fill the screen below the gauge card; more would only be scrolled to. */
private const val SKELETON_TILE_ROWS = 2
