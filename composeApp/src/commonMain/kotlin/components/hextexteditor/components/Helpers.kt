package components.hextexteditor.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier

fun getTextAlignmentLine(
    textWidth: Float,
    digitsPerLine: Int,
    bits: Int,
    totalWidth: Float
): Float {
    val lineWidth = ((textWidth * digitsPerLine) + (digitsPerLine / bits) * textWidth)
    return (totalWidth / 2) - (lineWidth / 2)
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.hexTextOnClick(): Modifier {
    return this.combinedClickable(
        onClick = {
            // Set up cursor and edit logic when clicked
        },
        onDoubleClick = {
            // Implement double click logic, not sure what to have it do?
        },
        onLongClick = {
            // Implement long click, for selecting text
        }
    )
}