package org.mr0btain.tunerstoolkit.ui.finder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.mr0btain.tunerstoolkit.core.calibration.DataType
import org.mr0btain.tunerstoolkit.core.finder.FoundMap
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.toHexAddress
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import org.mr0btain.tunerstoolkit.ui.kit.map.MapTableState
import org.mr0btain.tunerstoolkit.ui.kit.map.TmMapChart2D
import org.mr0btain.tunerstoolkit.ui.kit.map.TmMapTable
import org.mr0btain.uikit.buttons.TmButton
import org.mr0btain.uikit.buttons.TmButtonVariant
import org.mr0btain.uikit.containers.TmListRow
import org.mr0btain.uikit.containers.TmPageHeader
import org.mr0btain.uikit.containers.TmPanel
import org.mr0btain.uikit.feedback.TmBadge
import org.mr0btain.uikit.feedback.TmEmptyState
import org.mr0btain.uikit.feedback.TmProgressBar
import org.mr0btain.uikit.foundation.TmControlSize
import org.mr0btain.uikit.foundation.TmSurface
import org.mr0btain.uikit.foundation.TmSurfaceLevel
import org.mr0btain.uikit.inputs.TmTextField
import org.mr0btain.uikit.selection.TmFilterChip
import org.mr0btain.uikit.selection.TmSegmented
import org.mr0btain.uikit.containers.TmSwitchRow
import org.mr0btain.uikit.theme.TmTheme
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.tt_cpu
import tunerstoolkit.composeapp.generated.resources.tt_plus
import tunerstoolkit.composeapp.generated.resources.tt_scan

/**
 * Looking for maps in an image without a definition: what to look for, the suggestions (surest first), and each one's
 * values to look over before adding it as a map of your own
 */
@Composable
fun FinderScreen(
    documents: DocumentHandler,
    finder: FinderHandler,
    onOpenFiles: () -> Unit,
    onShowInHex: (Long) -> Unit,
    onAdded: (String) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (documents.document == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TmEmptyState(
                title = "No image open",
                message = "Open an image to look for maps in it.",
                icon = Res.drawable.tt_scan,
                actionText = "Open a file",
                onAction = onOpenFiles
            )
        }
        return
    }
    val spacing = TmTheme.spacing
    Column(modifier.fillMaxSize().padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        TmPageHeader(
            title = "Find maps",
            onBack = null,
            subtitle = when {
                finder.running -> "Looking…"
                finder.results == null -> "Looks for maps by their axes and how smoothly their values change"
                else -> "${finder.results!!.size} possible maps. They're guesses: look each one over before adding it"
            }
        )
        Options(finder)
        if (finder.running) TmProgressBar(progress = finder.progress)
        val results = finder.results ?: return@Column
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= 900.dp
            if (results.isEmpty()) {
                Text("Nothing found. Try more types, both byte orders, or a lower confidence.", style = TmTheme.typography.body, color = TmTheme.colors.textMuted)
                return@BoxWithConstraints
            }
            if (wide) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    Suggestions(results, finder, Modifier.width(380.dp).fillMaxHeight())
                    finder.selected?.let { Preview(it, finder, onShowInHex, onAdded, onMessage, Modifier.weight(1f).fillMaxHeight()) }
                }
            } else {
                val selected = finder.selected
                if (selected == null) Suggestions(results, finder, Modifier.fillMaxSize())
                else Column(Modifier.fillMaxSize()) {
                    TmButton(text = "Back to the list", onClick = { finder.selected = null }, variant = TmButtonVariant.GHOST, size = TmControlSize.SMALL)
                    Preview(selected, finder, onShowInHex, onAdded, onMessage, Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Options(finder: FinderHandler) {
    val options = finder.options
    TmPanel(modifier = Modifier.fillMaxWidth(), title = "Look for", icon = Res.drawable.tt_scan, actions = {
        if (finder.running) TmButton(text = "Stop", onClick = finder::cancel, variant = TmButtonVariant.SECONDARY)
        else TmButton(text = "Find maps", onClick = finder::run, leadingIcon = Res.drawable.tt_scan)
    }) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm), itemVerticalAlignment = Alignment.Bottom) {
            TmSegmented(
                options = listOf(ByteOrder.entries, listOf(ByteOrder.LITTLE_ENDIAN), listOf(ByteOrder.BIG_ENDIAN)),
                selected = options.byteOrders,
                onSelect = { finder.options = options.copy(byteOrders = it) },
                optionLabel = { if (it.size == 2) "Both" else if (it.single() == ByteOrder.LITTLE_ENDIAN) "LSB first" else "MSB first" },
                label = "Byte order",
                size = TmControlSize.SMALL
            )
            TmSegmented(
                options = listOf(40, 55, 70, 85),
                selected = options.minConfidence,
                onSelect = { finder.options = options.copy(minConfidence = it) },
                optionLabel = { "$it%" },
                label = "At least",
                size = TmControlSize.SMALL
            )
            TmTextField(
                value = options.minPoints.toString(),
                onValueChange = { text -> text.toIntOrNull()?.takeIf { it in 2..options.maxPoints }?.let { finder.options = options.copy(minPoints = it) } },
                label = "Fewest points", modifier = Modifier.width(110.dp)
            )
            TmTextField(
                value = options.maxPoints.toString(),
                onValueChange = { text -> text.toIntOrNull()?.takeIf { it in options.minPoints..64 }?.let { finder.options = options.copy(maxPoints = it) } },
                label = "Most points", modifier = Modifier.width(110.dp)
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.xs), verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.xs)) {
            Text("Axes", style = TmTheme.typography.caption, color = TmTheme.colors.textMuted)
            listOf(DataType.UBYTE, DataType.UWORD, DataType.ULONG).forEach { type ->
                val on = type in options.axisTypes
                TmFilterChip(text = type.name, selected = on, onClick = {
                    val next = if (on) options.axisTypes - type else options.axisTypes + type
                    if (next.isNotEmpty()) finder.options = options.copy(axisTypes = next)
                })
            }
            Text("Values", style = TmTheme.typography.caption, color = TmTheme.colors.textMuted)
            listOf(DataType.UBYTE, DataType.SBYTE, DataType.UWORD, DataType.SWORD, DataType.FLOAT32_IEEE).forEach { type ->
                val on = type in options.valueTypes
                TmFilterChip(text = type.name, selected = on, onClick = {
                    val next = if (on) options.valueTypes - type else options.valueTypes + type
                    if (next.isNotEmpty()) finder.options = options.copy(valueTypes = next)
                })
            }
        }
        TmSwitchRow(title = "Skip what the definition already has", checked = finder.skipKnown, onCheckedChange = { finder.skipKnown = it })
        TmSwitchRow(title = "Also axes without counts", subtitle = "Finds more, and more that aren't maps", checked = options.bare, onCheckedChange = { finder.options = options.copy(bare = it) })
    }
}

@Composable
private fun Suggestions(results: List<FoundMap>, finder: FinderHandler, modifier: Modifier) {
    TmSurface(modifier = modifier) {
        LazyColumn(Modifier.fillMaxSize().padding(TmTheme.spacing.xs)) {
            items(results.sortedByDescending { it.confidence }, key = { "${it.address}-${it.valueType}-${it.kind}" }) { found ->
                TmListRow(
                    title = found.label,
                    subtitle = found.reasons.joinToString(" · "),
                    selected = found === finder.selected,
                    onClick = { finder.selected = found },
                    trailing = {
                        TmBadge(text = "${found.confidence}%", tone = when {
                            found.confidence >= 80 -> TmTheme.colors.success
                            found.confidence >= 60 -> TmTheme.colors.accent
                            else -> TmTheme.colors.neutral
                        })
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Preview(found: FoundMap, finder: FinderHandler, onShowInHex: (Long) -> Unit, onAdded: (String) -> Unit, onMessage: (String) -> Unit, modifier: Modifier) {
    val spacing = TmTheme.spacing
    val values = remember(found) { finder.preview(found) }
    var name by remember(found) { mutableStateOf("Found ${found.address.toHexAddress()}") }
    val table = remember(found) { MapTableState() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(found.label, style = TmTheme.typography.subtitle, color = TmTheme.colors.text)
        Text(
            listOfNotNull(
                "values ${if (found.byteOrder == ByteOrder.LITTLE_ENDIAN) "LSB" else "MSB"} first",
                if (found.rows > 1) (if (found.order.name == "X_FIRST") "stored row by row" else "stored column by column") else null,
                "x axis ${found.x.type.name} at ${found.x.address.toHexAddress()}",
                found.y?.let { "y axis ${it.type.name} at ${it.address.toHexAddress()}" },
                "record ${found.record}"
            ).joinToString(" · "),
            style = TmTheme.typography.caption,
            color = TmTheme.colors.textMuted
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.sm), itemVerticalAlignment = Alignment.Bottom) {
            TmTextField(value = name, onValueChange = { name = it }, label = "Name", modifier = Modifier.width(240.dp))
            TmButton(text = "Add as a map", onClick = {
                val problem = finder.add(found, name)
                if (problem == null) onAdded(name.trim()) else onMessage(problem)
            }, leadingIcon = Res.drawable.tt_plus)
            TmButton(text = "Show in hex", onClick = { onShowInHex(found.record.start) }, variant = TmButtonVariant.SECONDARY, leadingIcon = Res.drawable.tt_cpu)
        }
        Text("Values are shown raw; set its factor, offset and units once it's added (Change, in the editor).", style = TmTheme.typography.caption, color = TmTheme.colors.textMuted)
        if (values != null) {
            TmSurface(modifier = Modifier.fillMaxWidth().weight(1f), level = TmSurfaceLevel.SUNKEN) {
                TmMapTable(values = values, state = table, readOnly = true, modifier = Modifier.fillMaxSize().padding(spacing.xs))
            }
            TmSurface(modifier = Modifier.fillMaxWidth().height(200.dp), level = TmSurfaceLevel.SUNKEN) {
                TmMapChart2D(values = values, modifier = Modifier.fillMaxSize().padding(spacing.md))
            }
        }
    }
}
