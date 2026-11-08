package org.mr0btain.tunerstoolkit.ui.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.mr0btain.applogger.AppLogger
import org.mr0btain.tunerstoolkit.components.testing.readTestDataFile
import org.mr0btain.tunerstoolkit.core.diagnostics.Diagnostic
import org.mr0btain.tunerstoolkit.core.diagnostics.Severity
import org.mr0btain.tunerstoolkit.core.image.parseAddress
import org.mr0btain.tunerstoolkit.core.image.toHexAddress
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import org.mr0btain.tunerstoolkit.ui.document.OpenDocument
import org.mr0btain.tunerstoolkit.ui.kit.hex.byteCount
import org.mr0btain.uikit.buttons.TmButton
import org.mr0btain.uikit.buttons.TmButtonVariant
import org.mr0btain.uikit.containers.TmPageHeader
import org.mr0btain.uikit.containers.TmPanel
import org.mr0btain.uikit.containers.TmScrollColumn
import org.mr0btain.uikit.data.TmKeyValueRow
import org.mr0btain.uikit.feedback.TmBadge
import org.mr0btain.uikit.feedback.TmNotice
import org.mr0btain.uikit.feedback.TmNoticeKind
import org.mr0btain.uikit.foundation.TmDivider
import org.mr0btain.uikit.inputs.TmTextField
import org.mr0btain.uikit.theme.TmTheme
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.tt_file
import tunerstoolkit.composeapp.generated.resources.tt_folder
import tunerstoolkit.composeapp.generated.resources.tt_grid
import tunerstoolkit.composeapp.generated.resources.tt_upload

private const val TAG = "FilesScreen"

/** Most diagnostics listed in the import report; the rest are counted */
private const val SHOWN_DIAGNOSTICS = 100

/**
 * Opening ECU images, and the import report for the one that's open: its segments and everything reading it found
 * wrong
 */
@Composable
fun FilesScreen(
    documents: DocumentHandler,
    onOpenEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TmTheme.spacing
    val scope = rememberCoroutineScope()
    var baseText by rememberSaveable { mutableStateOf("0x0") }
    val base = parseAddress(baseText)
    // The demo is read from the checkout, so it's only there on desktop while developing
    val demo = remember { readTestDataFile("demo/tt-demo-1.hex") }

    val picker = rememberFilePickerLauncher(type = FileKitType.File()) { file ->
        if (file == null) return@rememberFilePickerLauncher
        scope.launch {
            try {
                documents.open(file.readBytes(), file.name, base ?: 0)
            } catch (e: Exception) {
                AppLogger.e("$TAG:Read", "Couldn't read ${file.name}", e)
            }
        }
    }

    TmScrollColumn(
        modifier = modifier.fillMaxSize().padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg)
    ) {
        TmPageHeader(
            title = "Files",
            onBack = null,
            subtitle = "Open an ECU image: Intel HEX, Motorola S-record, or a raw binary"
        )

        TmPanel(modifier = Modifier.fillMaxWidth(), title = "Open an image", icon = Res.drawable.tt_folder) {
            OpenButtons(
                loading = documents.loading,
                onOpen = { picker.launch() },
                onOpenDemo = demo?.let { bytes -> { documents.open(bytes, "tt-demo-1.hex") } }
            )
            TmTextField(
                modifier = Modifier.widthIn(max = 360.dp),
                value = baseText,
                onValueChange = { baseText = it },
                label = "Where a raw binary starts",
                supportingText = "In hex, like 0x800000. HEX and S-record files say where they go themselves",
                error = if (base == null) "That isn't an address" else null
            )
            documents.openError?.let { TmNotice(kind = TmNoticeKind.ERROR, title = "Couldn't open the file", text = it) }
        }

        documents.document?.let { ImportReport(it, onOpenEditor) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpenButtons(loading: Boolean, onOpen: () -> Unit, onOpenDemo: (() -> Unit)?) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm)
    ) {
        TmButton(text = "Open a file", onClick = onOpen, leadingIcon = Res.drawable.tt_upload, loading = loading)
        if (onOpenDemo != null) {
            TmButton(text = "Open the demo image", onClick = onOpenDemo, variant = TmButtonVariant.SECONDARY, enabled = !loading)
        }
    }
}

/** What was read: where the segments are, and every problem reading found */
@Composable
private fun ImportReport(document: OpenDocument, onOpenEditor: () -> Unit) {
    val image = document.image
    TmPanel(
        modifier = Modifier.fillMaxWidth(),
        title = document.name,
        subtitle = "${document.format.label}, ${byteCount(image.size)} in ${image.ranges.size} segments",
        icon = Res.drawable.tt_file,
        actions = { TmButton(text = "Open in the editor", onClick = onOpenEditor, leadingIcon = Res.drawable.tt_grid) }
    ) {
        image.ranges.forEach { range ->
            TmKeyValueRow(label = range.toString(), value = byteCount(range.size), numeric = true)
        }
        image.startAddress?.let { TmKeyValueRow(label = "Start address", value = it.toHexAddress(), numeric = true) }
        image.header?.takeIf { it.isNotBlank() }?.let { TmKeyValueRow(label = "Header", value = it) }

        TmDivider()

        val errors = document.diagnostics.count { it.severity == Severity.ERROR }
        val warnings = document.diagnostics.count { it.severity == Severity.WARNING }
        when {
            document.diagnostics.isEmpty() -> TmNotice(kind = TmNoticeKind.SUCCESS, text = "Read cleanly, nothing was skipped.")
            errors > 0 -> TmNotice(
                kind = TmNoticeKind.ERROR,
                title = "$errors records couldn't be read",
                text = "They were left out and their bytes are missing. Check the lines below in the file."
            )
            else -> TmNotice(kind = TmNoticeKind.WARNING, text = "$warnings things looked odd, but everything was read.")
        }
        document.diagnostics.take(SHOWN_DIAGNOSTICS).forEach { DiagnosticRow(it) }
        if (document.diagnostics.size > SHOWN_DIAGNOSTICS) {
            Text(
                "…and ${document.diagnostics.size - SHOWN_DIAGNOSTICS} more",
                style = TmTheme.typography.caption,
                color = TmTheme.colors.textMuted
            )
        }
    }
}

@Composable
private fun DiagnosticRow(diagnostic: Diagnostic) {
    val colors = TmTheme.colors
    val (label, tone) = when (diagnostic.severity) {
        Severity.ERROR -> "Error" to colors.danger
        Severity.WARNING -> "Warning" to colors.warning
        Severity.INFO -> "Info" to colors.neutral
    }
    Row(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        TmBadge(text = label, tone = tone)
        diagnostic.location?.let {
            Text("line ${it.line}", style = TmTheme.typography.numeric, color = colors.textMuted)
        }
        Column(Modifier.weight(1f)) {
            Text(diagnostic.message, style = TmTheme.typography.body, color = colors.text)
        }
    }
}
