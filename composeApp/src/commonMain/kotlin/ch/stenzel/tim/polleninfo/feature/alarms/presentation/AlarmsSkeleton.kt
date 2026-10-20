package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
 * The Alarms tab while its list loads (Figma "Alarms · Loading"): three alarm cards — icon tile,
 * two text lines, the switch and the weekday circles — as grey blocks under one shimmer.
 */
@Composable
internal fun AlarmsSkeleton(modifier: Modifier = Modifier) {
    ShimmerBox(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(SKELETON_CARDS) {
                MeadowCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SkeletonBlock(46.dp, 46.dp, shape = RoundedCornerShape(12.dp))
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SkeletonBlock(170.dp, 16.dp, shape = RoundedCornerShape(7.dp))
                                SkeletonBlock(70.dp, 12.dp, shape = RoundedCornerShape(6.dp))
                            }
                            SkeletonBlock(52.dp, 32.dp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            repeat(DAYS) { SkeletonBlock(34.dp, 34.dp, shape = CircleShape) }
                        }
                    }
                }
            }
        }
    }
}

private const val SKELETON_CARDS = 3
private const val DAYS = 7
