package components.hextexteditor.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraintsScope

fun getTextAlignmentLine(
    textWidth: Float,
    digitsPerLine: Int,
    bits: Int,
    totalWidth: Float
): Float {
    val lineWidth = ((textWidth * digitsPerLine) + (digitsPerLine / bits) * textWidth)
    return (totalWidth / 2) - (lineWidth / 2)
}

fun BoxWithConstraintsScope.mapLineRectData(
    input: String,
    textWidth: Float,
    textHeight: Float,
    textAlignmentLine: Float,
    digitsPerLine: Int,
    bits: Int,
    textPositionHandler: HexTextPositionHandler
) {
    var lineNum = 0
    var xLinePosition = textAlignmentLine
    var yLinePosition = 0f

    // Setup Line data in position handler
    input.forEachIndexed { index, _ ->
        // Check if we need to start a new line
        if (index % digitsPerLine == 0 && index != 0) {
            textPositionHandler.addLineData(
                lineNumber = lineNum,
                lineStartOffset = textAlignmentLine,
                lineTopOffset = yLinePosition,
                lineEndOffset = xLinePosition
            )

            lineNum++
            xLinePosition = textAlignmentLine
            yLinePosition += (textHeight + 2)
        } else if (index % bits == 0 && xLinePosition != textAlignmentLine) {
            xLinePosition += textWidth
        }

        if (index == input.length - 1) {
            textPositionHandler.addLineData(
                lineNumber = lineNum,
                lineStartOffset = textAlignmentLine,
                lineTopOffset = yLinePosition,
                lineEndOffset = xLinePosition + textWidth
            )
        }

        // advance the xPosition
        xLinePosition += textWidth
    }
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