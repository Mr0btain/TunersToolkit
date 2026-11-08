package org.mr0btain.tunerstoolkit.ui.kit.tooltip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.mr0btain.uikit.buttons.TmButtonVariant
import org.mr0btain.uikit.buttons.TmIconButton
import org.mr0btain.uikit.foundation.TmControlSize
import org.mr0btain.uikit.theme.TmTheme
import androidx.compose.ui.graphics.Color

/**
 * Shows [text] in a small bubble above [content] while the mouse rests on it (or on a long press on a touch screen).
 * Null or blank [text] shows nothing, so a tip can be turned off without changing the layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TmTooltip(text: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (text.isNullOrBlank()) {
        content()
        return
    }
    val colors = TmTheme.colors
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above, 4.dp),
        tooltip = {
            PlainTooltip(
                maxWidth = 320.dp,
                modifier = Modifier.border(1.dp, colors.outline, TmTheme.shapes.chip),
                shape = TmTheme.shapes.chip,
                containerColor = colors.surfaceRaised,
                contentColor = colors.text
            ) {
                Text(text, style = TmTheme.typography.caption, color = colors.text, modifier = Modifier.padding(vertical = 2.dp))
            }
        },
        state = rememberTooltipState(),
        modifier = modifier,
        content = content
    )
}

/** The kit's icon button, with what it does ([contentDescription]) shown as a tooltip on hover */
@Composable
fun TmTipButton(
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: TmButtonVariant = TmButtonVariant.GHOST,
    size: TmControlSize = TmControlSize.MEDIUM,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified
) {
    TmTooltip(contentDescription, modifier) {
        TmIconButton(icon = icon, contentDescription = contentDescription, onClick = onClick, variant = variant, size = size, enabled = enabled, tint = tint)
    }
}

/**
 * A tooltip by the mouse at [at] (in the parent's coordinates), for things drawn on a Canvas that have no
 * composable of their own to hang a [TmTooltip] on, like hex bytes and map cells
 */
@Composable
fun TmPointerTooltip(text: String, at: Offset) {
    val colors = TmTheme.colors
    val offset = with(LocalDensity.current) { IntOffset((at.x + 12.dp.toPx()).toInt(), (at.y + 18.dp.toPx()).toInt()) }
    Popup(offset = offset, properties = PopupProperties(focusable = false)) {
        Text(
            text = text,
            style = TmTheme.typography.caption,
            color = colors.text,
            modifier = Modifier
                .widthIn(max = 360.dp)
                .background(colors.surfaceRaised, TmTheme.shapes.chip)
                .border(1.dp, colors.outline, TmTheme.shapes.chip)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Follows the mouse over this element: [onMove] gets where it is, or null when it leaves, a button is down or the
 * pointer isn't a mouse. Does nothing when not [enabled]
 */
fun Modifier.trackMouse(enabled: Boolean, onMove: (Offset?) -> Unit): Modifier =
    if (!enabled) this else pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: continue
                onMove(
                    when {
                        change.type != PointerType.Mouse -> null
                        event.type == PointerEventType.Exit || change.pressed -> null
                        else -> change.position
                    }
                )
            }
        }
    }

/** How long the mouse rests on something before its [TmPointerTooltip] shows */
const val TM_HOVER_DELAY_MS = 500L
