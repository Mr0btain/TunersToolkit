package org.mr0btain.tunerstoolkit.ui.kit.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
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
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.mr0btain.applogger.AppLogger
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationValues
import org.mr0btain.tunerstoolkit.core.calibration.CellSelection
import org.mr0btain.tunerstoolkit.core.calibration.MapEdit
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.calibration.Tsv
import org.mr0btain.tunerstoolkit.core.calibration.columns
import org.mr0btain.tunerstoolkit.core.calibration.formatValue
import org.mr0btain.tunerstoolkit.core.calibration.indexAt
import org.mr0btain.tunerstoolkit.core.calibration.rows
import org.mr0btain.tunerstoolkit.core.calibration.step
import org.mr0btain.tunerstoolkit.ui.kit.hex.readText
import org.mr0btain.tunerstoolkit.ui.kit.hex.writeText
import org.mr0btain.uikit.theme.TmColors
import org.mr0btain.uikit.theme.TmTheme
import kotlin.math.max
import kotlin.math.min

private const val TAG = "TmMapTable"

/**
 * Where the cursor and selection are in a [TmMapTable], and the number being typed. Positions are (x, y): x across,
 * y down. [outer] picks the slice of a cube shown (its z, 4th and 5th positions)
 */
@Stable
class MapTableState {
    var cursor by mutableStateOf(0 to 0)
    var anchor by mutableStateOf<Pair<Int, Int>?>(null)
    var outer by mutableStateOf<List<Int>>(emptyList())

    /** The number being typed into the selection, null when not typing */
    var editText by mutableStateOf<String?>(null)
    val listState = LazyListState()

    val selection: CellSelection get() = CellSelection.between(anchor ?: cursor, cursor, outer)

    fun moveTo(x: Int, y: Int, extend: Boolean = false) {
        if (extend) {
            if (anchor == null) anchor = cursor
        } else {
            anchor = null
        }
        cursor = x to y
    }

    fun select(selection: CellSelection) {
        anchor = selection.x.first to selection.y.first
        cursor = selection.x.last to selection.y.last
    }

    /** Keeps the cursor inside [values], whose size may have changed */
    fun fit(values: CalibrationValues) {
        fun inside(at: Pair<Int, Int>) = min(at.first, values.columns - 1) to min(at.second, values.rows - 1)
        if (inside(cursor) != cursor) cursor = inside(cursor)
        anchor?.let { if (inside(it) != it) anchor = inside(it) }
        val outerCount = max(0, values.dimensions.size - 2)
        if (outer.size != outerCount) outer = List(outerCount) { 0 }
    }
}

/** Which numbers a [TmMapTable] shows */
enum class MapTableMode(val label: String) {
    VALUES("This"),

    /** The values of the object it's compared with */
    OTHER("Other"),

    /** This minus the other */
    DIFFERENCE("Difference")
}

/**
 * An editable grid of an object's values: x across with its axis points as column headers, y down with its points as
 * row headers, one 2D slice of a cube at a time. Cells are tinted from low to high (or by sign, for a difference),
 * and cells that differ from [original] are marked in the warning tone.
 *
 * Click a cell to put the cursor there; shift-click or drag to select. Keys: arrows (with shift to select), Home/End,
 * Ctrl+A for everything; type a number and Enter to set the selection to it (Esc cancels, F2 starts from the current
 * value); + and - step the selection by one stored unit; Ctrl+C copies the selection for a spreadsheet and Ctrl+V
 * pastes one (a single value fills the selection); Ctrl+Z/Y undo and redo. Ctrl is Cmd on a Mac.
 *
 * It never writes: changes go to [onChange] as new physical values by index, so the screen can write them through the
 * codec (with limits and as one undo step).
 * @param compareWith Values of the same object from another image, for [mode] OTHER and DIFFERENCE (which are read only)
 */
@Composable
fun TmMapTable(
    values: CalibrationValues,
    state: MapTableState,
    modifier: Modifier = Modifier,
    original: CalibrationValues? = null,
    compareWith: CalibrationValues? = null,
    mode: MapTableMode = MapTableMode.VALUES,
    readOnly: Boolean = false,
    onChange: (Map<Int, Double>) -> Unit = {},
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onMessage: (String) -> Unit = {}
) {
    val colors = TmTheme.colors
    val style = TmTheme.typography.numeric.copy(fontSize = 12.sp, lineHeight = 16.sp, color = colors.text)
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val density = LocalDensity.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    state.fit(values)

    val other = compareWith?.takeIf { it.size == values.size }
    val shown = remember(values, other, mode) { shownNumbers(values, other, mode) }
    val editable = !readOnly && mode == MapTableMode.VALUES && !values.obj.readOnly
    val texts = remember(values, other, shown, mode) { cellTexts(values, other, shown, mode) }
    val xHeaders = remember(values) { axisTexts(values, 0) }
    val yHeaders = remember(values) { axisTexts(values, 1) }

    val padX = with(density) { 8.dp.toPx() }
    val charWidth = remember(style) { measurer.measure("0", style).size.width.toFloat() }
    val lineHeight = remember(style) { measurer.measure("0", style).size.height.toFloat() }
    val widestCell = remember(texts, xHeaders) { (texts + xHeaders).maxOfOrNull { it.length } ?: 1 }
    val cellWidth = max(with(density) { 52.dp.toPx() }, widestCell * charWidth + padX * 2)
    val headerWidth = if (values.dimensions.size >= 2 || values.obj.kind == ObjectKind.CURVE) {
        max(with(density) { 52.dp.toPx() }, ((yHeaders.maxOfOrNull { it.length } ?: 1) * charWidth + padX * 2))
    } else {
        0f
    }
    val rowHeightPx = lineHeight + with(density) { 10.dp.toPx() }
    val rowHeight = with(density) { rowHeightPx.toDp() }
    val totalWidth = with(density) { (headerWidth + cellWidth * values.columns + 2).toDp() }
    val outer = state.outer
    val range = remember(shown, outer, values) { valueRange(values, shown, outer) }
    val geometry = TableGeometry(headerWidth, cellWidth, padX, lineHeight)

    fun cellAt(x: Float): Int? {
        if (x < headerWidth) return null
        val column = ((x - headerWidth) / cellWidth).toInt()
        return column.takeIf { it in 0 until values.columns }
    }

    fun rowAt(y: Float): Int? = state.listState.layoutInfo.visibleItemsInfo.firstOrNull { y >= it.offset && y < it.offset + it.size }?.index

    fun commitEdit() {
        val text = state.editText ?: return
        state.editText = null
        val number = text.replace(',', '.').toDoubleOrNull()
        val verbal = values.obj.conversion.rawForText(text)?.let(values.obj.conversion::toPhysical)
        val value = number ?: verbal ?: return onMessage("\"$text\" isn't a number")
        onChange(MapEdit.apply(values, state.selection, org.mr0btain.tunerstoolkit.core.calibration.MapOperation.SET, value))
    }

    fun nudge(direction: Int) {
        val cells = state.selection.indices(values)
        onChange(cells.associateWith { values.physical[it] + direction * values.step(it) })
    }

    fun copy() {
        val text = Tsv.format(selectionTexts(values, shown, state.selection))
        scope.launch {
            try {
                clipboard.writeText(text)
                onMessage("Copied ${state.selection.width * state.selection.height} values")
            } catch (e: Exception) {
                AppLogger.w("$TAG:Copy", "Couldn't copy", e)
                onMessage("Couldn't copy to the clipboard")
            }
        }
    }

    fun paste() {
        scope.launch {
            val text = try {
                clipboard.readText()
            } catch (e: Exception) {
                AppLogger.w("$TAG:Paste", "Couldn't read the clipboard", e)
                null
            }
            val grid = Tsv.parse(text.orEmpty())
            if (grid.isEmpty() || grid.all { row -> row.all { it == null } }) return@launch onMessage("The clipboard has no numbers")
            onChange(MapEdit.paste(values, state.selection, grid))
        }
    }

    fun handleKey(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        val command = event.isCtrlPressed || event.isMetaPressed
        val shift = event.isShiftPressed
        val (x, y) = state.cursor
        fun move(toX: Int, toY: Int) {
            state.editText = null
            state.moveTo(toX.coerceIn(0, values.columns - 1), toY.coerceIn(0, values.rows - 1), extend = shift)
            scope.launch {
                val first = state.listState.firstVisibleItemIndex
                val visible = state.listState.layoutInfo.visibleItemsInfo.size
                val row = state.cursor.second
                if (row < first || row >= first + visible - 1) state.listState.scrollToItem(max(0, row - visible / 2))
            }
        }
        if (command) {
            when (event.key) {
                Key.C -> copy()
                Key.V -> if (editable) paste()
                Key.A -> state.select(CellSelection.all(values, state.outer))
                Key.Z -> if (shift) onRedo() else onUndo()
                Key.Y -> onRedo()
                else -> return false
            }
            return true
        }
        val editing = state.editText
        when (event.key) {
            Key.DirectionLeft -> move(x - 1, y)
            Key.DirectionRight -> move(x + 1, y)
            Key.DirectionUp -> move(x, y - 1)
            Key.DirectionDown -> move(x, y + 1)
            Key.MoveHome -> move(0, y)
            Key.MoveEnd -> move(values.columns - 1, y)
            Key.Escape -> if (editing != null) state.editText = null else state.anchor = null
            Key.Enter, Key.NumPadEnter -> if (editing != null) commitEdit() else if (editable) state.editText = numberText(values, shown, x, y, state.outer)
            Key.F2 -> if (editable) state.editText = numberText(values, shown, x, y, state.outer)
            Key.Backspace -> if (editing != null) state.editText = editing.dropLast(1) else return false
            else -> {
                if (!editable) return false
                val char = event.utf16CodePoint.toChar()
                when {
                    editing != null && (char.isDigit() || char in ".,-eE") -> state.editText = editing + char
                    editing == null && (char.isDigit() || char == '.') -> state.editText = char.toString()
                    editing == null && (char == '+' || char == '=') -> nudge(1)
                    editing == null && char == '-' -> nudge(-1)
                    else -> return false
                }
            }
        }
        return true
    }

    Box(
        modifier
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent(::handleKey)
            .horizontalScroll(rememberScrollState())
    ) {
        Column(Modifier.width(totalWidth)) {
            if (values.dimensions.isNotEmpty()) {
                val headers = remember(xHeaders, style, colors) { xHeaders.map { measurer.measure(it, style.copy(color = colors.textMuted)) } }
                val corner = remember(values, style, colors) { measurer.measure(cornerText(values), style.copy(color = colors.textFaint)) }
                Canvas(Modifier.width(totalWidth).height(rowHeight)) {
                    drawRect(colors.surfaceRaised, size = size)
                    drawText(corner, topLeft = Offset(padX, (size.height - lineHeight) / 2))
                    headers.forEachIndexed { column, layout ->
                        val selected = column in state.selection.x
                        if (selected) drawRect(colors.accent.soft, topLeft = Offset(geometry.cellX(column), 0f), size = Size(cellWidth, size.height))
                        drawText(layout, topLeft = Offset(geometry.cellX(column) + cellWidth - padX - layout.size.width, (size.height - lineHeight) / 2))
                    }
                    drawRect(colors.hairline, topLeft = Offset(0f, size.height - 1f), size = Size(size.width, 1f))
                }
            }
            LazyColumn(
                state = state.listState,
                modifier = Modifier
                    .width(totalWidth)
                    .fillMaxHeight()
                    .pointerInput(values, cellWidth, headerWidth) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            focus.requestFocus()
                            val shift = currentEvent.keyboardModifiers.isShiftPressed
                            val column = cellAt(down.position.x)
                            val row = rowAt(down.position.y)
                            if (column != null && row != null) {
                                if (state.editText != null) commitEdit()
                                state.moveTo(column, row, extend = shift)
                            }
                            if (down.type != PointerType.Mouse) return@awaitEachGesture
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                val c = cellAt(change.position.x) ?: continue
                                val r = rowAt(change.position.y) ?: continue
                                state.moveTo(c, r, extend = true)
                            }
                        }
                    }
            ) {
                items(values.rows) { row ->
                    TableRow(
                        values = values,
                        row = row,
                        state = state,
                        shown = shown,
                        texts = texts,
                        yHeader = yHeaders.getOrNull(row),
                        original = original,
                        mode = mode,
                        range = range,
                        geometry = geometry,
                        style = style,
                        colors = colors,
                        measurer = measurer,
                        modifier = Modifier.width(totalWidth).height(rowHeight)
                    )
                }
            }
        }
    }
}

/** Where columns sit along a row, in pixels */
private class TableGeometry(val headerWidth: Float, val cellWidth: Float, val padX: Float, val lineHeight: Float) {
    fun cellX(column: Int): Float = headerWidth + column * cellWidth
}

@Composable
private fun TableRow(
    values: CalibrationValues,
    row: Int,
    state: MapTableState,
    shown: DoubleArray,
    texts: List<String>,
    yHeader: String?,
    original: CalibrationValues?,
    mode: MapTableMode,
    range: ClosedFloatingPointRange<Double>,
    geometry: TableGeometry,
    style: TextStyle,
    colors: TmColors,
    measurer: TextMeasurer,
    modifier: Modifier
) {
    val outer = state.outer
    val indices = remember(values, row, outer) { IntArray(values.columns) { values.indexAt(it, row, outer) } }
    val changed = remember(values, original, row, outer) {
        BooleanArray(values.columns) { original != null && original.size == values.size && original.raw[indices[it]] != values.raw[indices[it]] }
    }
    val layouts: List<TextLayoutResult> = remember(texts, indices, changed, style, colors) {
        indices.mapIndexed { column, index ->
            measurer.measure(texts[index], style.copy(color = if (changed[column]) colors.warning.color else colors.text))
        }
    }
    val header: TextLayoutResult? = remember(yHeader, style, colors) { yHeader?.let { measurer.measure(it, style.copy(color = colors.textMuted)) } }
    val editText = state.editText

    Canvas(modifier) {
        val selection = state.selection
        val textTop = (size.height - geometry.lineHeight) / 2
        if (header != null) {
            drawRect(if (row in selection.y) colors.accent.soft else colors.surfaceRaised, size = Size(geometry.headerWidth, size.height))
            drawText(header, topLeft = Offset(geometry.headerWidth - geometry.padX - header.size.width, textTop))
        }
        for (column in indices.indices) {
            val index = indices[column]
            val left = geometry.cellX(column)
            val cell = Size(geometry.cellWidth, size.height)
            drawRect(cellColor(shown[index], range, mode, colors), topLeft = Offset(left, 0f), size = cell)
            val inSelection = column in selection.x && row in selection.y
            if (inSelection) drawRect(colors.accent.soft, topLeft = Offset(left, 0f), size = cell)
            if (changed[column]) drawCorner(left + geometry.cellWidth, colors.warning.color)
            val isCursor = state.cursor == column to row
            if (isCursor && editText != null) {
                drawRect(colors.surface, topLeft = Offset(left, 0f), size = cell)
                val typed = measurer.measure("$editText▏", style.copy(color = colors.accent.color))
                drawText(typed, topLeft = Offset(left + geometry.cellWidth - geometry.padX - typed.size.width, textTop))
            } else {
                val layout = layouts[column]
                drawText(layout, topLeft = Offset(left + geometry.cellWidth - geometry.padX - layout.size.width, textTop))
            }
            if (isCursor) drawRect(colors.accent.color, topLeft = Offset(left + 1f, 1f), size = Size(geometry.cellWidth - 2f, size.height - 2f), style = Stroke(2f))
        }
        drawRect(colors.hairline, topLeft = Offset(0f, size.height - 1f), size = Size(size.width, 1f))
    }
}

/** A small triangle in a cell's top right corner */
private fun DrawScope.drawCorner(right: Float, color: Color) {
    val side = 7f
    val path = Path().apply {
        moveTo(right - side, 0f)
        lineTo(right, 0f)
        lineTo(right, side)
        close()
    }
    drawPath(path, color)
}

/** A cell's tint: low to high across the palette, or by sign for a difference */
private fun cellColor(value: Double, range: ClosedFloatingPointRange<Double>, mode: MapTableMode, colors: TmColors): Color {
    if (value.isNaN()) return Color.Transparent
    if (mode == MapTableMode.DIFFERENCE) {
        return when {
            value > 0 -> colors.success.color.copy(alpha = 0.22f)
            value < 0 -> colors.danger.color.copy(alpha = 0.22f)
            else -> Color.Transparent
        }
    }
    val span = range.endInclusive - range.start
    val t = if (span <= 0) 0.5f else ((value - range.start) / span).toFloat().coerceIn(0f, 1f)
    return lerp(colors.accent.color, colors.danger.color, t).copy(alpha = 0.10f + 0.18f * t)
}

/** The numbers shown: the values, the other image's, or the difference */
private fun shownNumbers(values: CalibrationValues, other: CalibrationValues?, mode: MapTableMode): DoubleArray = when {
    other == null || mode == MapTableMode.VALUES -> values.physical
    mode == MapTableMode.OTHER -> other.physical
    else -> DoubleArray(values.size) { values.physical[it] - other.physical[it] }
}

private fun cellTexts(values: CalibrationValues, other: CalibrationValues?, shown: DoubleArray, mode: MapTableMode): List<String> = List(values.size) { i ->
    val number = shown[i]
    when {
        // Cells that didn't change stay blank, so the ones that did stand out
        mode == MapTableMode.DIFFERENCE && other != null -> if (values.raw[i] == other.raw[i]) "" else (if (number > 0) "+" else "") + formatValue(number, values.obj.format)
        mode == MapTableMode.OTHER && other != null -> other.textAt(i) ?: formatValue(number, values.obj.format)
        else -> values.textAt(i) ?: formatValue(number, values.obj.format)
    }
}

/** An axis's points as header texts; positions when it has none */
private fun axisTexts(values: CalibrationValues, axis: Int): List<String> {
    val count = values.dimensions.getOrElse(axis) { if (axis == 1) 1 else 0 }
    val points = values.axes.getOrNull(axis) ?: return List(count) { if (values.dimensions.size > axis) it.toString() else "" }
    return List(count) { points.text(it) ?: formatValue(points.physical[it], points.axis.format) }
}

private fun cornerText(values: CalibrationValues): String {
    val y = values.axes.getOrNull(1)?.axis?.unit.orEmpty()
    val x = values.axes.getOrNull(0)?.axis?.unit.orEmpty()
    return listOf(y, x).filter { it.isNotBlank() }.joinToString(" \\ ")
}

/** The lowest and highest number in the slice shown, for the tints */
private fun valueRange(values: CalibrationValues, shown: DoubleArray, outer: List<Int>): ClosedFloatingPointRange<Double> {
    var low = Double.POSITIVE_INFINITY
    var high = Double.NEGATIVE_INFINITY
    for (y in 0 until values.rows) for (x in 0 until values.columns) {
        val value = shown[values.indexAt(x, y, outer)]
        if (value.isNaN()) continue
        low = min(low, value)
        high = max(high, value)
    }
    return if (low > high) 0.0..0.0 else low..high
}

private fun numberText(values: CalibrationValues, shown: DoubleArray, x: Int, y: Int, outer: List<Int>): String =
    org.mr0btain.tunerstoolkit.core.calibration.plainNumber(shown[values.indexAt(x, y, outer)])

private fun selectionTexts(values: CalibrationValues, shown: DoubleArray, selection: CellSelection): List<List<String>> =
    selection.y.map { y -> selection.x.map { x -> org.mr0btain.tunerstoolkit.core.calibration.plainNumber(shown[values.indexAt(x, y, selection.outer)]) } }
