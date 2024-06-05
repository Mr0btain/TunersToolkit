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

fun String.formatHexStringForEditor(
    bits: Int,
    digitsPerLine: Int
): String {
    val actualHexCharsPerLine = bits * digitsPerLine
    val result = StringBuilder()

    for (i in this.indices) {
        if (i % bits == 0 && i != 0 && i % actualHexCharsPerLine != 0) {
            result.append(' ')
        }
        if (i % (actualHexCharsPerLine) == 0 && i != 0) {
            result.append('\n')
        }
        result.append(this[i])
    }

    return result.toString()
}

@OptIn(ExperimentalStdlibApi::class)
fun String.getOffsetsForHexEditor(digitsPer: Int): String {
    val lines = this.length / (digitsPer * 2) // split the input string into lines
    val byteLocations = mutableListOf<String>() // create an empty list to store byte locations
    var offset = 0 // initialize the offset to 0

    for (line in 0..lines) {
        byteLocations.add(offset.toHexString(HexFormat.UpperCase).removeRange(0, 2)) //  String.format("%04X", offset) add the current offset as a string to the list
        offset += (digitsPer * 2) // increment the offset by half the length of the line
        // (assuming each byte is represented by two characters)
    }

    return byteLocations.joinToString("\n") // join the byte locations into a
    // single string with line breaks
}