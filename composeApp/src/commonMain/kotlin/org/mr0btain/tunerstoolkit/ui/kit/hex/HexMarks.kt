package org.mr0btain.tunerstoolkit.ui.kit.hex

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.uikit.theme.TmColors
import org.mr0btain.uikit.theme.TmTone
import kotlin.math.max
import kotlin.math.min

/** What a marked stretch of bytes holds, which sets how it's drawn */
enum class HexMarkRole {
    /** Filled */
    VALUES,

    /** Filled more faintly, and underlined */
    AXIS,

    /** Underlined only: counts and other bookkeeping */
    RECORD
}

/**
 * A stretch of bytes belonging to something with a name, like a calibration map. [key] picks its color, the same
 * every time; [selected] fills it more strongly
 */
class HexMark(val range: AddressRange, val key: String, val role: HexMarkRole, val selected: Boolean = false)

/** The marks on the bytes of a row; asked for every row on screen as it's drawn */
fun interface HexMarks {
    fun on(range: AddressRange): List<HexMark>
}

/** The tone a mark's key always gets. Warning stays for changed bytes, so it isn't one of them */
internal fun markTone(key: String, colors: TmColors): TmTone {
    val tones = listOf(colors.accent, colors.success, colors.danger, colors.neutral)
    var hash = 0
    key.forEach { hash = hash * 31 + it.code }
    return tones[(hash and 0x7FFFFFFF) % tones.size]
}

/**
 * Draws [mark]'s bytes on this row (both columns): a tint (stronger when selected) inside a rounded outline. Each row
 * draws its own piece of the outline, with a top edge only where the row above isn't part of the mark and a bottom
 * edge only where the row below isn't, so the pieces join into one shape round the whole object.
 */
internal fun DrawScope.drawMark(mark: HexMark, row: AddressRange, geometry: HexGeometry, colors: TmColors) {
    if (!mark.range.overlaps(row)) return
    val tone = markTone(mark.key, colors)
    val perRow = row.size
    val from = (max(mark.range.start, row.start) - row.start).toInt()
    val to = (min(mark.range.endExclusive, row.endExclusive) - row.start).toInt() - 1
    // The first byte of this row's columns the row above covers, and the last the row below covers
    val aboveFrom = if (mark.range.start < row.start) (mark.range.start - (row.start - perRow)).coerceAtLeast(0).toInt() else null
    val belowTo = if (mark.range.endExclusive > row.endExclusive) (mark.range.endExclusive - 1 - row.endExclusive).coerceAtMost(perRow - 1).toInt() else null
    val wordSize = perRow.toInt() / geometry.cells

    val hex = MarkColumn(
        leftOf = { geometry.cellX(it / wordSize) - 2 },
        rightOf = { geometry.cellX(it / wordSize) + geometry.cellWidth + 2 }
    )
    val ascii = MarkColumn(leftOf = { geometry.asciiCharX(it) }, rightOf = { geometry.asciiCharX(it + 1) })

    val fill: Color? = when {
        mark.selected -> tone.color.copy(alpha = if (mark.role == HexMarkRole.RECORD) 0.15f else 0.3f)
        mark.role == HexMarkRole.VALUES -> tone.color.copy(alpha = 0.14f)
        mark.role == HexMarkRole.AXIS -> tone.color.copy(alpha = 0.08f)
        else -> null
    }
    val strokeWidth = (if (mark.selected) 2.dp else 1.dp).toPx()
    // Record bytes (counts and other bookkeeping) are dashed, so they read as part of the object but not its values
    val dash = if (mark.role == HexMarkRole.RECORD) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null
    val line = tone.color.copy(alpha = if (mark.selected) 1f else 0.75f)

    for (column in listOf(hex, ascii)) {
        val left = column.leftOf(from)
        val right = column.rightOf(to)
        // The top edge runs from the left to where the row above starts; the bottom from where the row below ends
        val topEnd = when (aboveFrom) {
            null -> right
            0 -> null
            else -> min(right, column.leftOf(aboveFrom))
        }?.takeIf { it > left }
        val bottomStart = when (belowTo) {
            null -> left
            perRow.toInt() - 1 -> null
            else -> max(left, column.rightOf(belowTo))
        }?.takeIf { it < right }

        val half = strokeWidth / 2
        val piece = MarkPiece(left + half, half, right - half, size.height - half, topEnd?.minus(half), bottomStart?.plus(half))
        val radius = minOf(4.dp.toPx(), (piece.right - piece.left) / 2, (piece.bottom - piece.top) / 2)
        // The fill runs the full row height, so the pieces of a mark meet without a seam
        fill?.let { drawPath(piece.fillPath(radius, 0f, size.height), it) }
        drawPath(piece.outlinePath(radius), line, style = Stroke(width = strokeWidth, pathEffect = dash))
    }
}

/** Where a byte's box starts and ends along one column of a row */
private class MarkColumn(val leftOf: (Int) -> Float, val rightOf: (Int) -> Float)

/**
 * One row's piece of a mark: a box whose top edge runs from [left] to [topEnd] and bottom edge from [bottomStart]
 * to [right] (null where there's none), rounded at the corners where an edge meets a side
 */
private class MarkPiece(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val topEnd: Float?,
    val bottomStart: Float?
) {
    private val roundTopLeft = topEnd != null
    private val roundTopRight = topEnd != null && topEnd >= right
    private val roundBottomRight = bottomStart != null
    private val roundBottomLeft = bottomStart != null && bottomStart <= left

    fun fillPath(radius: Float, top: Float, bottom: Float): Path = Path().apply {
        fun corner(round: Boolean) = if (round) CornerRadius(radius) else CornerRadius.Zero
        addRoundRect(
            RoundRect(
                left, top, right, bottom,
                topLeftCornerRadius = corner(roundTopLeft),
                topRightCornerRadius = corner(roundTopRight),
                bottomRightCornerRadius = corner(roundBottomRight),
                bottomLeftCornerRadius = corner(roundBottomLeft)
            )
        )
    }

    /** Clockwise from the bottom left: left side, top edge, right side, bottom edge */
    fun outlinePath(radius: Float): Path = Path().apply {
        val diameter = radius * 2
        moveTo(left, if (roundBottomLeft) bottom - radius else bottom)
        lineTo(left, if (roundTopLeft) top + radius else top)
        if (roundTopLeft) arcTo(Rect(left, top, left + diameter, top + diameter), 180f, 90f, false)
        if (topEnd != null) {
            if (roundTopRight) {
                lineTo(right - radius, top)
                arcTo(Rect(right - diameter, top, right, top + diameter), 270f, 90f, false)
            } else {
                lineTo(topEnd, top)
                moveTo(right, top)
            }
        } else {
            moveTo(right, top)
        }
        lineTo(right, if (roundBottomRight) bottom - radius else bottom)
        if (bottomStart != null) {
            arcTo(Rect(right - diameter, bottom - diameter, right, bottom), 0f, 90f, false)
            if (roundBottomLeft) {
                lineTo(left + radius, bottom)
                arcTo(Rect(left, bottom - diameter, left + diameter, bottom), 90f, 90f, false)
            } else {
                lineTo(bottomStart, bottom)
            }
        }
    }
}
