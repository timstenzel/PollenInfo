package ch.stenzel.tim.polleninfo.feature.allstations.map

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min

/** A position on a canvas, in the canvas's own units (pixels on screen, viewport units in an icon). */
data class MapPoint(val x: Float, val y: Float)

/**
 * Places WGS84 points on a flat canvas showing Switzerland.
 *
 * Equirectangular, with longitude scaled by cos 46.8° (the country's mid-latitude) so a degree east
 * is as long as it is at that latitude: across Switzerland the distortion is a few percent, far below
 * what a map without basemap can show. The border, the station dots and the tab icon all go through
 * [project], so they agree by construction.
 *
 * The frame is [SWISS_BORDER]'s bounding box padded by [MARGIN] of its extent on every side, so a
 * dot on a station near the border is never clipped. A canvas of any shape gets the whole frame,
 * scaled to fit inside and centred on the other axis — the map is never cropped.
 *
 * Pure Kotlin on purpose: no Compose types, so the geometry is tested in `commonTest`.
 */
object SwissMapProjection {

    private val LONGITUDE_SCALE = cos(46.8 * PI / 180)

    /** Padding around the border's bounding box, as a fraction of its width and height. */
    private const val MARGIN = 0.04

    private val minX: Double
    private val maxY: Double
    private val frameWidth: Double
    private val frameHeight: Double

    init {
        val xs = SWISS_BORDER.map { it.longitude * LONGITUDE_SCALE }
        val ys = SWISS_BORDER.map { it.latitude }
        val width = xs.max() - xs.min()
        val height = ys.max() - ys.min()
        minX = xs.min() - width * MARGIN
        maxY = ys.max() + height * MARGIN
        frameWidth = width * (1 + 2 * MARGIN)
        frameHeight = height * (1 + 2 * MARGIN)
    }

    /** Width / height of the map's frame (≈ 1.55); a canvas of this shape is filled exactly. */
    val aspectRatio: Float = (frameWidth / frameHeight).toFloat()

    /** Projects [point] into a canvas of [width] × [height], keeping the map's shape and centring it. */
    fun project(point: GeoPoint, width: Float, height: Float): MapPoint {
        val scale = min(width / frameWidth, height / frameHeight)
        val offsetX = (width - frameWidth * scale) / 2
        val offsetY = (height - frameHeight * scale) / 2
        return MapPoint(
            x = (offsetX + (point.longitude * LONGITUDE_SCALE - minX) * scale).toFloat(),
            // Canvas y grows downwards, latitude grows northwards.
            y = (offsetY + (maxY - point.latitude) * scale).toFloat(),
        )
    }
}
