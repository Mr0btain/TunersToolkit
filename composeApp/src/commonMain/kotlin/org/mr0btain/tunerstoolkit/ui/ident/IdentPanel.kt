package org.mr0btain.tunerstoolkit.ui.ident

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.mr0btain.tunerstoolkit.core.image.toHexAddress
import org.mr0btain.tunerstoolkit.ui.project.ProjectHandler
import org.mr0btain.uikit.buttons.TmButton
import org.mr0btain.uikit.buttons.TmButtonVariant
import org.mr0btain.uikit.containers.TmListRow
import org.mr0btain.uikit.containers.TmPanel
import org.mr0btain.uikit.feedback.TmBadge
import org.mr0btain.uikit.foundation.TmControlSize
import org.mr0btain.uikit.theme.TmTheme
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.tt_info

/** Most findings listed; a big image can hold many copies of text that looks like numbers */
private const val SHOWN = 30

/**
 * What the open image says it is: hardware and software numbers, ECU family, calibration ID, each with where it is
 * and whose numbering it follows. With a project open, it can fill in the project's ECU and software number
 */
@Composable
fun IdentPanel(ident: IdentHandler, projects: ProjectHandler) {
    val findings = ident.findings
    val project = projects.current
    TmPanel(
        modifier = Modifier.fillMaxWidth(),
        title = "What the image says it is",
        subtitle = when {
            ident.scanning -> "Reading…"
            findings.isEmpty() -> "No identification found in its text"
            else -> "From the text in the image; surest first"
        },
        icon = Res.drawable.tt_info,
        actions = {
            if (project != null && findings.isNotEmpty()) {
                val suggested = ident.suggest(project.manifest.info)
                TmButton(
                    text = "Fill in the project",
                    onClick = { projects.rename(project.manifest.id, project.manifest.name, suggested) },
                    enabled = suggested != project.manifest.info,
                    variant = TmButtonVariant.SECONDARY,
                    size = TmControlSize.SMALL
                )
            }
        }
    ) {
        findings.take(SHOWN).forEach { finding ->
            TmListRow(
                title = finding.value,
                subtitle = listOfNotNull(
                    finding.maker,
                    "at ${finding.address.toHexAddress()}",
                    if (finding.count > 1) "${finding.count} times" else null,
                    finding.note
                ).joinToString(" · "),
                trailing = {
                    TmBadge(text = finding.kind.label, tone = if (finding.confidence >= 70) TmTheme.colors.accent else TmTheme.colors.neutral)
                }
            )
        }
        if (findings.size > SHOWN) Text("…and ${findings.size - SHOWN} more", style = TmTheme.typography.caption, color = TmTheme.colors.textMuted)
    }
}
