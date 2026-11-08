package org.mr0btain.tunerstoolkit.ui.kit.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationValues
import org.mr0btain.tunerstoolkit.core.calibration.columns
import org.mr0btain.tunerstoolkit.core.calibration.formatValue
import org.mr0btain.tunerstoolkit.core.calibration.indexAt
import org.mr0btain.tunerstoolkit.core.calibration.rows
import org.mr0btain.uikit.theme.TmColors
import org.mr0btain.uikit.theme.TmTheme
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * A line chart of an object's values over its x axis: one line for a curve, one per row for a map (coloured low to
 * high down the y axis, [highlightRow] drawn thicker). [original]'s line is drawn dashed behind, for the curve or the
 * highlighted row, so a change shows against where it started
 * @param outer Which slice of a cube
 */
@Composable
fun TmMapChart2D(
    values: CalibrationValues,
    modifier: Modifier = Modifier,
    original: CalibrationValues? = null,
    outer: List<Int> = emptyList(),
    highlightRow: Int? = null
) {
    val colors = TmTheme.colors
    val style = TmTheme.typography.numeric.copy(fontSize = 11.sp, color = colors.textMuted)
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        val xs = axisPositions(values, 0, values.columns)
        val rows = values.rows
        val lines = List(rows) { y -> DoubleArray(values.columns) { x -> values.physical[values.indexAt(x, y, outer)] } }
        val ghost = original?.takeIf { it.size == values.size }?.let { o ->
            val row = highlightRow ?: 0
            DoubleArray(values.columns) { x -> o.physical[o.indexAt(x, row, outer)] }
        }
        val all = lines.flatMap { it.asIterable() } + (ghost?.asIterable() ?: emptyList())
        val low = all.filter { !it.isNaN() }.minOrNull() ?: 0.0
        val high = all.filter { !it.isNaN() }.maxOrNull() ?: 1.0
        val plot = Plot(this, low, if (high == low) low + 1 else high, xs.first(), if (xs.last() == xs.first()) xs.first() + 1 else xs.last(), measurer, style)
        plot.grid(colors, values.obj.format, values.axes.getOrNull(0)?.axis?.format)

        ghost?.let { plot.line(xs, it, colors.textFaint, 1.5f, dashed = true) }
        lines.forEachIndexed { y, line ->
            val t = if (rows <= 1) 0f else y.toFloat() / (rows - 1)
            val color = lerp(colors.accent.color, colors.danger.color, t)
            val highlighted = highlightRow == y || rows == 1
            plot.line(xs, line, if (highlightRow == null || highlighted) color else color.copy(alpha = 0.45f), if (highlighted) 2.5f else 1.25f)
        }
    }
}

/** Positions along an axis: its physical points when it has them, 0, 1, 2… otherwise */
private fun axisPositions(values: CalibrationValues, axis: Int, count: Int): DoubleArray {
    val points = values.axes.getOrNull(axis)?.physical
    return if (points != null && points.size == count && points.none { it.isNaN() }) points else DoubleArray(count) { it.toDouble() }
}

/** Maps values onto a chart's area, inside margins for the labels */
private class Plot(
    val scope: DrawScope,
    val low: Double,
    val high: Double,
    val xLow: Double,
    val xHigh: Double,
    val measurer: TextMeasurer,
    val style: TextStyle
) {
    val left = 56f
    val bottom = scope.size.height - 22f
    val top = 8f
    val right = scope.size.width - 8f

    fun x(value: Double): Float = (left + (value - xLow) / (xHigh - xLow) * (right - left)).toFloat()
    fun y(value: Double): Float = (bottom - (value - low) / (high - low) * (bottom - top)).toFloat()

    fun grid(colors: TmColors, format: String?, xFormat: String?) = with(scope) {
        for (i in 0..4) {
            val value = low + (high - low) * i / 4
            val y = y(value)
            drawLine(colors.hairline, Offset(left, y), Offset(right, y))
            val label = measurer.measure(formatValue(value, format), style)
            drawText(label, topLeft = Offset(left - 6f - label.size.width, y - label.size.height / 2))
        }
        listOf(xLow, xHigh).forEach { value ->
            val label = measurer.measure(formatValue(value, xFormat), style)
            val x = (x(value) - label.size.width / 2).coerceIn(left, right - label.size.width)
            drawText(label, topLeft = Offset(x, bottom + 4f))
        }
        drawLine(colors.outline, Offset(left, top), Offset(left, bottom))
        drawLine(colors.outline, Offset(left, bottom), Offset(right, bottom))
    }

    fun line(xs: DoubleArray, ys: DoubleArray, color: Color, width: Float, dashed: Boolean = false) = with(scope) {
        val path = Path()
        var started = false
        for (i in ys.indices) {
            if (ys[i].isNaN()) continue
            val point = Offset(x(xs[i]), y(ys[i]))
            if (started) path.lineTo(point.x, point.y) else path.moveTo(point.x, point.y)
            started = true
        }
        drawPath(path, color, style = Stroke(width = width, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6f, 5f)) else null))
        if (!dashed && xs.size <= 40) ys.forEachIndexed { i, v -> if (!v.isNaN()) drawCircle(color, 2.5f, Offset(x(xs[i]), y(v))) }
    }
}

/**
 * A map as a 3D surface: x across, y into the screen, values up, each patch coloured low to high. Drag to turn it
 * round (sideways) and tilt it (up and down). Drawn back to front on a Canvas, no 3D library
 */
@Composable
fun TmMapChart3D(values: CalibrationValues, modifier: Modifier = Modifier, outer: List<Int> = emptyList()) {
    val colors = TmTheme.colors
    var yaw by remember { mutableFloatStateOf(-0.6f) }
    var pitch by remember { mutableFloatStateOf(0.55f) }
    val style = TmTheme.typography.caption.copy(color = colors.textMuted)
    val measurer = rememberTextMeasurer()

    Canvas(
        modifier.pointerInput(Unit) {
            detectDragGestures { change, drag ->
                change.consume()
                yaw += drag.x * 0.01f
                pitch = (pitch + drag.y * 0.01f).coerceIn(0.05f, 1.45f)
            }
        }
    ) {
        val nx = values.columns
        val ny = values.rows
        if (nx < 2 || ny < 2) return@Canvas
        val xs = normalise(axisPositions(values, 0, nx))
        val ys = normalise(axisPositions(values, 1, ny))
        val heights = DoubleArray(nx * ny) { values.physical[values.indexAt(it % nx, it / nx, outer)] }
        val low = heights.filter { !it.isNaN() }.minOrNull() ?: 0.0
        val high = heights.filter { !it.isNaN() }.maxOrNull() ?: 1.0
        val span = if (high == low) 1.0 else high - low

        val cosYaw = cos(yaw)
        val sinYaw = sin(yaw)
        val cosPitch = cos(pitch)
        val sinPitch = sin(pitch)
        val scale = min(size.width, size.height) * 0.36f
        val center = Offset(size.width / 2, size.height * 0.55f)

        // A point at (x, y) across the floor (-1..1) and height h (0..1): turned, tilted and flattened onto the screen
        fun project(x: Float, y: Float, h: Float): Pair<Offset, Float> {
            val rx = x * cosYaw - y * sinYaw
            val ry = x * sinYaw + y * cosYaw
            val z = (h - 0.5f) * 1.1f
            // Tilting: the floor's far edge rises up the screen, and depth is how far into it a point is
            val up = z * cosPitch + ry * sinPitch
            val depth = ry * cosPitch - z * sinPitch
            return Offset(center.x + rx * scale, center.y - up * scale) to depth
        }

        fun point(i: Int, j: Int): Pair<Offset, Float> {
            val h = ((heights[j * nx + i] - low) / span).toFloat()
            return project(xs[i] * 2 - 1, ys[j] * 2 - 1, if (h.isNaN()) 0f else h)
        }

        class Patch(val corners: List<Offset>, val depth: Float, val level: Float)
        val patches = ArrayList<Patch>((nx - 1) * (ny - 1))
        for (j in 0 until ny - 1) for (i in 0 until nx - 1) {
            val corners = listOf(point(i, j), point(i + 1, j), point(i + 1, j + 1), point(i, j + 1))
            val level = ((listOf(heights[j * nx + i], heights[j * nx + i + 1], heights[(j + 1) * nx + i + 1], heights[(j + 1) * nx + i]).average() - low) / span).toFloat()
            patches += Patch(corners.map { it.first }, corners.map { it.second }.average().toFloat(), if (level.isNaN()) 0f else level)
        }
        // Furthest first, so nearer patches cover them
        patches.sortByDescending { it.depth }
        for (patch in patches) {
            val path = Path().apply {
                moveTo(patch.corners[0].x, patch.corners[0].y)
                patch.corners.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            drawPath(path, lerp(colors.accent.color, colors.danger.color, patch.level).copy(alpha = 0.85f))
            drawPath(path, colors.background.copy(alpha = 0.6f), style = Stroke(width = 0.8f))
        }

        // Which way is which
        val xName = values.axes.getOrNull(0)?.axis?.input ?: "x"
        val yName = values.axes.getOrNull(1)?.axis?.input ?: "y"
        val xEnd = project(1.15f, -1f, 0f).first
        val yEnd = project(-1f, 1.15f, 0f).first
        drawText(measurer.measure(xName, style), topLeft = xEnd)
        drawText(measurer.measure(yName, style), topLeft = yEnd)
        drawText(measurer.measure("${formatValue(low, values.obj.format)} to ${formatValue(high, values.obj.format)} ${values.obj.unit}", style), topLeft = Offset(8f, 8f))
    }
}

/** Positions scaled to 0..1 */
private fun normalise(points: DoubleArray): FloatArray {
    val low = points.min()
    val high = points.max()
    val span = if (high == low) 1.0 else high - low
    return FloatArray(points.size) { ((points[it] - low) / span).toFloat() }
}
