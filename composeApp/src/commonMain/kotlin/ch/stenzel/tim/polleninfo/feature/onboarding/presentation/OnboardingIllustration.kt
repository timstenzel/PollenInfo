package ch.stenzel.tim.polleninfo.feature.onboarding.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.map.GeoPoint
import ch.stenzel.tim.polleninfo.core.map.SWISS_BORDER
import ch.stenzel.tim.polleninfo.core.map.SwissMapProjection
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.core.ui.layout.drawWash
import ch.stenzel.tim.polleninfo.theme.meadowColors
import kotlin.math.min

/**
 * The welcome illustration (Figma "Onboarding · Hero"): Switzerland's outline on a pale fill, a dot
 * per station of [stations] with a gold halo on [selectedAbbr], and pollen grains around it. The
 * outline and the dots go through [SwissMapProjection], like the All stations map. Pure decoration:
 * a `Canvas` without semantics, which TalkBack never reaches.
 *
 * [stations] is empty until the list has loaded; the outline shows on its own until then.
 */
@Composable
fun OnboardingIllustration(stations: List<Station>, selectedAbbr: String?, modifier: Modifier = Modifier) {
    val land = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = LAND_ALPHA)
    val ink = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.meadowColors.accent
    Canvas(modifier.fillMaxWidth().height(HEIGHT)) {
        val mapWidth = min(MAP_WIDTH.toPx(), size.width - 2 * MAP_MIN_MARGIN.toPx())
        val mapHeight = mapWidth / SwissMapProjection.aspectRatio
        val origin = Offset((size.width - mapWidth) / 2, MAP_TOP.toPx())
        fun place(point: GeoPoint): Offset {
            val p = SwissMapProjection.project(point, mapWidth, mapHeight)
            return Offset(origin.x + p.x, origin.y + p.y)
        }

        val border = Path().apply {
            SWISS_BORDER.forEachIndexed { index, point ->
                val p = place(point)
                if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(border, land)
        drawPath(border, ink, style = Stroke(width = BORDER_WIDTH.toPx(), join = StrokeJoin.Round))

        stations.forEach { station ->
            val center = place(GeoPoint(station.latitude, station.longitude))
            drawCircle(ink, radius = DOT_RADIUS.toPx(), center = center)
            if (station.abbr == selectedAbbr) {
                drawCircle(accent.copy(alpha = HALO_ALPHA), radius = HALO_RADIUS.toPx(), center = center)
            }
        }

        // Figma places the grains on a 393 dp wide frame; they spread with the screen's width.
        val xScale = size.width / FRAME_WIDTH.toPx()
        GRAINS.forEach { (x, y, diameter) ->
            drawCircle(
                accent.copy(alpha = GRAIN_ALPHA),
                radius = (diameter / 2).dp.toPx(),
                center = Offset((x + diameter / 2).dp.toPx() * xScale, (y + diameter / 2).dp.toPx()),
            )
        }
    }
}

/**
 * Onboarding's own background washes, in place of the standard ones: a large sage wash behind the
 * illustration and a pollen-gold one at its top end.
 */
@Composable
fun OnboardingWashes(modifier: Modifier) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val wash = MaterialTheme.meadowColors.wash.copy(alpha = if (dark) 0.9f else 1f)
    val accent = MaterialTheme.meadowColors.accent.copy(alpha = if (dark) 0.25f else 0.45f)
    Canvas(modifier) {
        drawWash(wash, center = Offset(140.dp.toPx(), 200.dp.toPx()), radius = 260.dp.toPx())
        drawWash(accent, center = Offset(size.width - 58.dp.toPx(), 145.dp.toPx()), radius = 85.dp.toPx())
    }
}

private val HEIGHT = 330.dp
private val FRAME_WIDTH = 393.dp
private val MAP_WIDTH = 330.dp
private val MAP_MIN_MARGIN = 20.dp
private val MAP_TOP = 70.dp
private val BORDER_WIDTH = 2.dp
private val DOT_RADIUS = 5.dp
private val HALO_RADIUS = 15.dp
private const val LAND_ALPHA = 0.75f
private const val HALO_ALPHA = 0.45f
private const val GRAIN_ALPHA = 0.8f

/** Figma's grains: x and y of the top-left corner and the diameter, in dp of the 393 dp frame. */
private val GRAINS = listOf(
    Triple(60f, 40f, 10f),
    Triple(300f, 20f, 7f),
    Triple(340f, 250f, 12f),
    Triple(30f, 280f, 8f),
    Triple(180f, 10f, 6f),
    Triple(250f, 300f, 9f),
)
