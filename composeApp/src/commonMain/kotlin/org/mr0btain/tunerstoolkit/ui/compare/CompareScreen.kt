package org.mr0btain.tunerstoolkit.ui.compare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationObject
import org.mr0btain.tunerstoolkit.core.error.getOrNull
import org.mr0btain.tunerstoolkit.core.project.ProjectManifest
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionHandler
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import org.mr0btain.tunerstoolkit.ui.editor.kindLabel
import org.mr0btain.tunerstoolkit.ui.kit.hex.byteCount
import org.mr0btain.tunerstoolkit.ui.kit.map.MapTableMode
import org.mr0btain.tunerstoolkit.ui.kit.map.MapTableState
import org.mr0btain.tunerstoolkit.ui.kit.map.TmMapTable
import org.mr0btain.tunerstoolkit.ui.project.ProjectHandler
import org.mr0btain.uikit.buttons.TmButton
import org.mr0btain.uikit.buttons.TmButtonVariant
import org.mr0btain.uikit.containers.TmListRow
import org.mr0btain.uikit.containers.TmPageHeader
import org.mr0btain.uikit.containers.TmPanel
import org.mr0btain.uikit.containers.TmScrollColumn
import org.mr0btain.uikit.data.TmKeyValueRow
import org.mr0btain.uikit.data.TmStatTile
import org.mr0btain.uikit.feedback.TmEmptyState
import org.mr0btain.uikit.feedback.TmLoading
import org.mr0btain.uikit.feedback.TmNotice
import org.mr0btain.uikit.feedback.TmNoticeKind
import org.mr0btain.uikit.foundation.TmControlSize
import org.mr0btain.uikit.foundation.TmSurface
import org.mr0btain.uikit.foundation.TmSurfaceLevel
import org.mr0btain.uikit.selection.TmDropdown
import org.mr0btain.uikit.selection.TmSegmented
import org.mr0btain.uikit.theme.TmTheme
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.tt_compare
import tunerstoolkit.composeapp.generated.resources.tt_table
import tunerstoolkit.composeapp.generated.resources.tt_upload

private const val TAG = "CompareScreen"

/** Most changed stretches outside maps listed; the rest are counted */
private const val SHOWN_REGIONS = 50

/**
 * The open image against its original, a saved version or another file: what changed, by map, with each map's values
 * side by side and a way to copy a map across
 */
@Composable
fun CompareScreen(
    documents: DocumentHandler,
    projects: ProjectHandler,
    definitions: DefinitionHandler,
    compare: CompareHandler,
    onOpenFiles: () -> Unit,
    onOpenMap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val document = documents.document
    if (document == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TmEmptyState(
                title = "No image open",
                message = "Open an image to compare it with its original, a saved version or another file.",
                icon = Res.drawable.tt_compare,
                actionText = "Open a file",
                onAction = onOpenFiles
            )
        }
        return
    }
    val spacing = TmTheme.spacing
    val version = documents.version
    // Edits change what differs
    LaunchedEffect(version, compare.other, definitions.onImage) { if (compare.other != null && compare.comparedAt != version) compare.refresh() }

    TmScrollColumn(modifier.fillMaxSize().padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        TmPageHeader(
            title = "Compare",
            onBack = null,
            subtitle = compare.other?.let { "${projects.current?.versionName ?: document.name} against ${it.label}" } ?: "Pick what to compare ${document.name} with"
        )
        ChooseOther(projects, compare)
        compare.error?.let { TmNotice(kind = TmNoticeKind.ERROR, text = it) }
        val comparison = compare.comparison
        when {
            compare.busy && comparison == null -> TmLoading(text = "Comparing")
            comparison != null && compare.other != null -> {
                Summary(compare, definitions.onImage == null)
                ChangedMaps(compare, definitions, onOpenMap)
                Outside(compare)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChooseOther(projects: ProjectHandler, compare: CompareHandler) {
    val scope = rememberCoroutineScope()
    val picker = rememberFilePickerLauncher(type = FileKitType.File()) { file ->
        if (file == null) return@rememberFilePickerLauncher
        scope.launch {
            try {
                compare.compareWithFile(file.readBytes(), file.name)
            } catch (e: Exception) {
                AppLogger.e("$TAG:Read", "Couldn't read ${file.name}", e)
            }
        }
    }
    val project = projects.current
    TmPanel(modifier = Modifier.fillMaxWidth(), title = "Compare with", icon = Res.drawable.tt_compare) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm),
            itemVerticalAlignment = Alignment.Bottom
        ) {
            TmButton(text = "The original", onClick = compare::compareWithOriginal, variant = TmButtonVariant.SECONDARY)
            if (project != null && project.manifest.versions.isNotEmpty()) {
                val choices = listOf(ProjectManifest.ORIGINAL) + project.manifest.versions.map { it.id }.filter { it != project.versionId }
                TmDropdown(
                    options = choices,
                    selected = null,
                    onSelect = { compare.compareWithVersion(project.manifest, it) },
                    optionLabel = { id -> project.manifest.version(id)?.name ?: "Original, as saved" },
                    placeholder = "A saved version",
                    modifier = Modifier.width(260.dp)
                )
            }
            TmButton(text = "Another file", onClick = { picker.launch() }, variant = TmButtonVariant.SECONDARY, leadingIcon = Res.drawable.tt_upload, loading = compare.busy)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Summary(compare: CompareHandler, noDefinition: Boolean) {
    val comparison = compare.comparison ?: return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm)) {
        val tile = Modifier.widthIn(min = 160.dp)
        TmStatTile(label = "Bytes that differ", value = byteCount(comparison.changedBytes), modifier = tile)
        TmStatTile(label = "Changed stretches", value = comparison.regions.size.toString(), modifier = tile)
        TmStatTile(label = "Maps changed", value = if (noDefinition) "–" else comparison.changedObjects.size.toString(), modifier = tile, caption = if (noDefinition) "Open an A2L to see which" else null)
        TmStatTile(label = "Outside maps", value = comparison.outsideObjects.size.toString(), modifier = tile, attention = !noDefinition && comparison.outsideObjects.isNotEmpty())
    }
    if (comparison.regions.isEmpty()) TmNotice(kind = TmNoticeKind.SUCCESS, text = "They're the same, byte for byte.")
}

/** Each changed map; one opens to show its values against the other image's */
@Composable
private fun ChangedMaps(compare: CompareHandler, definitions: DefinitionHandler, onOpenMap: (String) -> Unit) {
    val comparison = compare.comparison ?: return
    if (comparison.changedObjects.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    TmPanel(modifier = Modifier.fillMaxWidth(), title = "Changed maps", subtitle = "Open one to see it against ${compare.other?.label}", icon = Res.drawable.tt_table) {
        comparison.changedObjects.forEach { obj ->
            val open = expanded == obj.name
            TmListRow(
                title = obj.displayName ?: obj.name,
                subtitle = listOf(kindLabel(obj.kind), obj.description).filter { it.isNotBlank() }.joinToString(" · "),
                selected = open,
                onClick = { expanded = if (open) null else obj.name }
            )
            if (open) MapDiff(obj, compare, definitions, onOpenMap)
        }
    }
}

@Composable
private fun MapDiff(obj: CalibrationObject, compare: CompareHandler, definitions: DefinitionHandler, onOpenMap: (String) -> Unit) {
    val other = compare.other ?: return
    var mode by rememberSaveable(obj.name) { mutableStateOf(MapTableMode.DIFFERENCE) }
    val mine = remember(obj, compare.comparedAt) { definitions.decode(obj)?.getOrNull() }
    val theirs = remember(obj, other) { definitions.decode(obj, other.image)?.getOrNull() }
    val state = remember(obj.name) { MapTableState() }
    Column(Modifier.padding(start = TmTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm)) {
        if (mine == null || theirs == null) {
            TmNotice(kind = TmNoticeKind.WARNING, text = "${obj.name} can't be read from ${if (mine == null) "this image" else other.label}, so only its bytes can be compared.")
        } else {
            TmSegmented(options = MapTableMode.entries, selected = mode, onSelect = { mode = it }, optionLabel = { if (it == MapTableMode.OTHER) other.label.replaceFirstChar(Char::uppercase) else it.label }, size = TmControlSize.SMALL)
            TmSurface(modifier = Modifier.fillMaxWidth().height((36 * (mine.rows() + 1) + 16).coerceIn(60, 360).dp), level = TmSurfaceLevel.SUNKEN) {
                TmMapTable(values = mine, state = state, compareWith = theirs, mode = mode, readOnly = true, modifier = Modifier.fillMaxSize().padding(TmTheme.spacing.xs))
            }
        }
        Buttons {
            TmButton(text = "Copy from ${other.label}", onClick = { compare.copyFromOther(obj) }, size = TmControlSize.SMALL, variant = TmButtonVariant.SECONDARY)
            TmButton(text = "Open in the editor", onClick = { onOpenMap(obj.name) }, size = TmControlSize.SMALL, variant = TmButtonVariant.GHOST)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Buttons(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm)) { content() }
}

private fun org.mr0btain.tunerstoolkit.core.calibration.CalibrationValues.rows(): Int = dimensions.getOrElse(1) { 1 }

/** Changed bytes no map covers: often checksums, or code */
@Composable
private fun Outside(compare: CompareHandler) {
    val regions = compare.comparison?.outsideObjects.orEmpty()
    if (regions.isEmpty()) return
    TmPanel(modifier = Modifier.fillMaxWidth(), title = "Changed outside any map", subtitle = "Checksums and code, often") {
        regions.take(SHOWN_REGIONS).forEach { TmKeyValueRow(label = it.range.toString(), value = byteCount(it.range.size), numeric = true) }
        if (regions.size > SHOWN_REGIONS) {
            Text("…and ${regions.size - SHOWN_REGIONS} more", style = TmTheme.typography.caption, color = TmTheme.colors.textMuted)
        }
    }
}
