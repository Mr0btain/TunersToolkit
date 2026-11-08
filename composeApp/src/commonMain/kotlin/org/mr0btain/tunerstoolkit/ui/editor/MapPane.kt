package org.mr0btain.tunerstoolkit.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.mr0btain.tunerstoolkit.core.calibration.AxisSource
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationObject
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationValues
import org.mr0btain.tunerstoolkit.core.calibration.Conversion
import org.mr0btain.tunerstoolkit.core.calibration.MapEdit
import org.mr0btain.tunerstoolkit.core.calibration.MapOperation
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.calibration.formatValue
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.toHexAddress
import org.mr0btain.tunerstoolkit.ui.kit.map.MapTableMode
import org.mr0btain.tunerstoolkit.ui.kit.map.TmMapChart2D
import org.mr0btain.tunerstoolkit.ui.kit.map.TmMapChart3D
import org.mr0btain.tunerstoolkit.ui.kit.map.TmMapTable
import org.mr0btain.uikit.buttons.TmButton
import org.mr0btain.uikit.buttons.TmButtonVariant
import org.mr0btain.uikit.buttons.TmIconButton
import org.mr0btain.uikit.containers.TmPaneHeader
import org.mr0btain.uikit.data.TmKeyValueRow
import org.mr0btain.uikit.feedback.TmNotice
import org.mr0btain.uikit.feedback.TmNoticeKind
import org.mr0btain.uikit.foundation.TmControlSize
import org.mr0btain.uikit.foundation.TmSurface
import org.mr0btain.uikit.foundation.TmSurfaceLevel
import org.mr0btain.uikit.inputs.TmTextField
import org.mr0btain.uikit.selection.TmDropdown
import org.mr0btain.uikit.selection.TmSegmented
import org.mr0btain.uikit.theme.TmTheme
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.tt_cpu
import tunerstoolkit.composeapp.generated.resources.tt_info
import tunerstoolkit.composeapp.generated.resources.tt_x

/**
 * One open map: its values as a table, chart or surface, the selection operations, and its details
 * @param values Read from the image as it is now, or why they couldn't be
 * @param original The same object read from the image as it was opened, for marking changes
 * @param onChange Writes new physical values by index
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MapPane(
    obj: CalibrationObject,
    open: OpenMap,
    values: TkResult<CalibrationValues, *>?,
    original: CalibrationValues?,
    onChange: (CalibrationValues, Map<Int, Double>) -> Unit,
    onWriteText: (CalibrationValues, String) -> Unit,
    onShowInHex: () -> Unit,
    onClose: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TmTheme.spacing
    var details by rememberSaveable(obj.name) { mutableStateOf(false) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        TmPaneHeader(
            title = obj.displayName ?: obj.name,
            subtitle = listOf(obj.description, kindName(obj.kind), obj.unit, obj.address.toHexAddress()).filter { it.isNotBlank() }.joinToString(" · "),
            padded = false,
            actions = {
                TmIconButton(icon = Res.drawable.tt_info, contentDescription = if (details) "Hide details" else "Show details", onClick = { details = !details })
                TmIconButton(icon = Res.drawable.tt_cpu, contentDescription = "Show in the hex view", onClick = onShowInHex)
                TmIconButton(icon = Res.drawable.tt_x, contentDescription = "Close ${obj.name}", onClick = onClose)
            }
        )

        val read = when (values) {
            is TkResult.Success -> values.data
            is TkResult.Failure -> {
                TmNotice(kind = TmNoticeKind.ERROR, title = "${obj.name} can't be read from this image", text = values.error.message)
                null
            }
            null -> null
        }
        if (details) Details(obj, read)
        if (read == null) return@Column
        open.table.fit(read)

        if (obj.kind == ObjectKind.ASCII) {
            AsciiEditor(read, onWriteText)
            return@Column
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm), itemVerticalAlignment = Alignment.Bottom) {
            val views = if (read.dimensions.size >= 2) MapView.entries else listOf(MapView.TABLE, MapView.CHART)
            if (read.dimensions.isNotEmpty()) {
                TmSegmented(options = views, selected = open.view.takeIf { it in views } ?: MapView.TABLE, onSelect = { open.view = it }, optionLabel = { it.label }, size = TmControlSize.SMALL)
            }
            SliceSelectors(read, open)
        }

        if (open.view == MapView.TABLE || read.dimensions.isEmpty()) {
            Operations(read, open, enabled = !obj.readOnly, onChange = { onChange(read, it) })
        }

        TmSurface(modifier = Modifier.fillMaxWidth().weight(1f), level = TmSurfaceLevel.SUNKEN) {
            when (open.view.takeIf { read.dimensions.isNotEmpty() } ?: MapView.TABLE) {
                MapView.TABLE -> TmMapTable(
                    values = read,
                    state = open.table,
                    original = original,
                    mode = MapTableMode.VALUES,
                    onChange = { onChange(read, it) },
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onMessage = onMessage,
                    modifier = Modifier.fillMaxSize().padding(spacing.xs)
                )
                MapView.CHART -> TmMapChart2D(
                    values = read,
                    original = original,
                    outer = open.table.outer,
                    highlightRow = if (read.dimensions.size >= 2) open.table.cursor.second else null,
                    modifier = Modifier.fillMaxSize().padding(spacing.md)
                )
                MapView.SURFACE -> TmMapChart3D(values = read, outer = open.table.outer, modifier = Modifier.fillMaxSize().padding(spacing.md))
            }
        }
        SelectionLine(read, open)
    }
}

/** Pickers for which slice of a cube is shown: one per axis past y */
@Composable
private fun SliceSelectors(values: CalibrationValues, open: OpenMap) {
    val outer = open.table.outer
    for (d in 2 until values.dimensions.size) {
        val axis = values.axes.getOrNull(d)
        val points = List(values.dimensions[d]) { i -> axis?.let { it.text(i) ?: formatValue(it.physical[i], it.axis.format) } ?: i.toString() }
        TmDropdown(
            options = points.indices.toList(),
            selected = outer.getOrElse(d - 2) { 0 },
            onSelect = { chosen -> open.table.outer = outer.toMutableList().also { list -> while (list.size <= d - 2) list += 0; list[d - 2] = chosen } },
            optionLabel = { points[it] },
            label = "${AXIS_NAMES.getOrElse(d) { "Axis" }} ${axis?.axis?.input.orEmpty()}".trim(),
            modifier = Modifier.width(160.dp)
        )
    }
}

/** Set, add, multiply, add %, interpolate and smooth, on the selection */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Operations(values: CalibrationValues, open: OpenMap, enabled: Boolean, onChange: (Map<Int, Double>) -> Unit) {
    var amount by rememberSaveable(values.obj.name) { mutableStateOf("") }
    val number = amount.replace(',', '.').toDoubleOrNull()
    fun run(operation: MapOperation) {
        onChange(MapEdit.apply(values, open.table.selection, operation, number ?: 0.0))
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.xs),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        TmTextField(
            value = amount,
            onValueChange = { amount = it },
            placeholder = "Amount",
            modifier = Modifier.width(120.dp),
            error = if (amount.isNotBlank() && number == null) "Not a number" else null
        )
        val withAmount = enabled && number != null
        TmButton(text = "Set", onClick = { run(MapOperation.SET) }, enabled = withAmount, size = TmControlSize.SMALL)
        TmButton(text = "+", onClick = { run(MapOperation.ADD) }, enabled = withAmount, size = TmControlSize.SMALL, variant = TmButtonVariant.SECONDARY)
        TmButton(text = "×", onClick = { run(MapOperation.MULTIPLY) }, enabled = withAmount, size = TmControlSize.SMALL, variant = TmButtonVariant.SECONDARY)
        TmButton(text = "+%", onClick = { run(MapOperation.ADD_PERCENT) }, enabled = withAmount, size = TmControlSize.SMALL, variant = TmButtonVariant.SECONDARY)
        if (values.dimensions.isNotEmpty()) {
            TmButton(text = "Interpolate", onClick = { run(MapOperation.INTERPOLATE) }, enabled = enabled, size = TmControlSize.SMALL, variant = TmButtonVariant.SECONDARY)
            TmButton(text = "Smooth", onClick = { run(MapOperation.SMOOTH) }, enabled = enabled, size = TmControlSize.SMALL, variant = TmButtonVariant.SECONDARY)
        }
    }
}

/** What's selected, its lowest and highest values, and the limits */
@Composable
private fun SelectionLine(values: CalibrationValues, open: OpenMap) {
    val obj = values.obj
    val selection = open.table.selection
    val selected = selection.indices(values).map { values.physical[it] }
    val parts = buildList {
        if (values.dimensions.isNotEmpty()) add("${selection.width} × ${selection.height} selected")
        if (selected.size > 1) add("${formatValue(selected.min(), obj.format)} to ${formatValue(selected.max(), obj.format)}")
        add("limits ${formatValue(obj.lowerLimit, obj.format)} to ${formatValue(obj.upperLimit, obj.format)} ${obj.unit}".trim())
        if (obj.readOnly) add("read only")
    }
    Text(parts.joinToString(" · "), style = TmTheme.typography.caption, color = TmTheme.colors.textMuted)
}

/** Where it is, how it's stored and converted, and its axes */
@Composable
private fun Details(obj: CalibrationObject, values: CalibrationValues?) {
    val storage = obj.storage
    Column {
        TmKeyValueRow(label = "Address", value = obj.address.toHexAddress(), numeric = true)
        values?.ranges?.takeIf { it.isNotEmpty() }?.let { ranges ->
            TmKeyValueRow(label = "Memory", value = ranges.joinToString { it.toString() }, numeric = true)
        }
        storage.values?.let { TmKeyValueRow(label = "Stored as", value = "${it.type.name}, ${if (storage.byteOrder.name.startsWith("BIG")) "MSB first" else "LSB first"}") }
        TmKeyValueRow(label = "Conversion", value = describe(obj.conversion))
        TmKeyValueRow(label = "Limits", value = "${formatValue(obj.lowerLimit, obj.format)} to ${formatValue(obj.upperLimit, obj.format)} ${obj.unit}".trim())
        obj.extendedLimits?.let { TmKeyValueRow(label = "Extended limits", value = "${formatValue(it.start, obj.format)} to ${formatValue(it.endInclusive, obj.format)}") }
        obj.axes.forEachIndexed { i, axis ->
            val source = when (val s = axis.source) {
                AxisSource.Record -> "in the record"
                is AxisSource.Shared -> "shared, ${s.name}"
                is AxisSource.Rescaled -> "rescaled by ${s.name}"
                is AxisSource.Fixed -> "fixed"
                is AxisSource.Curve -> "through the curve ${s.name}"
            }
            val count = values?.axes?.getOrNull(i)?.count?.let { "$it points" } ?: "up to ${axis.maxPoints} points"
            TmKeyValueRow(label = "${AXIS_NAMES.getOrElse(i) { "" }} axis", value = listOfNotNull(axis.input, source, count, axis.unit.takeIf { it.isNotBlank() }).joinToString(", "))
        }
        obj.source?.let { TmKeyValueRow(label = "Defined at", value = "line ${it.line}") }
    }
}

/** An ASCII object's text, edited as a whole: it's padded with zeros to its length when written */
@Composable
private fun AsciiEditor(values: CalibrationValues, onWrite: (CalibrationValues, String) -> Unit) {
    var text by remember(values) { mutableStateOf(values.text.orEmpty()) }
    val length = values.size * (values.slots?.type?.size ?: 1)
    val bytes = text.encodeToByteArray().size
    Box(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm)) {
            TmTextField(
                value = text,
                onValueChange = { text = it },
                label = "Text",
                supportingText = "$bytes of $length bytes",
                error = if (bytes > length) "That's longer than the space for it" else null,
                modifier = Modifier.fillMaxWidth()
            )
            TmButton(
                text = "Write",
                onClick = { onWrite(values, text) },
                enabled = !values.obj.readOnly && bytes <= length && text != values.text,
                size = TmControlSize.SMALL
            )
        }
    }
}

internal fun kindName(kind: ObjectKind): String = when (kind) {
    ObjectKind.VALUE -> "value"
    ObjectKind.CURVE -> "curve"
    ObjectKind.MAP -> "map"
    ObjectKind.CUBOID -> "cuboid"
    ObjectKind.CUBE_4 -> "4D cube"
    ObjectKind.CUBE_5 -> "5D cube"
    ObjectKind.VAL_BLK -> "value block"
    ObjectKind.ASCII -> "text"
    ObjectKind.AXIS -> "shared axis"
}

private fun describe(conversion: Conversion): String = when (conversion) {
    Conversion.Identical -> "none: raw values are physical"
    is Conversion.Linear -> "${formatValue(conversion.factor)} × raw ${if (conversion.offset < 0) "−" else "+"} ${formatValue(kotlin.math.abs(conversion.offset))}"
    is Conversion.Rational -> "rational function"
    is Conversion.Table -> if (conversion.interpolate) "table, interpolated" else "table"
    is Conversion.Verbal -> "texts: " + (conversion.entries.take(4).joinToString { "${formatValue(it.first)} ${it.second}" } + if (conversion.entries.size > 4) ", …" else "")
    is Conversion.Formula -> "formula ${conversion.formula}"
    is Conversion.WithStatus -> describe(conversion.base) + ", with status texts"
}
