package org.mr0btain.tunerstoolkit.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.mr0btain.tunerstoolkit.core.calibration.DataType
import org.mr0btain.tunerstoolkit.core.calibration.ManualAxis
import org.mr0btain.tunerstoolkit.core.calibration.ManualMap
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.calibration.ValueOrder
import org.mr0btain.tunerstoolkit.core.calibration.plainNumber
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.parseAddress
import org.mr0btain.tunerstoolkit.core.image.toHexAddress
import org.mr0btain.uikit.buttons.TmButtonRow
import org.mr0btain.uikit.containers.TmDialog
import org.mr0btain.uikit.feedback.TmNotice
import org.mr0btain.uikit.feedback.TmNoticeKind
import org.mr0btain.uikit.foundation.TmControlSize
import org.mr0btain.uikit.inputs.TmTextField
import org.mr0btain.uikit.selection.TmDropdown
import org.mr0btain.uikit.selection.TmSegmented
import org.mr0btain.uikit.theme.TmTheme

/** The form's text, kept as typed so half typed numbers stay put */
private class AxisForm(axis: ManualAxis) {
    var source by mutableStateOf(axis.source)
    var from by mutableStateOf(plainNumber(axis.from))
    var to by mutableStateOf(plainNumber(axis.to))
    var address by mutableStateOf(axis.address.toHexAddress())
    var type by mutableStateOf(axis.type)
    var byteOrder by mutableStateOf(axis.byteOrder)
    var factor by mutableStateOf(plainNumber(axis.factor))
    var offset by mutableStateOf(plainNumber(axis.offset))
    var unit by mutableStateOf(axis.unit)

    fun axis(): ManualAxis? = ManualAxis(
        source = source,
        from = number(from) ?: return null,
        to = number(to) ?: return null,
        address = if (source == ManualAxis.Source.MEMORY) parseAddress(address) ?: return null else 0,
        type = type,
        byteOrder = byteOrder,
        factor = number(factor) ?: return null,
        offset = number(offset) ?: return null,
        unit = unit
    )
}

private fun number(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

/**
 * Making a map by hand, or changing one made that way: where it is, how it's stored and scaled, and its axes
 * @param initial The map being changed, or a new one's starting point
 * @param taken Names already used, which a new map can't have
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ManualMapDialog(initial: ManualMap, editing: Boolean, taken: Set<String>, onSave: (ManualMap) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var description by remember { mutableStateOf(initial.description) }
    var kind by remember { mutableStateOf(initial.kind) }
    var address by remember { mutableStateOf(initial.address.toHexAddress()) }
    var type by remember { mutableStateOf(initial.type) }
    var byteOrder by remember { mutableStateOf(initial.byteOrder) }
    var columns by remember { mutableStateOf(initial.columns.toString()) }
    var rows by remember { mutableStateOf(initial.rows.toString()) }
    var order by remember { mutableStateOf(initial.order) }
    var factor by remember { mutableStateOf(plainNumber(initial.factor)) }
    var offset by remember { mutableStateOf(plainNumber(initial.offset)) }
    var unit by remember { mutableStateOf(initial.unit) }
    var decimals by remember { mutableStateOf(initial.decimals.toString()) }
    var lower by remember { mutableStateOf(initial.lower?.let(::plainNumber).orEmpty()) }
    var upper by remember { mutableStateOf(initial.upper?.let(::plainNumber).orEmpty()) }
    val x = remember { AxisForm(initial.x) }
    val y = remember { AxisForm(initial.y) }

    val axes = when (kind) {
        ObjectKind.CURVE -> listOf("X" to x)
        ObjectKind.MAP -> listOf("X" to x, "Y" to y)
        else -> emptyList()
    }
    val map: ManualMap? = run {
        ManualMap(
            name = name.trim(),
            description = description,
            kind = kind,
            address = parseAddress(address) ?: return@run null,
            type = type,
            byteOrder = byteOrder,
            columns = if (kind == ObjectKind.VALUE) 1 else columns.trim().toIntOrNull() ?: return@run null,
            rows = if (kind == ObjectKind.MAP || kind == ObjectKind.VAL_BLK) rows.trim().toIntOrNull() ?: return@run null else 1,
            order = order,
            factor = number(factor) ?: return@run null,
            offset = number(offset) ?: return@run null,
            unit = unit,
            decimals = decimals.trim().toIntOrNull()?.coerceIn(0, 9) ?: 2,
            lower = if (lower.isBlank()) null else number(lower) ?: return@run null,
            upper = if (upper.isBlank()) null else number(upper) ?: return@run null,
            x = x.axis() ?: return@run null,
            y = y.axis() ?: return@run null
        )
    }
    val problem = when {
        map == null -> "Check the numbers and addresses"
        !editing && map.name in taken -> "There's already something called ${map.name}"
        else -> map.problem()
    }

    TmDialog(
        title = if (editing) "Change ${initial.name}" else "New map",
        description = "Where it is in the image and how to read it. It's saved with the project, next to the definition",
        onDismissRequest = onDismiss,
        actions = { TmButtonRow(confirmText = if (editing) "Save" else "Add it", onConfirm = { map?.let(onSave) }, onCancel = onDismiss, confirmEnabled = problem == null) }
    ) {
        val narrow = Modifier.width(150.dp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm)) {
            TmTextField(value = name, onValueChange = { name = it }, label = "Name", modifier = Modifier.width(220.dp))
            TmTextField(value = description, onValueChange = { description = it }, label = "Description", modifier = Modifier.width(260.dp))
        }
        TmSegmented(options = ManualMap.KINDS, selected = kind, onSelect = { kind = it }, optionLabel = ::kindName, size = TmControlSize.SMALL)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm)) {
            TmTextField(value = address, onValueChange = { address = it }, label = "Address", modifier = narrow, error = if (parseAddress(address) == null) "Not an address" else null)
            TmDropdown(options = DataType.entries, selected = type, onSelect = { type = it }, optionLabel = { it.name }, label = "Stored as", modifier = narrow)
            TmSegmented(options = ByteOrder.entries, selected = byteOrder, onSelect = { byteOrder = it }, optionLabel = { if (it == ByteOrder.LITTLE_ENDIAN) "LSB first" else "MSB first" }, label = "Byte order", size = TmControlSize.SMALL)
        }
        if (kind != ObjectKind.VALUE) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm)) {
                TmTextField(value = columns, onValueChange = { columns = it }, label = "Columns (x)", modifier = Modifier.width(110.dp))
                if (kind == ObjectKind.MAP || kind == ObjectKind.VAL_BLK) {
                    TmTextField(value = rows, onValueChange = { rows = it }, label = "Rows (y)", modifier = Modifier.width(110.dp))
                    TmSegmented(options = ValueOrder.entries, selected = order, onSelect = { order = it }, optionLabel = { if (it == ValueOrder.X_FIRST) "Row by row" else "Column by column" }, label = "Stored", size = TmControlSize.SMALL)
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm)) {
            TmTextField(value = factor, onValueChange = { factor = it }, label = "Factor", modifier = Modifier.width(110.dp))
            TmTextField(value = offset, onValueChange = { offset = it }, label = "Offset", modifier = Modifier.width(110.dp))
            TmTextField(value = unit, onValueChange = { unit = it }, label = "Unit", modifier = Modifier.width(90.dp))
            TmTextField(value = decimals, onValueChange = { decimals = it }, label = "Decimals", modifier = Modifier.width(90.dp))
            TmTextField(value = lower, onValueChange = { lower = it }, label = "Lowest", modifier = Modifier.width(100.dp))
            TmTextField(value = upper, onValueChange = { upper = it }, label = "Highest", modifier = Modifier.width(100.dp))
        }
        Text("Physical value = stored × factor + offset", style = TmTheme.typography.caption, color = TmTheme.colors.textMuted)
        axes.forEach { (letter, form) -> AxisFields(letter, form) }
        problem?.let { TmNotice(kind = TmNoticeKind.INFO, text = it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AxisFields(letter: String, form: AxisForm) {
    Text("$letter axis", style = TmTheme.typography.subtitle, color = TmTheme.colors.text)
    TmSegmented(options = ManualAxis.Source.entries, selected = form.source, onSelect = { form.source = it }, optionLabel = { it.label }, size = TmControlSize.SMALL)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm)) {
        when (form.source) {
            ManualAxis.Source.POSITIONS -> Unit
            ManualAxis.Source.EVEN -> {
                TmTextField(value = form.from, onValueChange = { form.from = it }, label = "First", modifier = Modifier.width(110.dp))
                TmTextField(value = form.to, onValueChange = { form.to = it }, label = "Last", modifier = Modifier.width(110.dp))
            }
            ManualAxis.Source.MEMORY -> {
                TmTextField(value = form.address, onValueChange = { form.address = it }, label = "Address", modifier = Modifier.width(150.dp))
                TmDropdown(options = DataType.entries, selected = form.type, onSelect = { form.type = it }, optionLabel = { it.name }, label = "Stored as", modifier = Modifier.width(150.dp))
                TmSegmented(options = ByteOrder.entries, selected = form.byteOrder, onSelect = { form.byteOrder = it }, optionLabel = { if (it == ByteOrder.LITTLE_ENDIAN) "LSB first" else "MSB first" }, size = TmControlSize.SMALL)
                TmTextField(value = form.factor, onValueChange = { form.factor = it }, label = "Factor", modifier = Modifier.width(100.dp))
                TmTextField(value = form.offset, onValueChange = { form.offset = it }, label = "Offset", modifier = Modifier.width(100.dp))
            }
        }
        if (form.source != ManualAxis.Source.POSITIONS) TmTextField(value = form.unit, onValueChange = { form.unit = it }, label = "Unit", modifier = Modifier.width(90.dp))
    }
}
