package org.mr0btain.tunerstoolkit.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationCodec
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationValues
import org.mr0btain.tunerstoolkit.core.calibration.writeAll
import org.mr0btain.tunerstoolkit.core.error.getOrNull
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionOnImage
import org.mr0btain.tunerstoolkit.ui.kit.hex.HexMarks
import org.mr0btain.uikit.buttons.TmButton
import org.mr0btain.uikit.buttons.TmButtonVariant
import org.mr0btain.uikit.containers.TmTabs
import org.mr0btain.uikit.foundation.TmControlSize
import tunerstoolkit.composeapp.generated.resources.tt_table
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
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionHandler
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

/**
 * The open image: the definition's map list beside tabs for the hex view and each open map, with saving, undo, go to,
 * find and export
 */
@Composable
fun EditorScreen(
    documents: DocumentHandler,
    projects: ProjectHandler,
    definitions: DefinitionHandler,
    settings: SettingsHandler,
    snackbar: SnackbarHostState,
    onOpenFiles: () -> Unit,
    modifier: Modifier = Modifier,
    editor: EditorHandler = remember { EditorHandler() },
    startAt: Long? = null
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
    LaunchedEffect(document, startAt) { startAt?.let { state.moveTo(it); state.reveal(it) } }
    LaunchedEffect(editor.revealRequest, editor.activeMap) {
        val address = editor.revealRequest ?: return@LaunchedEffect
        if (editor.activeMap == null) {
            state.moveTo(address)
            state.reveal(address)
            editor.revealRequest = null
        }
    }

    var goingTo by remember { mutableStateOf(false) }
    var finding by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf<SaveKind?>(null) }
    val saver = rememberFileSaver { name ->
        scope.launch { snackbar.showSnackbar(if (name != null) "Saved $name" else "Not saved") }
    }
    fun message(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }

    fun findNext(backwards: Boolean = false) {
        val pattern = documents.lastSearch ?: run { finding = true; return }
        val match = documents.find(pattern, state.cursor ?: 0, backwards)
        if (match != null) {
            state.highlight = match
            state.reveal(match.start)
        } else {
            message("Not found")
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

    val definition = definitions.definition
    LaunchedEffect(definition) { if (editor.openMaps.any { definition?.set?.get(it.name) == null }) editor.reset() }
    val onImage = definitions.onImage?.takeIf { it.document === document }
    val marks = remember(onImage, editor.selectedObject) { onImage?.let { objectMarks(it, editor.selectedObject) } }
    val coverage = remember(onImage) { onImage?.build?.index?.coverage.orEmpty() }

    fun openObject(name: String) {
        editor.open(name)
    }

    fun write(values: CalibrationValues, changes: Map<Int, Double>) {
        if (changes.isEmpty()) return
        val summary = documents.edit { CalibrationCodec.writeAll(it, values, changes) } ?: return
        when {
            summary.failed.isNotEmpty() -> message("${summary.failed.size} values couldn't be written: ${summary.failed.values.first().message}")
            summary.clamped > 0 -> message("${summary.clamped} values were past the limits and were set to them")
        }
    }

    Column(modifier.fillMaxSize().padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        TmPageHeader(
            title = project?.manifest?.name ?: document.name,
            onBack = null,
            subtitle = listOfNotNull(
                project?.versionName,
                "${document.format.label}, ${byteCount(image.size)}",
                definition?.name,
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

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= WIDE_EDITOR
            var showingList by rememberSaveable { mutableStateOf(false) }
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                if (definition != null && wide) {
                    TmSurface(modifier = Modifier.width(300.dp).fillMaxHeight()) {
                        MapList(definition, editor, onOpen = ::openObject, modifier = Modifier.fillMaxSize().padding(spacing.sm))
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    val listTab = definition != null && !wide
                    val tabs = buildList {
                        if (listTab) add("Maps")
                        add("Hex")
                        editor.openMaps.forEach { add(it.name) }
                    }
                    val selected = when {
                        listTab && showingList -> 0
                        editor.activeMap == null -> if (listTab) 1 else 0
                        else -> tabs.indexOf(editor.activeMap).coerceAtLeast(0)
                    }
                    if (tabs.size > 1) {
                        TmTabs(tabs = tabs, selectedIndex = selected, onSelect = { index ->
                            showingList = listTab && index == 0
                            if (!showingList) {
                                val name = tabs[index]
                                if (index == (if (listTab) 1 else 0)) editor.showHex() else editor.open(name)
                            }
                        })
                    }
                    val active = editor.active
                    when {
                        listTab && showingList -> MapList(definition!!, editor, onOpen = { showingList = false; openObject(it) }, modifier = Modifier.fillMaxSize())
                        active != null && definition?.set?.get(active.name) != null -> {
                            val obj = definition.set[active.name]!!
                            val values = remember(obj, version, document) { definitions.decode(obj) }
                            val original = remember(obj, document) { documents.original()?.let { definitions.decode(obj, it)?.getOrNull() } }
                            MapPane(
                                obj = obj,
                                open = active,
                                values = values,
                                original = original,
                                onChange = ::write,
                                onWriteText = { values, text ->
                                    val result = documents.edit { CalibrationCodec.writeAscii(it, values, text) }
                                    if (result is TkResult.Failure) message(result.error.message)
                                },
                                onShowInHex = {
                                    val address = onImage?.build?.index?.parts(obj.name)?.firstOrNull()?.range?.start ?: obj.address
                                    editor.selectedObject = obj.name
                                    editor.showHex(address)
                                },
                                onClose = { editor.close(obj.name) },
                                onUndo = documents::undo,
                                onRedo = documents::redo,
                                onMessage = ::message,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> HexContent(
                            documents = documents,
                            definitions = definitions,
                            settings = settings,
                            state = state,
                            version = version,
                            marks = marks,
                            coverage = coverage,
                            onImage = onImage,
                            editor = editor,
                            onSave = ::save,
                            onGoTo = { goingTo = true },
                            onFind = { finding = true },
                            onFindNext = { findNext() },
                            onMessage = ::message,
                            onOpen = ::openObject
                        )
                    }
                }
            }
        }
    }

    if (goingTo) GoToDialog(image, onGo = { editor.showHex(); state.reveal(it); goingTo = false }, onDismiss = { goingTo = false })
    if (finding) {
        FindDialog(
            initial = documents.lastSearch,
            onFind = { pattern, backwards ->
                documents.lastSearch = pattern
                editor.showHex()
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
                    is TkResult.Failure -> message(result.error.message)
                }
            },
            onDismiss = { exporting = false }
        )
    }
}

/** From this width the map list sits beside the editor; below it, it's a tab */
private val WIDE_EDITOR = 900.dp

/** The hex view with its display options, the object under the cursor and the status line */
@Composable
private fun ColumnScope.HexContent(
    documents: DocumentHandler,
    definitions: DefinitionHandler,
    settings: SettingsHandler,
    state: HexViewState,
    version: Long,
    marks: HexMarks?,
    coverage: List<AddressRange>,
    onImage: DefinitionOnImage?,
    editor: EditorHandler,
    onSave: () -> Unit,
    onGoTo: () -> Unit,
    onFind: () -> Unit,
    onFindNext: () -> Unit,
    onMessage: (String) -> Unit,
    onOpen: (String) -> Unit
) {
    val image = documents.document?.image ?: return
    val objectAtCursor = state.cursor?.let { cursor -> onImage?.build?.index?.at(cursor)?.firstOrNull()?.obj }
    LaunchedEffect(objectAtCursor) { objectAtCursor?.let { editor.selectedObject = it.name } }

    DisplayOptions(state, settings)
    TmSurface(modifier = Modifier.weight(1f).fillMaxWidth(), level = TmSurfaceLevel.SUNKEN) {
        TmHexView(
            image = image,
            version = version,
            state = state,
            onWrite = documents::write,
            modifier = Modifier.fillMaxSize().padding(vertical = TmTheme.spacing.xs),
            onUndo = documents::undo,
            onRedo = documents::redo,
            onGoTo = onGoTo,
            onFind = onFind,
            onFindNext = onFindNext,
            onSave = onSave,
            onMessage = onMessage,
            marks = marks,
            coverage = coverage,
            onOpenAt = { address -> onImage?.build?.index?.at(address)?.firstOrNull()?.let { onOpen(it.obj.name) } }
        )
    }
    objectAtCursor?.let { obj ->
        Row(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { ObjectAtCursor(obj, definitions, state.cursor!!, version) }
            TmButton(text = "Open", onClick = { onOpen(obj.name) }, size = TmControlSize.SMALL, variant = TmButtonVariant.SECONDARY, leadingIcon = Res.drawable.tt_table)
        }
    }
    StatusBar(image, state, version)
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
