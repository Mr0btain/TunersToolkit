package org.mr0btain.tunerstoolkit.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.mr0btain.uikit.containers.TmListRow
import org.mr0btain.uikit.containers.TmPanel
import org.mr0btain.uikit.containers.TmSwitchRow
import org.mr0btain.uikit.foundation.TmDivider
import org.mr0btain.uikit.foundation.tmClickable
import org.mr0btain.uikit.selection.TmSegmented
import org.mr0btain.uikit.theme.TmFont
import org.mr0btain.uikit.theme.TmFonts
import org.mr0btain.uikit.theme.TmPalette
import org.mr0btain.uikit.theme.TmPalettes
import org.mr0btain.uikit.theme.TmTheme
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.tt_chevron_right
import tunerstoolkit.composeapp.generated.resources.tt_droplet
import tunerstoolkit.composeapp.generated.resources.tt_layers

/**
 * How the app looks, the way TicketMan does it: each theme as a card drawn in its own colors (so you see what you'll
 * get before picking it), dark or light, each font as a sample, then transparency and the component gallery
 */
@Composable
internal fun AppearancePanel(settings: SettingsHandler, onOpenGallery: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = TmTheme.spacing
    val data = settings.data

    TmPanel(modifier = modifier.fillMaxWidth(), title = "Appearance", subtitle = "Only changes this device") {
        FieldLabel("Theme")
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Side by side when there's room for all three, one under another on a phone
            if (maxWidth >= THEMES_IN_A_ROW) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    TmPalettes.all.forEach { palette ->
                        ThemeCard(palette, data.darkMode, palette.id == settings.palette.id, { settings.setPalette(palette) }, Modifier.weight(1f))
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    TmPalettes.all.forEach { palette ->
                        ThemeCard(palette, data.darkMode, palette.id == settings.palette.id, { settings.setPalette(palette) }, Modifier.fillMaxWidth())
                    }
                }
            }
        }

        TmSegmented(
            label = "Mode",
            options = listOf(true, false),
            selected = data.darkMode,
            onSelect = settings::setDarkMode,
            optionLabel = { if (it) "Dark" else "Light" }
        )

        FieldLabel("Font")
        Column {
            TmFonts.all.forEach { font -> FontRow(font, font.id == settings.font.id) { settings.setFont(font) } }
        }

        TmDivider()
        TmSwitchRow(
            title = "Reduce transparency",
            subtitle = "Draws floating bars and dialogs as solid, easier to read and lighter on slower machines",
            icon = Res.drawable.tt_droplet,
            checked = data.reduceTransparency,
            onCheckedChange = settings::setReduceTransparency
        )
        TmListRow(
            title = "Component gallery",
            subtitle = "Every button, field, badge and map view in the current theme",
            icon = Res.drawable.tt_layers,
            trailing = {
                Icon(painterResource(Res.drawable.tt_chevron_right), contentDescription = null, tint = TmTheme.colors.textMuted, modifier = Modifier.size(16.dp))
            },
            onClick = onOpenGallery
        )
    }
}

/** From this width the three theme cards sit in a row */
private val THEMES_IN_A_ROW = 560.dp

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = TmTheme.typography.caption, color = TmTheme.colors.textMuted)
}

/**
 * A theme as a card in its own colors (dark or light, as picked): a small sample of a screen (a card with a line of
 * text, the accent as a button and the warning, danger and success dots) over its name and description
 */
@Composable
private fun ThemeCard(palette: TmPalette, dark: Boolean, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val own = palette.colors(dark)
    val spacing = TmTheme.spacing
    val shape = TmTheme.shapes.panel

    Column(
        modifier
            .background(own.surface, shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) own.accent.color else own.hairline, shape)
            .tmClickable(onClick = onClick, shape = shape, role = Role.RadioButton, overlay = own.text)
            .padding(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(88.dp)
                .background(own.background, TmTheme.shapes.control)
                .padding(spacing.md),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(own.surfaceRaised, TmTheme.shapes.chip)
                    .border(1.dp, own.hairline, TmTheme.shapes.chip)
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Box(Modifier.width(72.dp).height(6.dp).background(own.textMuted, TmTheme.shapes.pill))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(44.dp).height(16.dp).background(own.accent.color, TmTheme.shapes.pill))
                    Box(Modifier.width(2.dp))
                    listOf(own.warning.color, own.danger.color, own.success.color).forEach { dot ->
                        Box(Modifier.size(8.dp).background(dot, TmTheme.shapes.pill))
                    }
                }
            }
        }
        Column(Modifier.padding(horizontal = spacing.xs).padding(bottom = spacing.xs)) {
            Text(palette.name, style = TmTheme.typography.subtitle, color = own.text)
            Text(palette.description, style = TmTheme.typography.caption, color = own.textMuted)
        }
    }
}

/** A font as a row, with "Ag" written in it */
@Composable
private fun FontRow(font: TmFont, selected: Boolean, onClick: () -> Unit) {
    val sample = TmTheme.typography.title.copy(fontFamily = font.family())
    TmListRow(
        title = font.name,
        subtitle = font.description,
        selected = selected,
        leading = { Text("Ag", style = sample, color = TmTheme.colors.text, modifier = Modifier.width(40.dp)) },
        onClick = onClick
    )
}
