package org.mr0btain.tunerstoolkit.ui.kit.tabs

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.mr0btain.tunerstoolkit.ui.kit.tooltip.TmTooltip
import org.mr0btain.uikit.foundation.TmDivider
import org.mr0btain.uikit.foundation.tmClickable
import org.mr0btain.uikit.theme.TmTheme
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.tt_x

/** One tab of [TmClosableTabs] */
data class TmTab(
    val title: String,
    /** Shows a small X on the tab that calls onClose */
    val closable: Boolean = false,
    /** Shown on hover, like a map's whole name and description; none when null */
    val tooltip: String? = null
)

/**
 * The kit's tabs (an accent line under the open one), where a tab can have a small X to close it straight from the
 * tab, like the editor's open maps. Long titles are cut short; give the tab a [TmTab.tooltip] to show it all
 */
@Composable
fun TmClosableTabs(
    tabs: List<TmTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TmTheme.colors
    val spacing = TmTheme.spacing

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedIndex
                val line by animateColorAsState(if (isSelected) colors.accent.color else Color.Transparent)

                TmTooltip(tab.tooltip) {
                    Column(
                        modifier = Modifier.tmClickable(onClick = { onSelect(index) }, shape = TmTheme.shapes.chip, role = Role.Tab),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.padding(start = spacing.md, end = if (tab.closable) spacing.xs else spacing.md, top = spacing.sm, bottom = spacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tab.title,
                                style = TmTheme.typography.label,
                                color = if (isSelected) colors.text else colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 220.dp)
                            )
                            if (tab.closable) {
                                Box(
                                    Modifier
                                        .size(18.dp)
                                        .tmClickable(onClick = { onClose(index) }, shape = TmTheme.shapes.chip, overlay = colors.text),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.tt_x),
                                        contentDescription = "Close ${tab.title}",
                                        tint = if (isSelected) colors.textMuted else colors.textFaint,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        Box(
                            Modifier
                                .padding(horizontal = spacing.sm)
                                .height(2.dp)
                                .width(24.dp)
                                .background(line, TmTheme.shapes.pill)
                        )
                    }
                }
            }
        }
        TmDivider()
    }
}
