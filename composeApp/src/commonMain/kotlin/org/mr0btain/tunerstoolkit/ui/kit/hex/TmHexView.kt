package org.mr0btain.tunerstoolkit.ui.kit.hex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isShiftPressed as isPointerShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.mr0btain.applogger.AppLogger
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.image.toHexAddress
import org.mr0btain.uikit.theme.TmColors
import org.mr0btain.uikit.theme.TmTheme
import kotlin.math.max
import kotlin.math.min

/**
 * A hex and ASCII view of a [MemoryImage] that scrolls smoothly through any size: only the rows on screen exist,
 * each drawn on one Canvas straight from the image's bytes, and the scrollbar jumps anywhere at once.
 *
 * Click a byte to put the cursor there (shift-click or drag to select). Keys: arrows, Page Up/Down, Home/End
 * (with Ctrl for the start or end of everything), Tab between the hex and ASCII columns, hex digits or text to
 * overwrite, Esc to drop the selection, Ctrl+A for the segment, Ctrl+Z/Y to undo/redo, Ctrl+G go to, Ctrl+F find,
 * F3 find next, Ctrl+S save. Ctrl+C copies the selection (or the byte under the cursor) as hex in the hex column and
 * as text in the ASCII one, Ctrl+Shift+C the other way round; Ctrl+V pastes over the bytes from the selection or
 * cursor, the same way, never past the end of the segment. Ctrl is Cmd on a Mac.
 *
 * It never writes to [image] itself: edits go to [onWrite], so the screen's handler can record and redraw them.
 * @param version Changes whenever the image's bytes do (MemoryImage.version), so rows redraw
 * @param marks Tints bytes that belong to something, like calibration maps
 * @param coverage Where [marks] are, for the scrollbar's minimap
 * @param onOpenAt Called with the byte double-clicked (or double-tapped)
 */
@Composable
fun TmHexView(
    image: MemoryImage,
    version: Long,
    state: HexViewState,
    onWrite: (address: Long, bytes: ByteArray) -> Unit,
    modifier: Modifier = Modifier,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onGoTo: () -> Unit = {},
    onFind: () -> Unit = {},
    onFindNext: () -> Unit = {},
    onSave: () -> Unit = {},
    onMessage: (String) -> Unit = {},
    marks: HexMarks? = null,
    coverage: List<AddressRange> = emptyList(),
    onOpenAt: ((Long) -> Unit)? = null
) {
    val clipboard = LocalClipboard.current
    val colors = TmTheme.colors
    val style = TmTheme.typography.numeric.copy(fontSize = 13.sp, lineHeight = 18.sp, color = colors.text)
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val bytesPerRow = state.bytesPerRow
    val display = state.display
    val rows = remember(image, bytesPerRow) { HexRows(image.ranges, bytesPerRow) }
    val charWidth = remember(style) { measurer.measure("0", style).size.width.toFloat() }
    val lineHeight = remember(style) { measurer.measure("0", style).size.height.toFloat() }
    val padY = with(density) { 3.dp.toPx() }
    val rowHeightPx = lineHeight + padY * 2
    val rowHeight = with(density) { rowHeightPx.toDp() }
    val geometry = remember(charWidth, bytesPerRow, display) {
        HexGeometry(charWidth, bytesPerRow, display, with(density) { 12.dp.toPx() })
    }
    val changes = remember(image, version) { image.changedRanges() }
    // The last press, for telling a double-click
    var lastPress by remember { mutableStateOf<Pair<Long, Long>?>(null) }

    // The cursor starts on the first byte
    LaunchedEffect(image) {
        if (state.cursor == null) image.ranges.firstOrNull()?.let { state.moveTo(it.start) }
    }

    // Go to and find ask for an address through the state; a few rows above it stay in view
    LaunchedEffect(state.revealRequest, rows) {
        val address = state.revealRequest?.first ?: return@LaunchedEffect
        state.listState.scrollToItem((rows.rowOf(address) - 3).coerceAtLeast(0))
    }

    fun scrollToShow(address: Long) {
        val row = rows.rowOf(address)
        val info = state.listState.layoutInfo
        val first = state.listState.firstVisibleItemIndex
        val visible = max(1, info.visibleItemsInfo.size - 1)
        scope.launch {
            when {
                row < first -> state.listState.scrollToItem(row)
                row >= first + visible -> state.listState.scrollToItem(max(0, row - visible + 1))
            }
        }
    }

    // A move by [delta] bytes, skipping over gaps between segments
    fun step(from: Long, delta: Long): Long {
        val target = from + delta
        if (image.contains(target)) return target
        val ranges = image.ranges
        return if (delta > 0) {
            ranges.firstOrNull { it.endExclusive > target }?.let { maxOf(it.start, target) } ?: ranges.last().last
        } else {
            ranges.lastOrNull { it.start <= target }?.let { minOf(it.last, target) } ?: ranges.first().start
        }
    }

    // Typing over bytes: hex digits in the hex column, printable characters in the ASCII one
    fun typeAt(event: KeyEvent, cursor: Long): Boolean {
        if (state.readOnly) return false
        val code = event.utf16CodePoint
        if (state.asciiFocus) {
            if (code !in 0x20..0x7E) return false
            onWrite(cursor, byteArrayOf(code.toByte()))
            state.moveTo(step(cursor, 1))
            scrollToShow(state.cursor ?: cursor)
            return true
        }
        if (!display.editable) return false
        val digit = code.toChar().digitToIntOrNull(16) ?: return false
        val old = image.byteAtOrNull(cursor) ?: return false
        val new = if (state.nibble == 0) (digit shl 4) or (old and 0x0F) else (old and 0xF0) or digit
        onWrite(cursor, byteArrayOf(new.toByte()))
        if (state.nibble == 0) {
            state.moveTo(cursor)
            state.nibble = 1
        } else {
            state.moveTo(step(cursor, 1))
            scrollToShow(state.cursor ?: cursor)
        }
        return true
    }

    fun copy(asAscii: Boolean) {
        val cursor = state.cursor ?: return
        val range = state.selection ?: AddressRange.ofSize(cursor, 1)
        val text = HexClipboard.copy(image, range, asAscii)
        if (text == null) {
            onMessage(if (range.size > HexClipboard.MAX_COPY) "That's too much to copy at once" else "Nothing there to copy")
            return
        }
        scope.launch {
            try {
                clipboard.writeText(text)
                onMessage("Copied ${byteCount(range.size)}")
            } catch (e: Exception) {
                AppLogger.w("$TAG:Copy", "Couldn't copy", e)
                onMessage("Couldn't copy to the clipboard")
            }
        }
    }

    fun paste() {
        if (state.readOnly) return
        val at = state.selection?.start ?: state.cursor ?: return
        val asAscii = state.asciiFocus
        scope.launch {
            val text = try {
                clipboard.readText()
            } catch (e: Exception) {
                AppLogger.w("$TAG:Paste", "Couldn't read the clipboard", e)
                null
            }
            when (val paste = HexClipboard.paste(image, at, text.orEmpty(), asAscii)) {
                is HexClipboard.Paste.Write -> {
                    onWrite(paste.address, paste.bytes)
                    val pasted = AddressRange.ofSize(paste.address, paste.bytes.size.toLong())
                    state.select(pasted)
                    scrollToShow(pasted.last)
                    onMessage(
                        if (paste.dropped > 0) "Pasted ${byteCount(pasted.size)}; ${byteCount(paste.dropped.toLong())} more would run past the segment"
                        else "Pasted ${byteCount(pasted.size)}"
                    )
                }
                HexClipboard.Paste.NotHex -> onMessage("The clipboard doesn't hold hex bytes. Paste into the text column for text")
                HexClipboard.Paste.Empty -> onMessage("The clipboard is empty")
                HexClipboard.Paste.Outside -> Unit
            }
        }
    }

    fun handleKey(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        val command = event.isCtrlPressed || event.isMetaPressed
        val shift = event.isShiftPressed
        val cursor = state.cursor ?: return false
        val word = if (state.asciiFocus) 1L else display.wordSize.toLong()
        val pageRows = max(1, state.listState.layoutInfo.visibleItemsInfo.size - 2)

        fun move(to: Long) {
            state.moveTo(to, extend = shift)
            scrollToShow(to)
        }

        if (command) {
            when (event.key) {
                Key.Z -> if (shift) onRedo() else onUndo()
                Key.Y -> onRedo()
                Key.G -> onGoTo()
                Key.F -> onFind()
                Key.S -> onSave()
                Key.C -> copy(asAscii = state.asciiFocus != shift)
                Key.V -> paste()
                Key.A -> image.rangeAt(cursor)?.let { state.select(it) }
                Key.MoveHome -> move(image.ranges.first().start)
                Key.MoveEnd -> move(image.ranges.last().last)
                else -> return false
            }
            return true
        }

        when (event.key) {
            Key.DirectionRight -> move(step(cursor, word))
            Key.DirectionLeft -> move(step(cursor, -word))
            Key.DirectionDown -> move(step(cursor, bytesPerRow.toLong()))
            Key.DirectionUp -> move(step(cursor, -bytesPerRow.toLong()))
            Key.PageDown -> move(step(cursor, bytesPerRow.toLong() * pageRows))
            Key.PageUp -> move(step(cursor, -bytesPerRow.toLong() * pageRows))
            Key.MoveHome -> move(step(cursor - cursor % bytesPerRow, 0))
            Key.MoveEnd -> move(step(cursor - cursor % bytesPerRow + bytesPerRow - 1, 0))
            Key.Tab -> state.asciiFocus = !state.asciiFocus
            Key.Escape -> state.clearSelection()
            Key.F3 -> onFindNext()
            else -> return typeAt(event, cursor)
        }
        return true
    }

    // The byte under a point in the list, and whether it's in the ASCII column
    fun addressAt(position: Offset): Pair<Long, Boolean>? {
        val item = state.listState.layoutInfo.visibleItemsInfo.firstOrNull {
            position.y >= it.offset && position.y < it.offset + it.size
        } ?: return null
        val row = rows.rowAt(item.index) as? HexRows.Row.Data ?: return null
        val (byteIndex, ascii) = geometry.hit(position.x) ?: return null
        val address = row.address + byteIndex
        return if (image.contains(address)) address to ascii else null
    }

    Row(modifier) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent(::handleKey)
                .horizontalScroll(rememberScrollState())
        ) {
            LazyColumn(
                state = state.listState,
                modifier = Modifier
                    .width(with(density) { geometry.width.toDp() })
                    .fillMaxHeight()
                    .pointerInput(rows, geometry, onOpenAt) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val shift = currentEvent.keyboardModifiers.isPointerShiftPressed
                            focusRequester.requestFocus()
                            addressAt(down.position)?.let { (address, ascii) ->
                                state.asciiFocus = ascii
                                state.moveTo(address, extend = shift)
                                val previous = lastPress
                                if (onOpenAt != null && previous != null && previous.first == address && down.uptimeMillis - previous.second < DOUBLE_CLICK_MS) {
                                    lastPress = null
                                    onOpenAt(address)
                                } else {
                                    lastPress = address to down.uptimeMillis
                                }
                            }
                            // Dragging selects with a mouse; on a touch screen it scrolls instead
                            if (down.type != PointerType.Mouse) return@awaitEachGesture
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                addressAt(change.position)?.let { (address, _) -> state.moveTo(address, extend = true) }
                            }
                        }
                    }
            ) {
                items(rows.count, key = { it }) { index ->
                    when (val row = rows.rowAt(index)) {
                        is HexRows.Row.Data -> DataRow(
                            image = image,
                            version = version,
                            address = row.address,
                            state = state,
                            geometry = geometry,
                            style = style,
                            colors = colors,
                            measurer = measurer,
                            padY = padY,
                            marks = marks,
                            modifier = Modifier.fillMaxWidth().height(rowHeight)
                        )
                        is HexRows.Row.Gap -> GapRow(row.range, geometry, style, colors, measurer, Modifier.fillMaxWidth().height(rowHeight))
                    }
                }
            }
        }

        HexScrollbar(
            listState = state.listState,
            rows = rows,
            markers = changes,
            coverage = coverage,
            highlight = state.highlight,
            cursor = state.cursor
        )
    }
}

/** Where everything sits along a row, in pixels */
internal class HexGeometry(
    private val charWidth: Float,
    private val bytesPerRow: Int,
    private val display: HexDisplay,
    private val padX: Float
) {
    val cells: Int = bytesPerRow / display.wordSize
    val addressX: Float = padX
    val hexX: Float = padX + 10 * charWidth

    /** Characters from the start of the hex column to cell [cell]: a space between cells, and another every 8 bytes */
    fun cellColumn(cell: Int): Int = cell * (display.cellChars + 1) + (cell * display.wordSize) / 8

    fun cellX(cell: Int): Float = hexX + cellColumn(cell) * charWidth
    val cellWidth: Float = display.cellChars * charWidth
    val asciiX: Float = cellX(cells) + charWidth
    fun asciiCharX(byteIndex: Int): Float = asciiX + byteIndex * charWidth
    val width: Float = asciiX + bytesPerRow * charWidth + padX

    /** The byte index under [x] along a row, and whether it's in the ASCII column */
    fun hit(x: Float): Pair<Int, Boolean>? {
        if (x >= asciiX - charWidth / 2) {
            val index = ((x - asciiX) / charWidth).toInt().coerceIn(0, bytesPerRow - 1)
            return index to true
        }
        if (x < hexX - charWidth) return null
        for (cell in 0 until cells) {
            if (x < cellX(cell) + cellWidth + charWidth / 2) return cell * display.wordSize to false
        }
        return (cells - 1) * display.wordSize to false
    }

    /** The hex text of a row, laid out to match [cellX] */
    fun hexText(bytes: ByteArray, present: BooleanArray, changed: BooleanArray, colors: TmColors): AnnotatedString =
        buildAnnotatedString {
            var column = 0
            for (cell in 0 until cells) {
                val target = cellColumn(cell)
                while (column < target) {
                    append(' ')
                    column++
                }
                val first = cell * display.wordSize
                val range = first until first + display.wordSize
                val text = when {
                    range.all { present[it] } -> display.format(bytes, first)
                    else -> "·".repeat(display.cellChars)
                }
                val color = when {
                    !range.all { present[it] } -> colors.textFaint
                    range.any { changed[it] } -> colors.warning.color
                    else -> Color.Unspecified
                }
                if (color == Color.Unspecified) append(text) else withStyle(SpanStyle(color = color)) { append(text) }
                column += text.length
            }
        }

    fun asciiText(bytes: ByteArray, present: BooleanArray, changed: BooleanArray, colors: TmColors): AnnotatedString =
        buildAnnotatedString {
            for (i in 0 until bytesPerRow) {
                val value = bytes[i].toInt() and 0xFF
                when {
                    !present[i] -> append(' ')
                    changed[i] -> withStyle(SpanStyle(color = colors.warning.color)) { append(asciiChar(value)) }
                    value in 0x20..0x7E -> append(value.toChar())
                    else -> withStyle(SpanStyle(color = colors.textFaint)) { append('.') }
                }
            }
        }

    private fun asciiChar(value: Int): Char = if (value in 0x20..0x7E) value.toChar() else '.'
}

@Composable
private fun DataRow(
    image: MemoryImage,
    version: Long,
    address: Long,
    state: HexViewState,
    geometry: HexGeometry,
    style: TextStyle,
    colors: TmColors,
    measurer: androidx.compose.ui.text.TextMeasurer,
    padY: Float,
    marks: HexMarks?,
    modifier: Modifier
) {
    val bytesPerRow = state.bytesPerRow
    val layouts = remember(address, version, geometry, style, colors) {
        val bytes = ByteArray(bytesPerRow)
        val present = BooleanArray(bytesPerRow)
        image.readInto(address, bytesPerRow, bytes, present)
        val changed = BooleanArray(bytesPerRow) { present[it] && image.isChanged(address + it) }
        RowLayouts(
            address = measurer.measure(address.toHexAddress().removePrefix("0x"), style.copy(color = colors.textMuted)),
            hex = measurer.measure(geometry.hexText(bytes, present, changed, colors), style),
            ascii = measurer.measure(geometry.asciiText(bytes, present, changed, colors), style)
        )
    }

    Canvas(modifier) {
        val rowRange = AddressRange.ofSize(address, bytesPerRow.toLong())
        marks?.on(rowRange)?.forEach { drawMark(it, rowRange, geometry, colors) }
        state.highlight?.let { markBytes(it, rowRange, geometry, colors.success.soft) }
        state.selection?.let { markBytes(it, rowRange, geometry, colors.accent.soft) }
        state.cursor?.takeIf { it in rowRange }?.let { cursor ->
            drawCursor((cursor - address).toInt(), geometry, state, colors)
        }

        drawText(layouts.address, topLeft = Offset(geometry.addressX, padY))
        drawText(layouts.hex, topLeft = Offset(geometry.hexX, padY))
        drawText(layouts.ascii, topLeft = Offset(geometry.asciiX, padY))
    }
}

private class RowLayouts(val address: TextLayoutResult, val hex: TextLayoutResult, val ascii: TextLayoutResult)

/** A background behind the bytes of [range] that are on this row, in both columns */
private fun DrawScope.markBytes(range: AddressRange, row: AddressRange, geometry: HexGeometry, color: Color) {
    if (!range.overlaps(row)) return
    val from = (max(range.start, row.start) - row.start).toInt()
    val to = (min(range.endExclusive, row.endExclusive) - row.start).toInt() - 1
    val wordSize = geometry.cells.let { row.size.toInt() / it }
    val firstCell = from / wordSize
    val lastCell = to / wordSize
    val left = geometry.cellX(firstCell) - 2
    val right = geometry.cellX(lastCell) + geometry.cellWidth + 2
    drawRect(color, topLeft = Offset(left, 0f), size = Size(right - left, size.height))
    val asciiLeft = geometry.asciiCharX(from)
    drawRect(color, topLeft = Offset(asciiLeft, 0f), size = Size(geometry.asciiCharX(to + 1) - asciiLeft, size.height))
}

/** The cursor's box, solid in the column being typed in, with a line under the digit being typed */
private fun DrawScope.drawCursor(byteIndex: Int, geometry: HexGeometry, state: HexViewState, colors: TmColors) {
    val wordSize = state.display.wordSize
    val cell = byteIndex / wordSize
    val stroke = Stroke(width = 1.5f)
    val accent = colors.accent.color
    val hexLeft = geometry.cellX(cell) - 2
    val hexWidth = geometry.cellWidth + 4
    val asciiLeft = geometry.asciiCharX(byteIndex)
    val asciiWidth = geometry.asciiCharX(byteIndex + 1) - asciiLeft

    if (state.asciiFocus) {
        drawRect(accent.copy(alpha = 0.35f), topLeft = Offset(asciiLeft, 1f), size = Size(asciiWidth, size.height - 2))
        drawRect(accent, topLeft = Offset(hexLeft, 1f), size = Size(hexWidth, size.height - 2), style = stroke)
    } else {
        drawRect(accent.copy(alpha = 0.35f), topLeft = Offset(hexLeft, 1f), size = Size(hexWidth, size.height - 2))
        drawRect(accent, topLeft = Offset(asciiLeft, 1f), size = Size(asciiWidth, size.height - 2), style = stroke)
        if (state.display.editable) {
            val digitWidth = geometry.cellWidth / 2
            val x = geometry.cellX(cell) + state.nibble * digitWidth
            drawRect(accent, topLeft = Offset(x, size.height - 3), size = Size(digitWidth, 2f))
        }
    }
}

@Composable
private fun GapRow(
    range: AddressRange,
    geometry: HexGeometry,
    style: TextStyle,
    colors: TmColors,
    measurer: androidx.compose.ui.text.TextMeasurer,
    modifier: Modifier
) {
    val layout = remember(range, style, colors) {
        measurer.measure("··· nothing at ${range.start.toHexAddress()} to ${range.last.toHexAddress()} (${byteCount(range.size)}) ···", style.copy(color = colors.textFaint))
    }
    Canvas(modifier) {
        drawLine(colors.hairline, Offset(0f, size.height / 2), Offset(geometry.hexX - 8, size.height / 2))
        drawText(layout, topLeft = Offset(geometry.hexX, (size.height - layout.size.height) / 2))
    }
}

/** 512 B, 12 KB, 1.5 MB, 3.2 GB */
internal fun byteCount(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024L * 1024 -> "${oneDecimal(bytes / 1024.0)} KB"
    bytes < 1024L * 1024 * 1024 -> "${oneDecimal(bytes / 1024.0 / 1024)} MB"
    else -> "${oneDecimal(bytes / 1024.0 / 1024 / 1024)} GB"
}

private fun oneDecimal(value: Double): String {
    val tenths = kotlin.math.round(value * 10).toLong()
    return if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
}

private const val TAG = "TmHexView"

/** Two presses on one byte within this long are a double-click */
private const val DOUBLE_CLICK_MS = 400L
