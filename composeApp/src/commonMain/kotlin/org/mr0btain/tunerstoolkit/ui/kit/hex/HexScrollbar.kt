package org.mr0btain.tunerstoolkit.ui.kit.hex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.uikit.theme.TmTheme
import kotlin.math.max

/**
 * A scrollbar that maps straight onto the rows, so dragging it or clicking the track goes anywhere in the image at
 * once, plus a minimap: changed bytes ([markers]) in the warning tone, [highlight] (a search match) and the
 * [cursor] marked along the track
 */
@Composable
fun HexScrollbar(
    listState: LazyListState,
    rows: HexRows,
    markers: List<AddressRange>,
    highlight: AddressRange?,
    cursor: Long?,
    modifier: Modifier = Modifier
) {
    val colors = TmTheme.colors
    val scope = rememberCoroutineScope()

    fun jumpTo(y: Float, height: Float) {
        if (rows.count == 0 || height <= 0f) return
        val visible = listState.layoutInfo.visibleItemsInfo.size
        val target = ((y / height) * rows.count).toInt() - visible / 2
        scope.launch { listState.scrollToItem(target.coerceIn(0, max(0, rows.count - 1))) }
    }

    Canvas(
        modifier
            .width(14.dp)
            .fillMaxHeight()
            .pointerInput(rows) { detectTapGestures { jumpTo(it.y, size.height.toFloat()) } }
            .pointerInput(rows) {
                detectVerticalDragGestures { change, _ -> jumpTo(change.position.y, size.height.toFloat()) }
            }
    ) {
        val count = rows.count
        val radius = CornerRadius(size.width / 2)
        drawRoundRect(colors.surfaceSunken, cornerRadius = radius)
        if (count == 0) return@Canvas

        fun y(address: Long) = rows.rowOf(address).toFloat() / count * size.height
        fun mark(range: AddressRange, color: androidx.compose.ui.graphics.Color, inset: Float) {
            val top = y(range.start)
            val bottom = max(y(range.last) + size.height / count, top + 2f)
            drawRect(color, topLeft = Offset(inset, top), size = Size(size.width - inset * 2, bottom - top))
        }

        markers.forEach { mark(it, colors.warning.color, size.width * 0.55f) }
        highlight?.let { mark(it, colors.success.color, 1f) }

        val first = listState.firstVisibleItemIndex
        val visible = listState.layoutInfo.visibleItemsInfo.size
        val thumbTop = first.toFloat() / count * size.height
        val thumbHeight = max(visible.toFloat() / count * size.height, 24f).coerceAtMost(size.height)
        drawRoundRect(
            colors.outline,
            topLeft = Offset(2f, thumbTop.coerceAtMost(size.height - thumbHeight)),
            size = Size(size.width - 4f, thumbHeight),
            cornerRadius = radius
        )

        cursor?.let { drawRect(colors.accent.color, topLeft = Offset(0f, y(it)), size = Size(size.width, 2f)) }
    }
}
