package components.hextexteditor.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

fun getTextAlignmentLine(
    textWidth: Float,
    digitsPerLine: Int,
    bits: Int,
    totalWidth: Float
): Float {
    val lineWidth = ((textWidth * digitsPerLine) + (digitsPerLine / bits) * textWidth)
    return (totalWidth / 2) - (lineWidth / 2)
}

fun mapLineRectData(
    input: String,
    textWidth: Float,
    textHeight: Float,
    textAlignmentLine: Float,
    digitsPerLine: Int,
    bits: Int,
    textPositionHandler: HexTextEditorManager
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

fun Modifier.hexTextOnClick(
    manager: HexTextEditorManager,
    isCursorVisible: MutableState<Boolean>,
    cursorPosition: MutableState<Offset>
): Modifier {
    return this.pointerInput(Unit) {
        detectTapGestures(
            onTap = { offset ->

                val characterPos = manager.getCharacterRectFromOffset(offset)
                val characterHalfPos = ((characterPos.right - characterPos.left) / 2) + characterPos.left

                // Check whether we should position the cursor on the left or the right of the character
                // by checking its x position relative to the center of the character
                if (offset.x <= characterHalfPos) {
                    cursorPosition.value = Offset(x = characterPos.left, y = characterPos.top)
                } else {
                    cursorPosition.value = Offset(x = characterPos.right, y = characterPos.top)
                }

                // Check if we should display cursor if position is blank hide it
                if (cursorPosition.value.x == 0f && cursorPosition.value.y == 0f) {
                    isCursorVisible.value = false
                } else {
                    isCursorVisible.value = true
                }
            }
        )
    }
}