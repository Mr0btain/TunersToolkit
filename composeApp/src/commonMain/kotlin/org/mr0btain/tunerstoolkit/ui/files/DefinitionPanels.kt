package org.mr0btain.tunerstoolkit.ui.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.mr0btain.tunerstoolkit.core.a2l.EpkResult
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.toHexAddress
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionFormat
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionHandler
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionOnImage
import org.mr0btain.tunerstoolkit.ui.definition.OpenDefinition
import org.mr0btain.tunerstoolkit.ui.exchange.ExchangeError
import org.mr0btain.tunerstoolkit.ui.exchange.ExchangeHandler
import org.mr0btain.tunerstoolkit.ui.exchange.ExportScope
import org.mr0btain.tunerstoolkit.ui.exchange.Exported
import org.mr0btain.tunerstoolkit.ui.exchange.ValuesFormat
import org.mr0btain.uikit.buttons.TmButton
import org.mr0btain.uikit.buttons.TmButtonRow
import org.mr0btain.uikit.buttons.TmButtonVariant
import org.mr0btain.uikit.containers.TmDialog
import org.mr0btain.uikit.containers.TmPanel
import org.mr0btain.uikit.data.TmKeyValueRow
import org.mr0btain.uikit.data.TmStatTile
import org.mr0btain.uikit.feedback.TmNotice
import org.mr0btain.uikit.feedback.TmNoticeKind
import org.mr0btain.uikit.foundation.TmControlSize
import org.mr0btain.uikit.foundation.TmDivider
import org.mr0btain.uikit.selection.TmFilterChip
import org.mr0btain.uikit.selection.TmRadio
import org.mr0btain.uikit.theme.TmTheme
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.tt_download
import tunerstoolkit.composeapp.generated.resources.tt_grid
import tunerstoolkit.composeapp.generated.resources.tt_list
import tunerstoolkit.composeapp.generated.resources.tt_table
import tunerstoolkit.composeapp.generated.resources.tt_upload

/** Most diagnostics listed at once; the rest are counted */
private const val SHOWN = 100

/** Opening a definition, and which one is open */
@Composable
internal fun DefinitionPanel(definitions: DefinitionHandler, onOpen: () -> Unit) {
    val definition = definitions.definition
    TmPanel(
        modifier = Modifier.fillMaxWidth(),
        title = "Open a definition",
        subtitle = "An A2L, a TunerPro XDF or a .ttdef says where the maps are and how to read them",
        icon = Res.drawable.tt_list
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm)) {
            TmButton(text = "Open a definition", onClick = onOpen, leadingIcon = Res.drawable.tt_upload, loading = definitions.loading)
            if (definition != null) {
                TmButton(text = "Close ${definition.name}", onClick = definitions::close, variant = TmButtonVariant.SECONDARY)
            }
        }
        definitions.openError?.let { TmNotice(kind = TmNoticeKind.ERROR, title = "Couldn't open the definition", text = it) }
    }
}

/**
 * What the definition holds, how much of it could be read from the image, whether it's the image's A2L at all (the
 * EPK), and everything that went wrong, filterable by how bad it is. It can be saved as a .ttdef or an XDF
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DefinitionReport(
    definition: OpenDefinition,
    onImage: DefinitionOnImage?,
    indexing: Boolean,
    exchange: ExchangeHandler,
    onSave: (TkResult<Exported, ExchangeError>) -> Unit,
    onOpenEditor: () -> Unit
) {
    val spacing = TmTheme.spacing
    val a2l = definition.a2l
    val objects = definition.set.objects.size
    val axes = definition.set.objects.count { it.kind == ObjectKind.AXIS }
    val problems = definition.diagnostics + onImage?.build?.problems.orEmpty()
    var shown by rememberSaveable { mutableStateOf<Severity?>(null) }

    TmPanel(
        modifier = Modifier.fillMaxWidth(),
        title = definition.name,
        subtitle = listOfNotNull(
            definition.format?.label,
            a2l?.file?.asap2Version?.let { "ASAP2 $it" },
            a2l?.module?.name?.let { "module $it" },
            definition.title.takeIf { it.isNotBlank() && it != definition.name },
            if (definition.additions.isNotEmpty()) "${definition.additions.count { it.kind != ObjectKind.AXIS }} made here" else null
        ).joinToString(" · "),
        icon = Res.drawable.tt_list,
        actions = { TmButton(text = "Open in the editor", onClick = onOpenEditor, leadingIcon = Res.drawable.tt_grid) }
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            val tile = Modifier.widthIn(min = 150.dp)
            TmStatTile(label = "Objects", value = objects.toString(), modifier = tile, caption = "${objects - axes} maps and values, $axes axes")
            TmStatTile(
                label = "Read from the image",
                value = when {
                    onImage != null -> onImage.build.decoded.toString()
                    indexing -> "…"
                    else -> "–"
                },
                modifier = tile,
                caption = if (onImage == null && !indexing) "Open an image to read them" else null
            )
            TmStatTile(
                label = "Left out",
                value = (definition.leftOut + (onImage?.let { objects - it.build.decoded } ?: 0)).toString(),
                modifier = tile,
                attention = definition.leftOut > 0 || (onImage != null && onImage.build.decoded < objects)
            )
            TmStatTile(label = "Warnings", value = problems.count { it.severity == Severity.WARNING }.toString(), modifier = tile)
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            TmButton(text = "Save as .ttdef", onClick = { onSave(exchange.exportDefinition(DefinitionFormat.TTDEF)) }, variant = TmButtonVariant.SECONDARY, size = TmControlSize.SMALL, leadingIcon = Res.drawable.tt_download)
            TmButton(text = "Save as XDF", onClick = { onSave(exchange.exportDefinition(DefinitionFormat.XDF)) }, variant = TmButtonVariant.SECONDARY, size = TmControlSize.SMALL, leadingIcon = Res.drawable.tt_download, enabled = onImage != null)
            TmButton(text = "Map list as CSV", onClick = { onSave(exchange.exportMapList()) }, variant = TmButtonVariant.SECONDARY, size = TmControlSize.SMALL, leadingIcon = Res.drawable.tt_download)
        }

        onImage?.let { EpkNotice(it.epk) }
        onImage?.overlaps?.takeIf { it.isNotEmpty() }?.let { overlaps ->
            TmNotice(
                kind = TmNoticeKind.WARNING,
                title = "${overlaps.size}${if (overlaps.size >= 200) "+" else ""} places where objects share memory",
                text = overlaps.take(5).joinToString("\n") { "${it.first} and ${it.second} at ${it.range}" } +
                    if (overlaps.size > 5) "\n…" else ""
            )
        }

        a2l?.module?.modPar?.let { modPar ->
            modPar.epk?.let { TmKeyValueRow(label = "EPK", value = it) }
            modPar.ecu?.let { TmKeyValueRow(label = "ECU", value = it) }
            modPar.ecuCalibrationOffset?.let { TmKeyValueRow(label = "Calibration offset", value = it.toHexAddress(), numeric = true) }
        }

        if (problems.isNotEmpty()) {
            TmDivider()
            DiagnosticList(problems, shown, onShow = { shown = it })
        } else {
            TmNotice(kind = TmNoticeKind.SUCCESS, text = "Read cleanly, nothing was left out.")
        }
    }
}

/** Diagnostics with chips to show one severity, at most [SHOWN] of them */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiagnosticList(problems: List<Diagnostic>, shown: Severity?, onShow: (Severity?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.xs)) {
        TmFilterChip(text = "All ${problems.size}", selected = shown == null, onClick = { onShow(null) })
        listOf(Severity.ERROR to "Errors", Severity.WARNING to "Warnings", Severity.INFO to "Notes").forEach { (severity, label) ->
            val count = problems.count { it.severity == severity }
            if (count > 0) TmFilterChip(text = "$label $count", selected = shown == severity, onClick = { onShow(severity) })
        }
    }
    val listed = problems.filter { shown == null || it.severity == shown }
    listed.take(SHOWN).forEach { DiagnosticRow(it) }
    if (listed.size > SHOWN) {
        Text("…and ${listed.size - SHOWN} more", style = TmTheme.typography.caption, color = TmTheme.colors.textMuted)
    }
}

/**
 * Calibration values in and out by name: DCM, CDFX or PaCo in (onto the open image, as one undo step), DCM or CDFX
 * out, every map or only the changed ones. The last import's report is under it
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ValuesPanel(exchange: ExchangeHandler, onImport: () -> Unit, onSave: (TkResult<Exported, ExchangeError>) -> Unit) {
    var exporting by remember { mutableStateOf(false) }
    var shown by rememberSaveable { mutableStateOf<Severity?>(null) }
    TmPanel(
        modifier = Modifier.fillMaxWidth(),
        title = "Calibration values",
        subtitle = "Maps' values by name, to and from other tools: DCM and CDFX (PaCo too, in)",
        icon = Res.drawable.tt_table
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm)) {
            TmButton(text = "Import values", onClick = onImport, leadingIcon = Res.drawable.tt_upload)
            TmButton(text = "Export values", onClick = { exporting = true }, variant = TmButtonVariant.SECONDARY, leadingIcon = Res.drawable.tt_download)
        }
        exchange.lastImport?.let { last ->
            val report = last.report
            TmDivider()
            TmNotice(
                kind = if (report.problems.any { it.severity == Severity.ERROR } || report.missing.isNotEmpty()) TmNoticeKind.WARNING else TmNoticeKind.SUCCESS,
                title = "From ${last.name}",
                text = report.summary + ". Undo puts them all back."
            )
            if (report.missing.isNotEmpty()) {
                TmKeyValueRow(label = "Not in the definition", value = report.missing.take(20).joinToString() + if (report.missing.size > 20) ", …" else "")
            }
            val problems = last.readProblems + report.problems
            if (problems.isNotEmpty()) DiagnosticList(problems, shown, onShow = { shown = it })
            TmButton(text = "Hide this", onClick = exchange::clearImport, variant = TmButtonVariant.GHOST, size = TmControlSize.SMALL)
        }
    }
    if (exporting) {
        ExportValuesDialog(onExport = { format, scope -> exporting = false; onSave(exchange.exportValues(format, scope)) }, onDismiss = { exporting = false })
    }
}

@Composable
private fun ExportValuesDialog(onExport: (ValuesFormat, ExportScope) -> Unit, onDismiss: () -> Unit) {
    var format by remember { mutableStateOf(ValuesFormat.DCM) }
    var scope by remember { mutableStateOf(ExportScope.ALL) }
    TmDialog(
        title = "Export values",
        description = "Physical values with their units and axes, by name",
        onDismissRequest = onDismiss,
        actions = { TmButtonRow(confirmText = "Save", onConfirm = { onExport(format, scope) }, onCancel = onDismiss) }
    ) {
        ValuesFormat.entries.forEach { option -> TmRadio(selected = format == option, onClick = { format = option }, label = option.label) }
        TmDivider()
        ExportScope.entries.forEach { option -> TmRadio(selected = scope == option, onClick = { scope = option }, label = option.label) }
    }
}

@Composable
private fun EpkNotice(epk: EpkResult) {
    when (epk) {
        is EpkResult.Match -> TmNotice(kind = TmNoticeKind.SUCCESS, text = "The image's EPK matches the A2L: \"${epk.epk}\" at ${epk.address.toHexAddress()}.")
        is EpkResult.Mismatch -> TmNotice(
            kind = TmNoticeKind.ERROR,
            title = "This A2L may not be for this image",
            text = "The A2L's EPK is \"${epk.expected}\", but the image has \"${epk.found}\" at ${epk.address.toHexAddress()}. " +
                "Values read with it may be wrong."
        )
        is EpkResult.NotInImage -> TmNotice(
            kind = TmNoticeKind.WARNING,
            text = "The A2L's EPK address ${epk.addresses.joinToString { it.toHexAddress() }} isn't in the image, so it can't be checked."
        )
        EpkResult.NotDescribed -> Unit
    }
}
