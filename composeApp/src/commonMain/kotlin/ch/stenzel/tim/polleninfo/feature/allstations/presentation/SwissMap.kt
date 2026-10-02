package ch.stenzel.tim.polleninfo.feature.allstations.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.ui.severity.color
import ch.stenzel.tim.polleninfo.feature.allstations.domain.model.StationReading
import ch.stenzel.tim.polleninfo.feature.allstations.map.GeoPoint
import ch.stenzel.tim.polleninfo.feature.allstations.map.SWISS_BORDER
import ch.stenzel.tim.polleninfo.feature.allstations.map.SWISS_LAKES
import ch.stenzel.tim.polleninfo.feature.allstations.map.SwissMapProjection
import ch.stenzel.tim.polleninfo.theme.mapWaterDark
import ch.stenzel.tim.polleninfo.theme.mapWaterEdgeDark
import ch.stenzel.tim.polleninfo.theme.mapWaterEdgeLight
import ch.stenzel.tim.polleninfo.theme.mapWaterLight

/**
 * Switzerland's outline and its big lakes, with one dot per station at its true position, coloured
 * by its overall severity. The lakes are there only so the dots are easier to place.
 *
 * Fits the available width at the map's own aspect ratio, and never grows taller than [maxHeight] —
 * when capped it is narrower and centred, never cropped.
 *
 * To a screen reader it is one element: the dots repeat what the list says, and selecting happens in
 * the list, so fifteen focus stops here would only be in the way.
 */
@Composable
fun SwissMap(
    stations: List<StationReading>,
    selectedAbbr: String?,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
) {
    // Read in composition: color() follows the applied scheme, which a draw block cannot ask.
    val dots = stations.map { reading -> MapDot(reading.station.position, dotStyleOf(reading)) }
    val selected = stations.firstOrNull { it.station.abbr == selectedAbbr }?.station?.position
    val ringColor = MaterialTheme.colorScheme.primary
    val borderFill = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = BORDER_FILL_ALPHA)
    val borderStroke = MaterialTheme.colorScheme.outline
    // Dark is read off the applied surface, the same rule as PollenSeverity.color(), so the lakes
    // follow the scheme the dots do.
    val darkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val lakeFill = if (darkTheme) mapWaterDark else mapWaterLight
    val lakeStroke = if (darkTheme) mapWaterEdgeDark else mapWaterEdgeLight
    val description = "Map of ${stations.size} pollen stations. Select a station in the list below."

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Spacer(
            Modifier
                // Order matters: the aspect ratio is resolved inside the height cap, so a capped
                // map gives up width rather than its shape.
                .heightIn(max = maxHeight)
                .aspectRatio(SwissMapProjection.aspectRatio)
                .clearAndSetSemantics { contentDescription = description }
                .drawWithCache {
                    val border = ringPath(SWISS_BORDER, size)
                    val lakes = SWISS_LAKES.map { ringPath(it.outline, size) }
                    val lakeStrokeStyle = Stroke(width = LAKE_STROKE_WIDTH.toPx(), join = StrokeJoin.Round)
                    val borderStrokeStyle = Stroke(width = BORDER_STROKE_WIDTH.toPx(), join = StrokeJoin.Round)
                    val dotRadius = DOT_RADIUS.toPx()
                    val hollowStroke = Stroke(width = HOLLOW_DOT_STROKE_WIDTH.toPx())
                    val ringStroke = Stroke(width = RING_STROKE_WIDTH.toPx())
                    val ringRadius = (DOT_RADIUS + RING_GAP).toPx() + ringStroke.width / 2
                    onDrawBehind {
                        drawPath(border, borderFill)
                        // Landmarks only, beneath the border line and the dots.
                        lakes.forEach { lake ->
                            drawPath(lake, lakeFill)
                            drawPath(lake, lakeStroke, style = lakeStrokeStyle)
                        }
                        drawPath(border, borderStroke, style = borderStrokeStyle)
                        dots.forEach { dot ->
                            val p = SwissMapProjection.project(dot.position, size.width, size.height)
                            val center = Offset(p.x, p.y)
                            when (val style = dot.style) {
                                is DotStyle.Filled -> drawCircle(style.color, dotRadius, center)
                                is DotStyle.Hollow -> drawCircle(
                                    style.color,
                                    dotRadius - hollowStroke.width / 2,
                                    center,
                                    style = hollowStroke,
                                )
                            }
                        }
                        // Around the dot with a gap, never over it, so its severity colour still shows.
                        selected?.let { position ->
                            val p = SwissMapProjection.project(position, size.width, size.height)
                            drawCircle(ringColor, ringRadius, Offset(p.x, p.y), style = ringStroke)
                        }
                    }
                },
        )
    }
}

/** [points] as a closed path in a canvas of [size], through the projection every map layer shares. */
private fun ringPath(points: List<GeoPoint>, size: Size): Path = Path().apply {
    points.forEachIndexed { index, point ->
        val p = SwissMapProjection.project(point, size.width, size.height)
        if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}

private data class MapDot(val position: GeoPoint, val style: DotStyle)

private sealed interface DotStyle {
    val color: Color

    data class Filled(override val color: Color) : DotStyle

    data class Hollow(override val color: Color) : DotStyle
}

@Composable
private fun dotStyleOf(reading: StationReading): DotStyle = when (reading) {
    // The same colour function as the row's bar, so a dot and its row cannot disagree.
    is StationReading.Available -> DotStyle.Filled(reading.overview.overallSeverity.color())

    // Neutral, so the map is useful at once and no colour is shown before it means something.
    is StationReading.Pending -> DotStyle.Filled(MaterialTheme.colorScheme.outlineVariant)

    // Hollow, so a missing reading is never mistaken for a filled None dot — the bar's empty track.
    is StationReading.Unavailable -> DotStyle.Hollow(MaterialTheme.colorScheme.outline)
}

private val Station.position: GeoPoint
    get() = GeoPoint(latitude, longitude)

private val DOT_RADIUS = 5.dp
private val HOLLOW_DOT_STROKE_WIDTH = 1.5.dp
private val RING_STROKE_WIDTH = 2.dp
private val RING_GAP = 3.dp
private val BORDER_STROKE_WIDTH = 1.dp
private const val BORDER_FILL_ALPHA = 0.5f
private val LAKE_STROKE_WIDTH = 0.75.dp
