package org.mr0btain.tunerstoolkit.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationObject
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationValues
import org.mr0btain.tunerstoolkit.core.calibration.CellRef
import org.mr0btain.tunerstoolkit.core.calibration.PartRole
import org.mr0btain.tunerstoolkit.core.calibration.cellAt
import org.mr0btain.tunerstoolkit.core.calibration.formatValue
import org.mr0btain.tunerstoolkit.core.calibration.position
import org.mr0btain.tunerstoolkit.core.error.getOrNull
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionHandler
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionOnImage
import org.mr0btain.tunerstoolkit.ui.kit.hex.HexMark
import org.mr0btain.tunerstoolkit.ui.kit.hex.HexMarkRole
import org.mr0btain.tunerstoolkit.ui.kit.hex.HexMarks
import org.mr0btain.uikit.theme.TmTheme

/** The definition's objects as marks for the hex view, [selected] outlined */
internal fun objectMarks(onImage: DefinitionOnImage, selected: String?): HexMarks = HexMarks { range ->
    onImage.build.index.overlapping(range).map { part ->
        val role = when (part.part.role) {
            PartRole.VALUES -> HexMarkRole.VALUES
            PartRole.AXIS_POINTS -> HexMarkRole.AXIS
            PartRole.RECORD -> HexMarkRole.RECORD
        }
        HexMark(part.range, part.obj.name, role, part.obj.name == selected)
    }
}

/** Which object the cursor is in, which of its numbers, and that number's physical value */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ObjectAtCursor(obj: CalibrationObject, definitions: DefinitionHandler, cursor: Long, version: Long) {
    val colors = TmTheme.colors
    val values = remember(obj, version) { definitions.decode(obj)?.getOrNull() }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.md)) {
        Text(obj.name, style = TmTheme.typography.subtitle, color = colors.text)
        Text(obj.kind.name.lowercase().replace('_', ' '), style = TmTheme.typography.caption, color = colors.textMuted)
        if (obj.description.isNotBlank()) Text(obj.description, style = TmTheme.typography.caption, color = colors.textMuted)
        values?.let { describeCell(it, cursor) }?.let { Text(it, style = TmTheme.typography.numeric, color = colors.accent.color) }
    }
}

/**
 * The hex view's tooltip for [address]: the name of each object there, with its description and the number at
 * [address] ("[3, 5] = 21.75 deg"), or null when it's in none
 */
internal fun hoverTip(onImage: DefinitionOnImage, definitions: DefinitionHandler, address: Long): String? {
    val objects = onImage.build.index.at(address).map { it.obj }.distinctBy { it.name }
    if (objects.isEmpty()) return null
    return objects.joinToString("\n\n") { obj ->
        listOfNotNull(
            obj.name,
            obj.description.takeIf { it.isNotBlank() && it != obj.name },
            definitions.decode(obj)?.getOrNull()?.let { describeCell(it, address) }
        ).joinToString("\n")
    }
}

/** "[3, 5] = 21.75 deg" for the number at [address], or the axis point it is */
internal fun describeCell(values: CalibrationValues, address: Long): String? {
    val obj = values.obj
    return when (val cell = values.cellAt(address)) {
        is CellRef.Value -> {
            val at = values.position(cell.index)
            val where = if (at.isEmpty()) "" else at.joinToString(", ", "[", "] ")
            "$where= ${values.textAt(cell.index) ?: formatValue(values.physical[cell.index], obj.format)} ${obj.unit}".trim()
        }
        is CellRef.AxisPoint -> {
            val axis = values.axes[cell.axis]
            "${AXIS_NAMES.getOrElse(cell.axis) { "" }} axis point ${cell.index + 1} = ${axis.text(cell.index) ?: formatValue(axis.physical[cell.index], axis.axis.format)} ${axis.axis.unit}".trim()
        }
        null -> null
    }
}

internal val AXIS_NAMES = listOf("X", "Y", "Z", "4th", "5th")
