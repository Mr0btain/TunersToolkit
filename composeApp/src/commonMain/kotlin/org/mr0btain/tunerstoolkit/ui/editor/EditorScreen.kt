package org.mr0btain.tunerstoolkit.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.binary.BinaryFormat
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.BytePattern
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.core.image.parseAddress
import org.mr0btain.tunerstoolkit.core.image.toHexAddress
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import org.mr0btain.tunerstoolkit.ui.document.OpenDocument
import org.mr0btain.tunerstoolkit.ui.files.rememberFileSaver
import org.mr0btain.tunerstoolkit.ui.project.ProjectHandler
import org.mr0btain.tunerstoolkit.ui.kit.hex.HexRadix
import org.mr0btain.tunerstoolkit.ui.kit.hex.HexViewState
import org.mr0btain.tunerstoolkit.ui.kit.hex.TmHexView
import org.mr0btain.tunerstoolkit.ui.kit.hex.byteCount
import org.mr0btain.tunerstoolkit.ui.settings.SettingsHandler
import org.mr0btain.uikit.buttons.TmButtonRow
import org.mr0btain.uikit.buttons.TmIconButton
import org.mr0btain.uikit.containers.TmDialog
import org.mr0btain.uikit.containers.TmPageHeader
import org.mr0btain.uikit.feedback.TmEmptyState
import org.mr0btain.uikit.feedback.TmNotice
import org.mr0btain.uikit.feedback.TmNoticeKind
import org.mr0btain.uikit.foundation.TmSurface
import org.mr0btain.uikit.foundation.TmSurfaceLevel
import org.mr0btain.uikit.inputs.TmTextField
import org.mr0btain.uikit.selection.TmRadio
import org.mr0btain.uikit.selection.TmSegmented
import org.mr0btain.uikit.theme.TmTheme
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.tt_corner_up_left
import tunerstoolkit.composeapp.generated.resources.tt_corner_up_right
import tunerstoolkit.composeapp.generated.resources.tt_crosshair
import tunerstoolkit.composeapp.generated.resources.tt_cpu
import tunerstoolkit.composeapp.generated.resources.tt_download
import tunerstoolkit.composeapp.generated.resources.tt_folder_plus
import tunerstoolkit.composeapp.generated.resources.tt_save
import tunerstoolkit.composeapp.generated.resources.tt_search

/** The open image in the hex view, with its display options, undo, go to, find and export */
@Composable
fun EditorScreen(
    documents: DocumentHandler,
    projects: ProjectHandler,
    settings: SettingsHandler,
    snackbar: SnackbarHostState,
    onOpenFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val document = documents.document
    if (document == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TmEmptyState(
                title = "No image open",
                message = "Open an Intel HEX, S-record or binary file to see and change its bytes.",
                icon = Res.drawable.tt_cpu,
                actionText = "Open a file",
                onAction = onOpenFiles
            )
        }
        return
    }

    val spacing = TmTheme.spacing
    val scope = rememberCoroutineScope()
    val state = remember(document) { HexViewState(settings.data.hexBytesPerRow) }
    LaunchedEffect(settings.data.hexBytesPerRow) { state.bytesPerRow = settings.data.hexBytesPerRow }

    var goingTo by remember { mutableStateOf(false) }
    var finding by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf<SaveKind?>(null) }
    val saver = rememberFileSaver { name ->
        scope.launch { snackbar.showSnackbar(if (name != null) "Saved $name" else "Not saved") }
    }

    fun findNext(backwards: Boolean = false) {
        val pattern = documents.lastSearch ?: run { finding = true; return }
        val match = documents.find(pattern, state.cursor ?: 0, backwards)
        if (match != null) {
            state.highlight = match
            state.reveal(match.start)
        } else {
            scope.launch { snackbar.showSnackbar("Not found") }
        }
    }

    // Read so the undo buttons and the changed count follow edits
    val version = documents.version
    val image = document.image
    val changedBytes = remember(document, version) { image.changedRanges().sumOf { it.size } }

    val project = projects.current
    val unsaved = projects.hasUnsavedChanges
    fun save() {
        when {
            project == null -> saving = SaveKind.PROJECT
            project.isOriginal -> saving = SaveKind.VERSION
            else -> projects.save()
        }
    }

    Column(modifier.fillMaxSize().padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        TmPageHeader(
            title = project?.manifest?.name ?: document.name,
            onBack = null,
            subtitle = listOfNotNull(
                project?.versionName,
                "${document.format.label}, ${byteCount(image.size)}",
                if (changedBytes > 0) "${byteCount(changedBytes)} changed from the original" else null,
                if (unsaved) "not saved" else null
            ).joinToString(" · "),
            actions = {
                TmIconButton(
                    icon = Res.drawable.tt_save,
                    contentDescription = if (project == null) "Save as a project" else "Save",
                    onClick = ::save,
                    enabled = !projects.busy && (project == null || unsaved || project.isOriginal)
                )
                if (project != null) {
                    TmIconButton(
                        icon = Res.drawable.tt_folder_plus,
                        contentDescription = "Save as a new version",
                        onClick = { saving = SaveKind.VERSION },
                        enabled = !projects.busy
                    )
                }
                TmIconButton(icon = Res.drawable.tt_corner_up_left, contentDescription = "Undo", onClick = documents::undo, enabled = image.canUndo)
                TmIconButton(icon = Res.drawable.tt_corner_up_right, contentDescription = "Redo", onClick = documents::redo, enabled = image.canRedo)
                TmIconButton(icon = Res.drawable.tt_crosshair, contentDescription = "Go to address", onClick = { goingTo = true })
                TmIconButton(icon = Res.drawable.tt_search, contentDescription = "Find", onClick = { finding = true })
                TmIconButton(icon = Res.drawable.tt_download, contentDescription = "Export", onClick = { exporting = true })
            }
        )

        DisplayOptions(state, settings)

        TmSurface(modifier = Modifier.weight(1f).fillMaxWidth(), level = TmSurfaceLevel.SUNKEN) {
            TmHexView(
                image = image,
                version = version,
                state = state,
                onWrite = documents::write,
                modifier = Modifier.fillMaxSize().padding(vertical = spacing.xs),
                onUndo = documents::undo,
                onRedo = documents::redo,
                onGoTo = { goingTo = true },
                onFind = { finding = true },
                onFindNext = { findNext() },
                onSave = ::save,
                onMessage = { message -> scope.launch { snackbar.showSnackbar(message) } }
            )
        }

        StatusBar(image, state, version)
    }

    if (goingTo) GoToDialog(image, onGo = { state.reveal(it); goingTo = false }, onDismiss = { goingTo = false })
    if (finding) {
        FindDialog(
            initial = documents.lastSearch,
            onFind = { pattern, backwards ->
                documents.lastSearch = pattern
                findNext(backwards)
            },
            onDismiss = { finding = false }
        )
    }
    saving?.let { kind ->
        SaveDialog(
            kind = kind,
            fileName = document.name,
            versionNumber = (project?.manifest?.versions?.size ?: 0) + 1,
            hasChanges = changedBytes > 0,
            onSaveProject = { name, versionName, note -> projects.saveAsProject(name, versionName, note); saving = null },
            onSaveVersion = { name, note -> projects.saveAsVersion(name, note); saving = null },
            onDismiss = { saving = null }
        )
    }
    if (exporting) {
        ExportDialog(
            document = document,
            onExport = { format ->
                exporting = false
                when (val result = documents.export(format)) {
                    is TkResult.Success -> saver.save(result.data, document.name.substringBeforeLast('.'), extensionFor(format))
                    is TkResult.Failure -> scope.launch { snackbar.showSnackbar(result.error.message) }
                }
            },
            onDismiss = { exporting = false }
        )
    }
}

/** What the save dialog makes */
private enum class SaveKind { PROJECT, VERSION }

/**
 * Saving a file as a new project (with its changes as the first version), or the open project as a new version
 */
@Composable
private fun SaveDialog(
    kind: SaveKind,
    fileName: String,
    versionNumber: Int,
    hasChanges: Boolean,
    onSaveProject: (name: String, versionName: String, note: String) -> Unit,
    onSaveVersion: (name: String, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    var projectName by remember { mutableStateOf(fileName.substringBeforeLast('.')) }
    var versionName by remember { mutableStateOf("Version $versionNumber") }
    var note by remember { mutableStateOf("") }
    val asProject = kind == SaveKind.PROJECT
    val askVersion = !asProject || hasChanges

    TmDialog(
        title = if (asProject) "Save as a project" else "Save as a new version",
        description = when {
            !asProject -> "The open version stays as it was"
            hasChanges -> "The original file is kept as it is, and your changes become its first version"
            else -> "The original file is kept, and you can save versions of it as you change it"
        },
        onDismissRequest = onDismiss,
        actions = {
            TmButtonRow(
                confirmText = "Save",
                onConfirm = { if (asProject) onSaveProject(projectName, versionName, note) else onSaveVersion(versionName, note) },
                onCancel = onDismiss,
                confirmEnabled = (!asProject || projectName.isNotBlank()) && (!askVersion || versionName.isNotBlank())
            )
        }
    ) {
        if (asProject) {
            TmTextField(value = projectName, onValueChange = { projectName = it }, label = "Project", placeholder = "Golf 2.0 TSI")
        }
        if (askVersion) {
            TmTextField(value = versionName, onValueChange = { versionName = it }, label = "Version", placeholder = "Stage 1")
            TmTextField(value = note, onValueChange = { note = it }, label = "Note", placeholder = "What changed, optional")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DisplayOptions(state: HexViewState, settings: SettingsHandler) {
    val display = state.display
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm)
    ) {
        TmSegmented(
            label = "Cells",
            options = listOf(1, 2, 4, 8),
            selected = display.wordSize,
            onSelect = { state.display = display.copy(wordSize = it) },
            optionLabel = { if (it == 1) "Bytes" else "${it * 8} bit" }
        )
        TmSegmented(
            label = "Byte order",
            options = ByteOrder.entries,
            selected = display.order,
            onSelect = { state.display = display.copy(order = it) },
            optionLabel = { if (it == ByteOrder.LITTLE_ENDIAN) "Intel (LSB first)" else "Motorola (MSB first)" },
            enabled = display.wordSize > 1
        )
        TmSegmented(
            label = "Values",
            options = HexRadix.entries,
            selected = display.radix,
            onSelect = { state.display = display.copy(radix = it) },
            optionLabel = { it.label }
        )
        TmSegmented(
            label = "Bytes per row",
            options = listOf(8, 16, 32),
            selected = settings.data.hexBytesPerRow,
            onSelect = settings::setHexBytesPerRow,
            optionLabel = { it.toString() }
        )
    }
}

/** Where the cursor is, what's selected, and the value under the cursor read every common way */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusBar(image: MemoryImage, state: HexViewState, version: Long) {
    val colors = TmTheme.colors
    val cursor = state.cursor ?: return
    val order = state.display.order
    val values = remember(cursor, order, version) { valuesAt(image, cursor, order) }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.xxs)
    ) {
        Text(cursor.toHexAddress(), style = TmTheme.typography.numeric, color = colors.text)
        state.selection?.let {
            Text("$it, ${byteCount(it.size)} selected", style = TmTheme.typography.numeric, color = colors.accent.color)
        }
        values.forEach { (label, value) ->
            Text("$label $value", style = TmTheme.typography.numeric, color = colors.textMuted)
        }
    }
}

/** The value at [address] as each common type, in [order], skipping the ones that would run off the segment */
internal fun valuesAt(image: MemoryImage, address: Long, order: ByteOrder): List<Pair<String, String>> {
    val values = mutableListOf<Pair<String, String>>()
    fun add(label: String, size: Int, read: () -> String) {
        if (image.contains(address, size)) values += label to read()
    }
    add("u8", 1) { image.readUnsigned(address, 1, order).toString() }
    add("s8", 1) { image.readSigned(address, 1, order).toString() }
    add("u16", 2) { image.readUnsigned(address, 2, order).toString() }
    add("s16", 2) { image.readSigned(address, 2, order).toString() }
    add("u32", 4) { image.readUnsigned(address, 4, order).toString() }
    add("s32", 4) { image.readSigned(address, 4, order).toString() }
    add("f32", 4) { image.readFloat32(address, order).toString() }
    return values
}

@Composable
private fun GoToDialog(image: MemoryImage, onGo: (Long) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val address = parseAddress(text)
    val inImage = address != null && image.contains(address)

    TmDialog(
        title = "Go to address",
        onDismissRequest = onDismiss,
        actions = { TmButtonRow(confirmText = "Go", onConfirm = { address?.let(onGo) }, onCancel = onDismiss, confirmEnabled = inImage) }
    ) {
        TmTextField(
            value = text,
            onValueChange = { text = it },
            label = "Address",
            placeholder = image.ranges.firstOrNull()?.start?.toHexAddress(),
            supportingText = "Hex like 0x800100, or #decimal",
            error = when {
                text.isBlank() -> null
                address == null -> "That isn't an address"
                !inImage -> "Nothing's there in this image"
                else -> null
            }
        )
    }
}

@Composable
private fun FindDialog(initial: BytePattern?, onFind: (BytePattern, Boolean) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var asText by remember { mutableStateOf(false) }
    val pattern = if (asText) BytePattern.ofText(text) else BytePattern.parseHex(text)

    TmDialog(
        title = "Find",
        description = if (initial != null) "Searches from the cursor, and wraps round at the end" else null,
        onDismissRequest = onDismiss,
        actions = {
            TmButtonRow(
                confirmText = "Find next",
                onConfirm = { pattern?.let { onFind(it, false) } },
                onCancel = { pattern?.let { onFind(it, true) } },
                cancelText = "Find previous",
                confirmEnabled = pattern != null
            )
        }
    ) {
        TmSegmented(options = listOf(false, true), selected = asText, onSelect = { asText = it }, optionLabel = { if (it) "Text" else "Hex bytes" })
        TmTextField(
            value = text,
            onValueChange = { text = it },
            label = if (asText) "Text" else "Bytes",
            placeholder = if (asText) "TTDEMO1" else "1A 90 ?? 00",
            supportingText = if (asText) null else "Pairs of hex digits; ?? matches any byte",
            error = if (text.isNotBlank() && pattern == null) "That isn't a byte pattern" else null
        )
    }
}

@Composable
private fun ExportDialog(document: OpenDocument, onExport: (BinaryFormat) -> Unit, onDismiss: () -> Unit) {
    var format by remember { mutableStateOf(document.format) }
    val gaps = document.image.ranges.size > 1

    TmDialog(
        title = "Export",
        description = "Saves the image as it is now, with your changes",
        onDismissRequest = onDismiss,
        actions = { TmButtonRow(confirmText = "Save", onConfirm = { onExport(format) }, onCancel = onDismiss) }
    ) {
        BinaryFormat.entries.forEach { option ->
            TmRadio(selected = format == option, onClick = { format = option }, label = option.label)
        }
        if (format == BinaryFormat.RAW && gaps) {
            TmNotice(
                kind = TmNoticeKind.INFO,
                text = "A raw binary has no addresses: it runs from the first segment to the end of the last, with the gaps filled with FF."
            )
        }
    }
}

private fun extensionFor(format: BinaryFormat) = when (format) {
    BinaryFormat.INTEL_HEX -> "hex"
    BinaryFormat.S_RECORD -> "s19"
    BinaryFormat.RAW -> "bin"
}
