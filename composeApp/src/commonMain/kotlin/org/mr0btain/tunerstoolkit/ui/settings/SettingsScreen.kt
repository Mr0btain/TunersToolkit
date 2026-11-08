package org.mr0btain.tunerstoolkit.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.mr0btain.tunerstoolkit.core.BuildInfo
import org.mr0btain.uikit.buttons.TmButton
import org.mr0btain.uikit.buttons.TmButtonVariant
import org.mr0btain.uikit.containers.TmPageHeader
import org.mr0btain.uikit.containers.TmPanel
import org.mr0btain.uikit.containers.TmScrollColumn
import org.mr0btain.uikit.containers.TmSwitchRow
import org.mr0btain.uikit.data.TmKeyValueRow
import org.mr0btain.uikit.selection.TmSegmented
import org.mr0btain.uikit.theme.TmFonts
import org.mr0btain.uikit.theme.TmPalettes
import org.mr0btain.uikit.theme.TmTheme
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.tt_grid
import tunerstoolkit.composeapp.generated.resources.tt_info
import tunerstoolkit.composeapp.generated.resources.tt_sliders

@Composable
fun SettingsScreen(settings: SettingsHandler, onOpenGallery: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = TmTheme.spacing
    val data = settings.data

    TmScrollColumn(
        modifier = modifier.fillMaxSize().padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg)
    ) {
        TmPageHeader(title = "Settings", onBack = null)

        TmPanel(modifier = Modifier.fillMaxWidth(), title = "Appearance", icon = Res.drawable.tt_sliders) {
            TmSegmented(label = "Theme", options = TmPalettes.all, selected = settings.palette, onSelect = settings::setPalette, optionLabel = { it.name })
            TmSegmented(label = "Font", options = TmFonts.all, selected = settings.font, onSelect = settings::setFont, optionLabel = { it.name })
            TmSwitchRow(title = "Dark mode", checked = data.darkMode, onCheckedChange = settings::setDarkMode)
            TmSwitchRow(
                title = "Reduce transparency",
                subtitle = "Solid bars and dialogs instead of frosted glass",
                checked = data.reduceTransparency,
                onCheckedChange = settings::setReduceTransparency
            )
            TmButton(text = "Component gallery", onClick = onOpenGallery, variant = TmButtonVariant.SECONDARY, leadingIcon = Res.drawable.tt_grid)
        }

        TmPanel(modifier = Modifier.fillMaxWidth(), title = "Hex view", icon = Res.drawable.tt_grid) {
            TmSegmented(
                label = "Bytes per row",
                options = listOf(8, 16, 32),
                selected = data.hexBytesPerRow,
                onSelect = settings::setHexBytesPerRow,
                optionLabel = { it.toString() }
            )
        }

        TmPanel(modifier = Modifier.fillMaxWidth(), title = "About", icon = Res.drawable.tt_info) {
            TmKeyValueRow(label = "Version", value = BuildInfo.VERSION, numeric = true)
        }
    }
}
