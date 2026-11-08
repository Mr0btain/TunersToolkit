package org.mr0btain.tunerstoolkit.ui.kit.hex

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
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

/** Draws [mark]'s bytes on this row (both columns): a tint (stronger when selected) and an edge where it starts or ends */
internal fun DrawScope.drawMark(mark: HexMark, row: AddressRange, geometry: HexGeometry, colors: TmColors) {
    if (!mark.range.overlaps(row)) return
    val tone = markTone(mark.key, colors)
    val from = (max(mark.range.start, row.start) - row.start).toInt()
    val to = (min(mark.range.endExclusive, row.endExclusive) - row.start).toInt() - 1
    val wordSize = row.size.toInt() / geometry.cells
    val spans = listOf(
        (geometry.cellX(from / wordSize) - 2) to (geometry.cellX(to / wordSize) + geometry.cellWidth + 2),
        geometry.asciiCharX(from) to geometry.asciiCharX(to + 1)
    )
    val fill: Color? = when {
        mark.selected -> tone.color.copy(alpha = if (mark.role == HexMarkRole.RECORD) 0.15f else 0.3f)
        mark.role == HexMarkRole.VALUES -> tone.color.copy(alpha = 0.14f)
        mark.role == HexMarkRole.AXIS -> tone.color.copy(alpha = 0.08f)
        else -> null
    }
    for ((left, right) in spans) {
        fill?.let { drawRect(it, topLeft = Offset(left, 0f), size = Size(right - left, size.height)) }
        if (mark.role != HexMarkRole.VALUES) {
            drawRect(tone.color.copy(alpha = 0.7f), topLeft = Offset(left, size.height - 2f), size = Size(right - left, 2f))
        }
        // An edge where the object starts and ends, so neighbours can be told apart
        if (mark.range.start >= row.start) drawRect(tone.color, topLeft = Offset(left, 0f), size = Size(2f, size.height))
        if (mark.range.endExclusive <= row.endExclusive) drawRect(tone.color, topLeft = Offset(right - 2f, 0f), size = Size(2f, size.height))
    }
}
