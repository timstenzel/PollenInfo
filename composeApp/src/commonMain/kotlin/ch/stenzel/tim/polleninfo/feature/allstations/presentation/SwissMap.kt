package ch.stenzel.tim.polleninfo.feature.allstations.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.ui.severity.color
import ch.stenzel.tim.polleninfo.feature.allstations.domain.model.StationReading
import ch.stenzel.tim.polleninfo.feature.allstations.map.GeoPoint
import ch.stenzel.tim.polleninfo.feature.allstations.map.MapPoint
import ch.stenzel.tim.polleninfo.feature.allstations.map.SWISS_BORDER
import ch.stenzel.tim.polleninfo.feature.allstations.map.SWISS_LAKES
import ch.stenzel.tim.polleninfo.feature.allstations.map.SwissMapProjection
import ch.stenzel.tim.polleninfo.feature.allstations.map.nearestStation
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.all_stations_map_description
import ch.stenzel.tim.polleninfo.theme.mapWaterDark
import ch.stenzel.tim.polleninfo.theme.mapWaterLight
import ch.stenzel.tim.polleninfo.theme.meadowColors
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.pluralStringResource

/**
 * Switzerland's outline and its big lakes, with one dot per station at its true position, coloured
 * by its overall severity and ringed in the card's colour so neighbouring dots stay apart. The lakes
 * are there only so the dots are easier to place. The selected station's dot is larger, sits in a
 * soft halo of its own colour and carries a "Name · Level" tag ([selectedLabel]).
 *
 * Fits the available width at the map's own aspect ratio, and never grows taller than [maxHeight] —
 * when capped it is narrower and centred, never cropped.
 *
 * A tap selects the station whose dot is nearest, within [TAP_RADIUS] ([nearestStation]), through
 * [onStationClick] — the same entry point as the list rows, so a dot tap and a row tap behave alike,
 * including deselecting on a second tap. A tap farther from every dot does nothing.
 *
 * To a screen reader it is one element, tag included: the dots repeat what the list says, and
 * selecting happens in the list, so fifteen focus stops here would only be in the way.
 */
@Composable
fun SwissMap(
    stations: List<StationReading>,
    selectedAbbr: String?,
    selectedLabel: String?,
    maxHeight: Dp,
    onStationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Read in composition: color() follows the applied scheme, which a draw block cannot ask.
    val dots = stations.map { reading -> MapDot(reading.station.position, dotStyleOf(reading)) }
    val selected = stations.firstOrNull { it.station.abbr == selectedAbbr }
    val selectedDot = selected?.let { MapDot(it.station.position, dotStyleOf(it)) }
    val ringColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val landFill = MaterialTheme.meadowColors.mapLand
    val borderStroke = MaterialTheme.colorScheme.primary.copy(alpha = BORDER_STROKE_ALPHA)
    // Dark is read off the applied surface, the same rule as PollenSeverity.color(), so the lakes
    // follow the scheme the dots do.
    val darkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val lakeFill = if (darkTheme) mapWaterDark else mapWaterLight
    val positions = stations.associate { it.station.abbr to it.station.position }
    val currentOnStationClick by rememberUpdatedState(onStationClick)
    val description = pluralStringResource(Res.plurals.all_stations_map_description, stations.size, stations.size)

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Layout(
            content = {
                if (selectedDot != null && selectedLabel != null) SelectedTag(selectedLabel)
            },
            modifier = Modifier
                // Order matters: the aspect ratio is resolved inside the height cap, so a capped
                // map gives up width rather than its shape.
                .heightIn(max = maxHeight)
                .aspectRatio(SwissMapProjection.aspectRatio)
                .clearAndSetSemantics { contentDescription = description }
                .pointerInput(positions) {
                    val radius = TAP_RADIUS.toPx()
                    detectTapGestures { tap ->
                        // Projected at tap time into the current size, through the same projection
                        // the dots are drawn with, so what is hit is exactly what is seen.
                        val width = size.width.toFloat()
                        val height = size.height.toFloat()
                        val dots = positions.mapValues { (_, position) ->
                            SwissMapProjection.project(position, width, height)
                        }
                        nearestStation(MapPoint(tap.x, tap.y), dots, radius)?.let(currentOnStationClick)
                    }
                }
                .drawWithCache {
                    val border = ringPath(SWISS_BORDER, size)
                    val lakes = SWISS_LAKES.map { ringPath(it.outline, size) }
                    val borderStrokeStyle = Stroke(width = BORDER_STROKE_WIDTH.toPx(), join = StrokeJoin.Round)
                    onDrawBehind {
                        drawPath(border, landFill)
                        // Landmarks only, a flat fill beneath the border line and the dots.
                        lakes.forEach { lake -> drawPath(lake, lakeFill) }
                        drawPath(border, borderStroke, style = borderStrokeStyle)
                        dots.forEach { dot -> drawDot(dot, DOT_RADIUS.toPx(), ringColor) }
                        // On top of its own small dot, so the larger one is never half covered.
                        selectedDot?.let { dot ->
                            val p = SwissMapProjection.project(dot.position, size.width, size.height)
                            drawCircle(dot.style.color.copy(alpha = HALO_ALPHA), HALO_RADIUS.toPx(), Offset(p.x, p.y))
                            drawDot(dot, SELECTED_DOT_RADIUS.toPx(), ringColor)
                        }
                    }
                },
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            // The tag may be as wide as the map, never wider; a longer name is cut with an ellipsis.
            val tag = measurables.firstOrNull()?.measure(Constraints(maxWidth = width))
            layout(width, height) {
                if (tag != null && selectedDot != null) {
                    val p = SwissMapProjection.project(selectedDot.position, width.toFloat(), height.toFloat())
                    val gap = TAG_GAP.roundToPx()
                    val halo = HALO_RADIUS.roundToPx()
                    // Above the halo; below it when a station near the northern border leaves no room.
                    val above = p.y.roundToInt() - halo - gap - tag.height
                    val y = if (above >= 0) above else p.y.roundToInt() + halo + gap
                    // Centred on the dot, but kept inside the map at the east and west ends.
                    val x = (p.x.roundToInt() - tag.width / 2).coerceIn(0, (width - tag.width).coerceAtLeast(0))
                    tag.place(x, y)
                }
            }
        }
    }
}

/** The selected station's "Name · Level" in a forest-green pill (Figma "SelectedTag"). */
@Composable
private fun SelectedTag(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
    )
}

/** A station's dot of [radius] inside a ring of [ringColor], [RING_WIDTH] wide. */
private fun DrawScope.drawDot(dot: MapDot, radius: Float, ringColor: Color) {
    val p = SwissMapProjection.project(dot.position, size.width, size.height)
    val center = Offset(p.x, p.y)
    drawCircle(ringColor, radius + RING_WIDTH.toPx(), center)
    when (val style = dot.style) {
        is DotStyle.Filled -> drawCircle(style.color, radius, center)
        is DotStyle.Hollow -> {
            val stroke = Stroke(width = HOLLOW_DOT_STROKE_WIDTH.toPx())
            drawCircle(style.color, radius - stroke.width / 2, center, style = stroke)
        }
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
    // The same colour function as the row's dot, so a dot and its row cannot disagree.
    is StationReading.Available -> DotStyle.Filled(reading.overview.overallSeverity.color())

    // Neutral, so the map is useful at once and no colour is shown before it means something.
    is StationReading.Pending -> DotStyle.Filled(MaterialTheme.colorScheme.outlineVariant)

    // Hollow, so a missing reading is never mistaken for a filled None dot — the bar's empty track.
    is StationReading.Unavailable -> DotStyle.Hollow(MaterialTheme.colorScheme.outline)
}

private val Station.position: GeoPoint
    get() = GeoPoint(latitude, longitude)

/** Figma's dots: 12 dp, the selected one 18 dp, each in a 2 dp ring of the card's colour. */
private val DOT_RADIUS = 6.dp
private val SELECTED_DOT_RADIUS = 9.dp
private val RING_WIDTH = 2.dp
private val HALO_RADIUS = 17.dp
private const val HALO_ALPHA = 0.22f

/** Between the halo and the tag. */
private val TAG_GAP = 1.dp

/** How far from a dot a tap still selects it — a fingertip, much larger than the dot itself. */
private val TAP_RADIUS = 24.dp
private val HOLLOW_DOT_STROKE_WIDTH = 1.5.dp
private val BORDER_STROKE_WIDTH = 1.5.dp
private const val BORDER_STROKE_ALPHA = 0.55f
