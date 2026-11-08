package org.mr0btain.tunerstoolkit.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.calibration.ObjectTree
import org.mr0btain.tunerstoolkit.core.calibration.TreeRow
import org.mr0btain.tunerstoolkit.ui.definition.OpenDefinition
import org.mr0btain.uikit.buttons.TmButton
import org.mr0btain.uikit.buttons.TmIconButton
import org.mr0btain.uikit.foundation.TmControlSize
import org.mr0btain.uikit.search.TmSearchField
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.tt_plus
import org.mr0btain.uikit.selection.TmSegmented
import org.mr0btain.uikit.theme.TmTheme

/**
 * The definition's objects as a tree by FUNCTION or GROUP (or all in one list), with a search over names and
 * descriptions. Clicking an object opens it
 */
@Composable
internal fun MapList(definition: OpenDefinition?, editor: EditorHandler, onOpen: (String) -> Unit, onNew: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = TmTheme.spacing
    if (definition == null) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text("No definition", style = TmTheme.typography.subtitle, color = TmTheme.colors.text)
            Text("Open an A2L, XDF or .ttdef on the files screen, or make a map here from its address.", style = TmTheme.typography.body, color = TmTheme.colors.textMuted)
            TmButton(text = "New map", onClick = onNew, leadingIcon = Res.drawable.tt_plus, size = TmControlSize.SMALL)
        }
        return
    }
    val set = definition.set
    val groupings = definition.groupings
    val choices = groupings.map { it.label } + EditorHandler.ALL
    val chosen = editor.listGrouping?.takeIf { it in choices } ?: choices.first()
    val roots = remember(definition, chosen) {
        groupings.firstOrNull { it.label == chosen }?.folders ?: listOf(ObjectTree.all(set))
    }
    // With everything in one folder, it's open
    val expanded = if (chosen == EditorHandler.ALL) editor.expanded + roots.map { it.name } else editor.expanded
    val rows = remember(roots, expanded, editor.query) { ObjectTree.rows(roots, set, expanded, editor.query) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            TmSearchField(query = editor.query, onQueryChange = { editor.query = it }, placeholder = "Find a map", modifier = Modifier.weight(1f))
            TmIconButton(icon = Res.drawable.tt_plus, contentDescription = "New map", onClick = onNew)
        }
        if (choices.size > 1) TmSegmented(options = choices, selected = chosen, onSelect = { editor.listGrouping = it }, optionLabel = { it })
        if (rows.isEmpty()) {
            Text(
                if (editor.query.isBlank()) "Nothing to list" else "Nothing matches \"${editor.query}\"",
                style = TmTheme.typography.body,
                color = TmTheme.colors.textMuted
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(rows, key = { row -> if (row is TreeRow.Object) "o:${row.path}/${row.obj.name}" else "f:${(row as TreeRow.Folder).path}" }) { row ->
                when (row) {
                    is TreeRow.Folder -> FolderRow(row, onClick = { editor.toggle(row.path) })
                    is TreeRow.Object -> ObjectRow(row, selected = row.obj.name == editor.selectedObject, onClick = { onOpen(row.obj.name) })
                }
            }
        }
    }
}

@Composable
private fun FolderRow(row: TreeRow.Folder, onClick: () -> Unit) {
    val colors = TmTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = TmTheme.spacing.md * row.depth, top = TmTheme.spacing.xs, bottom = TmTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (row.expanded) "▾" else "▸", style = TmTheme.typography.body, color = colors.textMuted)
        Text(row.folder.name, style = TmTheme.typography.subtitle, color = colors.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(row.count.toString(), style = TmTheme.typography.caption, color = colors.textMuted)
    }
}

@Composable
private fun ObjectRow(row: TreeRow.Object, selected: Boolean, onClick: () -> Unit) {
    val colors = TmTheme.colors
    val obj = row.obj
    Column(
        Modifier
            .fillMaxWidth()
            .background(if (selected) colors.accent.soft else colors.surface.copy(alpha = 0f), TmTheme.shapes.control)
            .clickable(onClick = onClick)
            .padding(start = TmTheme.spacing.md * row.depth + TmTheme.spacing.sm, top = TmTheme.spacing.xxs, bottom = TmTheme.spacing.xxs, end = TmTheme.spacing.xs)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Text(kindLabel(obj.kind), style = TmTheme.typography.caption, color = colors.accent.color)
            Text(obj.displayName ?: obj.name, style = TmTheme.typography.body, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (obj.description.isNotBlank()) {
            Text(obj.description, style = TmTheme.typography.caption, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A short tag for an object's kind */
internal fun kindLabel(kind: ObjectKind): String = when (kind) {
    ObjectKind.VALUE -> "VAL"
    ObjectKind.CURVE -> "CUR"
    ObjectKind.MAP -> "MAP"
    ObjectKind.CUBOID -> "3D"
    ObjectKind.CUBE_4 -> "4D"
    ObjectKind.CUBE_5 -> "5D"
    ObjectKind.VAL_BLK -> "BLK"
    ObjectKind.ASCII -> "TXT"
    ObjectKind.AXIS -> "AX"
}
