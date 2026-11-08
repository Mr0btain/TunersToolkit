package org.mr0btain.tunerstoolkit.ui.checksum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.mr0btain.tunerstoolkit.core.checksum.CheckResult
import org.mr0btain.tunerstoolkit.core.checksum.ChecksumAlgorithm
import org.mr0btain.tunerstoolkit.core.checksum.ChecksumCheck
import org.mr0btain.tunerstoolkit.core.checksum.ChecksumModule
import org.mr0btain.tunerstoolkit.core.checksum.ChecksumStore
import org.mr0btain.tunerstoolkit.core.checksum.DefinedModule
import org.mr0btain.tunerstoolkit.core.checksum.ModuleStatus
import org.mr0btain.tunerstoolkit.core.checksum.StoreForm
import org.mr0btain.tunerstoolkit.core.checksum.TtSum
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.parseAddress
import org.mr0btain.tunerstoolkit.core.image.toHexAddress
import org.mr0btain.tunerstoolkit.ui.document.DocumentHandler
import org.mr0btain.tunerstoolkit.ui.files.rememberFileSaver
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
import org.mr0btain.uikit.feedback.TmNotice
import org.mr0btain.uikit.feedback.TmNoticeKind
import org.mr0btain.uikit.foundation.TmControlSize
import org.mr0btain.uikit.inputs.TmTextField
import org.mr0btain.uikit.selection.TmDropdown
import org.mr0btain.uikit.selection.TmSegmented
import org.mr0btain.uikit.theme.TmTheme
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.tt_download
import tunerstoolkit.composeapp.generated.resources.tt_plus
import tunerstoolkit.composeapp.generated.resources.tt_shield
import tunerstoolkit.composeapp.generated.resources.tt_upload
import tunerstoolkit.composeapp.generated.resources.tt_x

/**
 * The open image's checksums: which family module is in use (found from the image, or picked, or loaded from a
 * .ttsum), each check with what's stored and what the bytes give now, fixing them, and checks of your own
 */
@Composable
fun ChecksumsScreen(
    documents: DocumentHandler,
    checksums: ChecksumHandler,
    onOpenFiles: () -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (documents.document == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TmEmptyState(
                title = "No image open",
                message = "Open an image to check its checksums.",
                icon = Res.drawable.tt_shield,
                actionText = "Open a file",
                onAction = onOpenFiles
            )
        }
        return
    }
    val spacing = TmTheme.spacing
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }
    val picker = rememberFilePickerLauncher(type = FileKitType.File()) { file ->
        if (file == null) return@rememberFilePickerLauncher
        scope.launch { onMessage(checksums.load(file.readBytes(), file.name)) }
    }
    val saver = rememberFileSaver { name -> if (name != null) onMessage("Saved $name") }
    val state = checksums.state
    val active = checksums.active

    fun fix() {
        val report = checksums.fixAll(confirmedUnverified = true) ?: return
        onMessage(if (report.allOk) "Fixed ${report.fixed} checksums" else "${report.results.count { !it.ok }} checksums couldn't be fixed")
    }

    TmScrollColumn(modifier.fillMaxSize().padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        TmPageHeader(
            title = "Checksums",
            onBack = null,
            subtitle = when {
                state == null || checksums.working && state.checks.isEmpty() -> "Checking…"
                state.checks.isEmpty() -> "No checksums known for this image"
                state.wrong == 0 -> "All ${state.checks.size} are right"
                else -> "${state.wrong} of ${state.checks.size} are wrong"
            }
        )
        ModulePanel(checksums, active, onLoad = { picker.launch() }, onSave = { module ->
            saver.save(TtSum.write(module).encodeToByteArray(), module.id, "ttsum")
        })

        TmPanel(
            modifier = Modifier.fillMaxWidth(),
            title = "Checks",
            subtitle = "What's stored in the image, and what its bytes give now",
            icon = Res.drawable.tt_shield,
            actions = {
                TmButton(text = "Add a check", onClick = { adding = true }, variant = TmButtonVariant.SECONDARY, size = TmControlSize.SMALL, leadingIcon = Res.drawable.tt_plus)
                TmButton(
                    text = "Fix all",
                    onClick = { if (checksums.needsConfirmation) confirming = true else fix() },
                    enabled = (state?.wrong ?: 0) > 0,
                    size = TmControlSize.SMALL
                )
            }
        ) {
            state?.problem?.let { TmNotice(kind = TmNoticeKind.ERROR, text = it) }
            if (state == null || state.checks.isEmpty()) {
                Text(
                    "No module recognises this image. Pick one above, load a .ttsum, or add checks of your own.",
                    style = TmTheme.typography.body,
                    color = TmTheme.colors.textMuted
                )
            }
            state?.results?.forEach { result -> CheckRow(result, own = checksums.isOwn(result.check), onRemove = { checksums.removeOwn(result.check.name) }) }
        }
    }

    if (adding) AddCheckDialog(onAdd = { checksums.addOwn(it); adding = false }, onDismiss = { adding = false })
    if (confirming) {
        TmConfirmDialog(
            title = "Fix with checks that aren't verified?",
            message = "Some of these checks aren't from a verified module built into the app: they came from a file, a project, or were added by hand. A wrong checksum can stop an ECU starting, so keep the original and check the result before flashing.",
            confirmText = "Fix them",
            destructive = true,
            onConfirm = { confirming = false; fix() },
            onDismissRequest = { confirming = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModulePanel(checksums: ChecksumHandler, active: ChecksumModule?, onLoad: () -> Unit, onSave: (DefinedModule) -> Unit) {
    val detectedIds = checksums.detected.map { it.module.id }.toSet()
    TmPanel(
        modifier = Modifier.fillMaxWidth(),
        title = active?.name ?: "No module",
        subtitle = active?.let { listOf(it.family, it.description).filter(String::isNotBlank).joinToString(" · ") } ?: "None of the modules recognises this image",
        actions = {
            if (active != null) TmBadge(text = active.status.label, tone = when (active.status) {
                ModuleStatus.VERIFIED -> TmTheme.colors.success
                ModuleStatus.UNVERIFIED -> TmTheme.colors.warning
                ModuleStatus.USER -> TmTheme.colors.accent
            })
        }
    ) {
        if (active?.status == ModuleStatus.UNVERIFIED) {
            TmNotice(kind = TmNoticeKind.WARNING, text = "This module hasn't been checked against real binaries. Check what it fixes before flashing.")
        }
        active?.source?.takeIf { it.isNotBlank() }?.let { Text("From: $it", style = TmTheme.typography.caption, color = TmTheme.colors.textMuted) }
        checksums.detected.firstOrNull { it.module == active }?.let {
            Text("Recognised by ${it.detection.reason}", style = TmTheme.typography.caption, color = TmTheme.colors.textMuted)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm), itemVerticalAlignment = Alignment.Bottom) {
            // By id, with "" for the one found from the image
            val byId = checksums.modules.associateBy { it.id }
            TmDropdown(
                options = listOf("") + byId.keys,
                selected = checksums.chosen?.id ?: "",
                onSelect = { id -> checksums.choose(byId[id]) },
                optionLabel = { id ->
                    val module = byId[id]
                    when {
                        module == null -> "Found from the image"
                        module.id in detectedIds -> "${module.name} (recognised)"
                        else -> "${module.name}, ${module.family}"
                    }
                },
                label = "Module",
                modifier = Modifier.width(280.dp)
            )
            TmButton(text = "Load a .ttsum", onClick = onLoad, variant = TmButtonVariant.SECONDARY, leadingIcon = Res.drawable.tt_upload)
            (active as? DefinedModule)?.let { module ->
                TmButton(text = "Save as .ttsum", onClick = { onSave(module) }, variant = TmButtonVariant.GHOST, leadingIcon = Res.drawable.tt_download)
            }
            checksums.own?.let { own ->
                TmButton(text = "Save your checks", onClick = { onSave(own) }, variant = TmButtonVariant.GHOST, leadingIcon = Res.drawable.tt_download)
            }
        }
    }
}

@Composable
private fun CheckRow(result: CheckResult, own: Boolean, onRemove: () -> Unit) {
    val check = result.check
    val digits = check.store.size * 2
    fun hex(value: ULong?) = value?.let { "0x" + it.toString(16).uppercase().padStart(digits, '0') } ?: "–"
    val where = check.ranges.joinToString { "${it.start.toHexAddress()}–${(it.endExclusive - 1).toHexAddress()}" }
    val stored = when (check.store.form) {
        StoreForm.FORCE -> "must come to ${hex(result.stored)} (correction at ${check.store.address.toHexAddress()})"
        else -> "${check.store.form.label.lowercase()} at ${check.store.address.toHexAddress()}: ${hex(result.stored)}"
    }
    TmListRow(
        title = check.name + if (own) " (yours)" else "",
        subtitle = listOfNotNull(check.algorithm.label, where, stored, "now ${hex(result.computed)}", result.problem).joinToString(" · "),
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                when {
                    result.problem != null -> TmBadge(text = "Can't check", tone = TmTheme.colors.neutral)
                    result.ok -> TmBadge(text = "Right", tone = TmTheme.colors.success)
                    else -> TmBadge(text = "Wrong", tone = TmTheme.colors.danger)
                }
                if (own) TmIconButton(icon = Res.drawable.tt_x, contentDescription = "Remove ${check.name}", onClick = onRemove)
            }
        }
    )
}

/** The algorithms offered when adding a check by hand */
private val ALGORITHMS: List<ChecksumAlgorithm> = listOf(
    ChecksumAlgorithm.Sum(1, 8),
    ChecksumAlgorithm.Sum(1, 16),
    ChecksumAlgorithm.Sum(1, 32),
    ChecksumAlgorithm.Sum(2, 16, ByteOrder.BIG_ENDIAN),
    ChecksumAlgorithm.Sum(2, 16, ByteOrder.LITTLE_ENDIAN),
    ChecksumAlgorithm.Sum(2, 32, ByteOrder.BIG_ENDIAN),
    ChecksumAlgorithm.Sum(2, 32, ByteOrder.LITTLE_ENDIAN),
    ChecksumAlgorithm.Sum(4, 32, ByteOrder.BIG_ENDIAN),
    ChecksumAlgorithm.Sum(4, 32, ByteOrder.LITTLE_ENDIAN),
    ChecksumAlgorithm.Xor(1),
    ChecksumAlgorithm.Adler32
) + ChecksumAlgorithm.CRCS

/** A check of your own: the algorithm, the ranges, and where the result goes */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCheckDialog(onAdd: (ChecksumCheck) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var algorithm by remember { mutableStateOf(ALGORITHMS.first()) }
    var ranges by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("2") }
    var byteOrder by remember { mutableStateOf(ByteOrder.BIG_ENDIAN) }
    var form by remember { mutableStateOf(StoreForm.VALUE) }
    var target by remember { mutableStateOf("0x0") }

    val parsedRanges = ranges.split(',', ';', '\n').map { it.trim() }.filter { it.isNotEmpty() }.map { text ->
        val parts = text.split('-', '–').map { it.trim() }
        val first = parts.getOrNull(0)?.let(::parseAddress)
        val last = parts.getOrNull(1)?.let(::parseAddress)
        if (parts.size == 2 && first != null && last != null && last >= first) org.mr0btain.tunerstoolkit.core.image.AddressRange(first, last + 1) else null
    }
    val storeAddress = parseAddress(address)
    val storeSize = size.trim().toIntOrNull()?.takeIf { it in 1..8 }
    val targetValue = target.trim().removePrefix("0x").removePrefix("0X").toULongOrNull(16)
    val check = if (name.isNotBlank() && parsedRanges.isNotEmpty() && parsedRanges.all { it != null } && storeAddress != null && storeSize != null && targetValue != null) {
        ChecksumCheck(name.trim(), algorithm, parsedRanges.filterNotNull(), ChecksumStore(storeAddress, storeSize, byteOrder, form, targetValue))
    } else {
        null
    }

    TmDialog(
        title = "Add a check",
        description = "Saved with the project, alongside the module's",
        onDismissRequest = onDismiss,
        actions = { TmButtonRow(confirmText = "Add it", onConfirm = { check?.let(onAdd) }, onCancel = onDismiss, confirmEnabled = check != null) }
    ) {
        TmTextField(value = name, onValueChange = { name = it }, label = "Name")
        TmDropdown(options = ALGORITHMS, selected = algorithm, onSelect = { algorithm = it }, optionLabel = { it.label }, label = "Algorithm")
        TmTextField(
            value = ranges,
            onValueChange = { ranges = it },
            label = "Ranges",
            placeholder = "0x804000-0x807FFB, 0x810000-0x81FFFF",
            supportingText = "First and last address of each, both included",
            error = if (ranges.isNotBlank() && parsedRanges.any { it == null }) "That isn't a list of ranges" else null
        )
        TmSegmented(options = StoreForm.entries, selected = form, onSelect = { form = it }, optionLabel = { it.label }, size = TmControlSize.SMALL)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm), verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm)) {
            TmTextField(value = address, onValueChange = { address = it }, label = if (form == StoreForm.FORCE) "Correction at" else "Stored at", modifier = Modifier.width(160.dp))
            TmTextField(value = size, onValueChange = { size = it }, label = "Bytes", modifier = Modifier.width(80.dp))
            TmSegmented(options = ByteOrder.entries, selected = byteOrder, onSelect = { byteOrder = it }, optionLabel = { if (it == ByteOrder.LITTLE_ENDIAN) "LSB first" else "MSB first" }, size = TmControlSize.SMALL)
            if (form == StoreForm.FORCE) TmTextField(value = target, onValueChange = { target = it }, label = "Target (hex)", modifier = Modifier.width(140.dp))
        }
    }
}
