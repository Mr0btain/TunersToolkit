package org.mr0btain.tunerstoolkit.ui.versions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.mr0btain.tunerstoolkit.core.project.ProjectManifest
import org.mr0btain.tunerstoolkit.core.project.ProjectVersion
import org.mr0btain.tunerstoolkit.ui.definition.DefinitionHandler
import org.mr0btain.tunerstoolkit.ui.ident.IdentHandler
import org.mr0btain.tunerstoolkit.ui.kit.hex.byteCount
import org.mr0btain.tunerstoolkit.ui.project.OpenProject
import org.mr0btain.tunerstoolkit.ui.project.ProjectHandler
import org.mr0btain.tunerstoolkit.ui.project.timeAgo
import org.mr0btain.uikit.buttons.TmButton
import org.mr0btain.uikit.buttons.TmButtonRow
import org.mr0btain.uikit.buttons.TmButtonVariant
import org.mr0btain.uikit.buttons.TmIconButton
import org.mr0btain.uikit.containers.TmConfirmDialog
import org.mr0btain.uikit.containers.TmDialog
import org.mr0btain.uikit.containers.TmListRow
import org.mr0btain.uikit.containers.TmPageHeader
import org.mr0btain.uikit.containers.TmPanel
import org.mr0btain.uikit.containers.TmScrollColumn
import org.mr0btain.uikit.feedback.TmBadge
import org.mr0btain.uikit.feedback.TmEmptyState
import org.mr0btain.uikit.inputs.TmTextField
import org.mr0btain.uikit.theme.TmTheme
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.tt_compare
import tunerstoolkit.composeapp.generated.resources.tt_file
import tunerstoolkit.composeapp.generated.resources.tt_folder_plus
import tunerstoolkit.composeapp.generated.resources.tt_history
import tunerstoolkit.composeapp.generated.resources.tt_info
import tunerstoolkit.composeapp.generated.resources.tt_sliders
import tunerstoolkit.composeapp.generated.resources.tt_x

/** A version as the timeline lists it: the original is one too */
private class Entry(val id: String, val name: String, val subtitle: String, val note: String)

/**
 * The open project's versions, newest first, down to the original: open one, compare it with what's open, rename it
 * or change its note, start a new version from it, or delete it. Above them, what the project is for
 */
@Composable
fun VersionsScreen(
    projects: ProjectHandler,
    definitions: DefinitionHandler,
    onOpenFiles: () -> Unit,
    onOpenEditor: () -> Unit,
    onCompare: (ProjectManifest, String) -> Unit,
    modifier: Modifier = Modifier,
    ident: IdentHandler? = null
) {
    val open = projects.current
    if (open == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TmEmptyState(
                title = "No project open",
                message = "Versions belong to a project. Open one from the files screen, or save the open image as one.",
                icon = Res.drawable.tt_history,
                actionText = "Open a project",
                onAction = onOpenFiles
            )
        }
        return
    }
    val spacing = TmTheme.spacing
    val manifest = open.manifest
    var editing by remember { mutableStateOf<Entry?>(null) }
    var branching by remember { mutableStateOf<Entry?>(null) }
    var deleting by remember { mutableStateOf<Entry?>(null) }

    TmScrollColumn(modifier.fillMaxSize().padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        TmPageHeader(
            title = manifest.name,
            onBack = null,
            subtitle = listOfNotNull(manifest.source.fileName, manifest.definition?.fileName, "${manifest.versions.size + 1} versions").joinToString(" · ")
        )
        ProjectInfoPanel(projects, definitions, ident, open)

        TmPanel(modifier = Modifier.fillMaxWidth(), title = "Versions", subtitle = "Newest first. Each is its changes from the original", icon = Res.drawable.tt_history) {
            entries(manifest).forEach { entry ->
                val isOpen = open.versionId == entry.id
                TmListRow(
                    title = entry.name,
                    subtitle = listOf(entry.subtitle, entry.note).filter { it.isNotBlank() }.joinToString(" · "),
                    icon = Res.drawable.tt_file,
                    selected = isOpen,
                    onClick = { if (!isOpen) projects.openProject(manifest, entry.id, onOpenEditor) else onOpenEditor() },
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xxs), verticalAlignment = Alignment.CenterVertically) {
                            if (isOpen) TmBadge(text = "Open", tone = TmTheme.colors.accent)
                            if (!isOpen) TmIconButton(icon = Res.drawable.tt_compare, contentDescription = "Compare ${entry.name} with what's open", onClick = { onCompare(manifest, entry.id) })
                            TmIconButton(icon = Res.drawable.tt_folder_plus, contentDescription = "Start a new version from ${entry.name}", onClick = { branching = entry })
                            if (entry.id != ProjectManifest.ORIGINAL) {
                                TmIconButton(icon = Res.drawable.tt_sliders, contentDescription = "Rename ${entry.name}", onClick = { editing = entry })
                                TmIconButton(icon = Res.drawable.tt_x, contentDescription = "Delete ${entry.name}", onClick = { deleting = entry })
                            }
                        }
                    }
                )
            }
        }
    }

    editing?.let { entry ->
        VersionDialog(
            title = "Rename ${entry.name}",
            confirm = "Save",
            name = entry.name,
            note = entry.note,
            onConfirm = { name, note -> projects.updateVersionInfo(entry.id, name, note); editing = null },
            onDismiss = { editing = null }
        )
    }
    branching?.let { entry ->
        VersionDialog(
            title = "New version from ${entry.name}",
            description = "It starts with ${entry.name}'s changes and opens in the editor. ${entry.name} stays as it is",
            confirm = "Start it",
            name = "Version ${manifest.versions.size + 1}",
            note = "",
            onConfirm = { name, note -> projects.branch(entry.id, name, note, onOpenEditor); branching = null },
            onDismiss = { branching = null }
        )
    }
    deleting?.let { entry ->
        TmConfirmDialog(
            title = "Delete ${entry.name}?",
            message = "Its changes are deleted from ${manifest.name}. The other versions stay.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = { projects.deleteVersion(manifest.id, entry.id); deleting = null },
            onDismissRequest = { deleting = null }
        )
    }
}

private fun entries(manifest: ProjectManifest): List<Entry> {
    fun name(id: String) = manifest.version(id)?.name ?: "the original"
    val versions = manifest.versions.sortedByDescending { it.createdAt }.map { version: ProjectVersion ->
        Entry(
            version.id,
            version.name,
            listOf("${byteCount(version.changedBytes)} changed", "from ${name(version.basedOn)}", "saved ${timeAgo(version.updatedAt)}").joinToString(" · "),
            version.note
        )
    }
    return versions + Entry(ProjectManifest.ORIGINAL, "Original", "As it was read, ${byteCount(manifest.source.size)}, ${timeAgo(manifest.createdAt)}", "")
}

/** What the project is for: vehicle, ECU, software number and notes, which the A2L can fill in */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectInfoPanel(projects: ProjectHandler, definitions: DefinitionHandler, ident: IdentHandler?, open: OpenProject) {
    val manifest = open.manifest
    var name by remember(manifest.id, manifest.name) { mutableStateOf(manifest.name) }
    var info by remember(manifest.id, manifest.info) { mutableStateOf(manifest.info) }
    val changed = name != manifest.name || info != manifest.info
    val modPar = definitions.definition?.a2l?.module?.modPar
    TmPanel(modifier = Modifier.fillMaxWidth(), title = "Project", icon = Res.drawable.tt_info, actions = {
        if (ident != null && ident.findings.isNotEmpty()) {
            TmButton(
                text = "Fill in from the image",
                onClick = { info = ident.suggest(info) },
                variant = TmButtonVariant.GHOST,
                size = org.mr0btain.uikit.foundation.TmControlSize.SMALL
            )
        }
        if (modPar != null) {
            TmButton(
                text = "Fill in from the A2L",
                onClick = {
                    info = info.copy(
                        ecu = info.ecu.ifBlank { modPar.ecu ?: modPar.cpuType.orEmpty() },
                        softwareNumber = info.softwareNumber.ifBlank { modPar.epk ?: modPar.version.orEmpty() }
                    )
                },
                variant = TmButtonVariant.GHOST,
                size = org.mr0btain.uikit.foundation.TmControlSize.SMALL
            )
        }
    }) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.md), verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm)) {
            val field = Modifier.widthIn(min = 220.dp, max = 360.dp)
            TmTextField(value = name, onValueChange = { name = it }, label = "Name", modifier = field)
            TmTextField(value = info.vehicle, onValueChange = { info = info.copy(vehicle = it) }, label = "Vehicle", placeholder = "Golf VII 2.0 TSI", modifier = field)
            TmTextField(value = info.ecu, onValueChange = { info = info.copy(ecu = it) }, label = "ECU", placeholder = "MED17.5", modifier = field)
            TmTextField(value = info.softwareNumber, onValueChange = { info = info.copy(softwareNumber = it) }, label = "Software number", modifier = field)
        }
        TmTextField(value = info.notes, onValueChange = { info = info.copy(notes = it) }, label = "Notes", modifier = Modifier.fillMaxWidth())
        TmButton(text = "Save", onClick = { projects.rename(manifest.id, name, info) }, enabled = changed && name.isNotBlank())
    }
}

@Composable
private fun VersionDialog(
    title: String,
    confirm: String,
    name: String,
    note: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
    description: String? = null
) {
    var newName by remember { mutableStateOf(name) }
    var newNote by remember { mutableStateOf(note) }
    TmDialog(
        title = title,
        description = description,
        onDismissRequest = onDismiss,
        actions = { TmButtonRow(confirmText = confirm, onConfirm = { onConfirm(newName, newNote) }, onCancel = onDismiss, confirmEnabled = newName.isNotBlank()) }
    ) {
        TmTextField(value = newName, onValueChange = { newName = it }, label = "Version")
        TmTextField(value = newNote, onValueChange = { newNote = it }, label = "Note", placeholder = "What changed, optional")
    }
}
