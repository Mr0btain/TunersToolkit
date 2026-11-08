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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.mr0btain.tunerstoolkit.core.a2l.EpkResult
import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.image.toHexAddress
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionHandler
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionOnImage
import org.mr0btain.tunerstoolkit.ui.definition.OpenDefinition
import org.mr0btain.uikit.buttons.TmButton
import org.mr0btain.uikit.buttons.TmButtonVariant
import org.mr0btain.uikit.containers.TmPanel
import org.mr0btain.uikit.data.TmKeyValueRow
import org.mr0btain.uikit.feedback.TmNotice
import org.mr0btain.uikit.feedback.TmNoticeKind
import org.mr0btain.uikit.data.TmStatTile
import org.mr0btain.uikit.foundation.TmDivider
import org.mr0btain.uikit.selection.TmFilterChip
import org.mr0btain.uikit.theme.TmTheme
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.tt_grid
import tunerstoolkit.composeapp.generated.resources.tt_list
import tunerstoolkit.composeapp.generated.resources.tt_upload

/** Most diagnostics listed at once; the rest are counted */
private const val SHOWN = 100

/** Opening an A2L, and which one is open */
@Composable
internal fun DefinitionPanel(definitions: DefinitionHandler, onOpen: () -> Unit) {
    val definition = definitions.definition
    TmPanel(
        modifier = Modifier.fillMaxWidth(),
        title = "Open a definition",
        subtitle = "An A2L says where the maps are and how to read them",
        icon = Res.drawable.tt_list
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm)) {
            TmButton(text = "Open an A2L", onClick = onOpen, leadingIcon = Res.drawable.tt_upload, loading = definitions.loading)
            if (definition != null) {
                TmButton(text = "Close ${definition.name}", onClick = definitions::close, variant = TmButtonVariant.SECONDARY)
            }
        }
        definitions.openError?.let { TmNotice(kind = TmNoticeKind.ERROR, title = "Couldn't open the A2L", text = it) }
    }
}

/**
 * What the A2L holds, how much of it could be read from the image, whether it's the image's A2L at all (the EPK), and
 * everything that went wrong, filterable by how bad it is
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DefinitionReport(definition: OpenDefinition, onImage: DefinitionOnImage?, indexing: Boolean, onOpenEditor: () -> Unit) {
    val spacing = TmTheme.spacing
    val module = definition.module
    val objects = definition.set.objects.size
    val problems = definition.diagnostics + onImage?.build?.problems.orEmpty()
    var shown by rememberSaveable { mutableStateOf<Severity?>(null) }

    TmPanel(
        modifier = Modifier.fillMaxWidth(),
        title = definition.name,
        subtitle = listOfNotNull(
            definition.file.asap2Version?.let { "ASAP2 $it" },
            "module ${module.name}",
            module.longId.takeIf { it.isNotBlank() }
        ).joinToString(" · "),
        icon = Res.drawable.tt_list,
        actions = { TmButton(text = "Open in the editor", onClick = onOpenEditor, leadingIcon = Res.drawable.tt_grid) }
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            val tile = Modifier.widthIn(min = 150.dp)
            TmStatTile(label = "Objects", value = objects.toString(), modifier = tile, caption = "${module.characteristics.size} maps and values, ${module.axisPts.size} axes")
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

        onImage?.let { EpkNotice(it.epk) }
        onImage?.overlaps?.takeIf { it.isNotEmpty() }?.let { overlaps ->
            TmNotice(
                kind = TmNoticeKind.WARNING,
                title = "${overlaps.size}${if (overlaps.size >= 200) "+" else ""} places where objects share memory",
                text = overlaps.take(5).joinToString("\n") { "${it.first} and ${it.second} at ${it.range}" } +
                    if (overlaps.size > 5) "\n…" else ""
            )
        }

        module.modPar?.let { modPar ->
            modPar.epk?.let { TmKeyValueRow(label = "EPK", value = it) }
            modPar.ecu?.let { TmKeyValueRow(label = "ECU", value = it) }
            modPar.ecuCalibrationOffset?.let { TmKeyValueRow(label = "Calibration offset", value = it.toHexAddress(), numeric = true) }
        }

        if (problems.isNotEmpty()) {
            TmDivider()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                TmFilterChip(text = "All ${problems.size}", selected = shown == null, onClick = { shown = null })
                listOf(Severity.ERROR to "Errors", Severity.WARNING to "Warnings", Severity.INFO to "Notes").forEach { (severity, label) ->
                    val count = problems.count { it.severity == severity }
                    if (count > 0) TmFilterChip(text = "$label $count", selected = shown == severity, onClick = { shown = severity })
                }
            }
            val listed: List<Diagnostic> = problems.filter { shown == null || it.severity == shown }
            listed.take(SHOWN).forEach { DiagnosticRow(it) }
            if (listed.size > SHOWN) {
                Text("…and ${listed.size - SHOWN} more", style = TmTheme.typography.caption, color = TmTheme.colors.textMuted)
            }
        } else {
            TmNotice(kind = TmNoticeKind.SUCCESS, text = "Read cleanly, nothing was left out.")
        }
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
