package components.hextexteditor.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

class HexTextPositionHandler {
    private val lineData = mutableMapOf<Int, LineData>()
    private var characterSizeData = HexCharacterData(Size.Zero, 0, 0)

    fun addCharacterData(
        characterSize: Size,
        charactersPerLine: Int,
        bits: Int
    ) {
        characterSizeData = HexCharacterData(characterSize, charactersPerLine, bits)
    }

    fun getCharacterRectFromOffset(offset: Offset): Rect {
        var line = LineData(0f, 0f, 0f)

        lineData.forEach { (_, data) ->
            // Check if our y falls inside the line
            if (offset.y >= data.lineTopOffset && offset.y <= data.lineTopOffset + characterSizeData.size.height) {
                line = data
            }
        }

        return when {
            offset.x <= line.lineStartOffset -> {
                Rect(
                    topLeft = Offset(
                        x = line.lineStartOffset,
                        y = line.lineTopOffset
                    ),
                    bottomRight = Offset(
                        x = line.lineStartOffset + characterSizeData.size.width,
                        y = line.lineTopOffset + characterSizeData.size.height
                    )
                )
            }

            offset.x >= line.lineEndOffset -> {
                Rect(
                    topLeft = Offset(
                        x = line.lineEndOffset - characterSizeData.size.width,
                        y = line.lineTopOffset
                    ),
                    bottomRight = Offset(
                        x = line.lineEndOffset,
                        y = line.lineTopOffset + characterSizeData.size.height
                    )
                )
            }

            offset.x >= line.lineStartOffset && offset.x <= line.lineEndOffset -> {
                val numberIn = ((offset.x - line.lineStartOffset) / characterSizeData.size.width).toInt()
                val characterStartOffset = numberIn * characterSizeData.size.width + line.lineStartOffset

                Rect(
                    topLeft = Offset(
                        x = characterStartOffset,
                        y = line.lineTopOffset
                    ),
                    bottomRight = Offset(
                        x = characterStartOffset + characterSizeData.size.width,
                        y = line.lineTopOffset + characterSizeData.size.height
                    )
                )
            }

            else -> {
                Rect(
                    topLeft = Offset(
                        x = line.lineStartOffset,
                        y = line.lineTopOffset
                    ),
                    bottomRight = Offset(
                        x = line.lineStartOffset + characterSizeData.size.width,
                        y = line.lineTopOffset + characterSizeData.size.height
                    )
                )
            }
        }
    }

    fun getLineRectFromOffset(offset: Offset): Rect {
        var line = LineData(0f, 0f, 0f)

        lineData.forEach { (_, data) ->
            // Check if our y falls inside the line
            if (offset.y >= data.lineTopOffset && offset.y <= data.lineTopOffset + characterSizeData.size.height) {
                line = data
            }
        }

        return Rect(
            topLeft = Offset(x = line.lineStartOffset, y = line.lineTopOffset),
            bottomRight = Offset(x = line.lineEndOffset, y = line.lineTopOffset + characterSizeData.size.height)
        )
    }

    fun getLineFromOffset(offset: Offset): Int {
        lineData.forEach { (number, data) ->
            // Check if our y falls inside the line
            if (offset.y >= data.lineTopOffset && offset.y <= data.lineTopOffset + characterSizeData.size.height) {
                return number
            }
        }

        return 0
    }

    fun getCharacterPositionFromOffset(offset: Offset): Int {
        var line = LineData(0f, 0f, 0f)
        var lineNum = 0
        var characterPositionInLine = 0

        lineData.forEach { (number, data) ->
            // Check if our y falls inside the line
            if (offset.y >= data.lineTopOffset && offset.y <= data.lineTopOffset + characterSizeData.size.height) {
                line = data
                lineNum = number
            }
        }

        return when {
            offset.x <= line.lineStartOffset && offset.x <= line.lineEndOffset -> {
                val charInLine = offset.x - line.lineStartOffset

                if (charInLine < 1f) {
                    lineNum * characterSizeData.charactersPerLine
                } else {
                    (charInLine / characterSizeData.size.width).toInt() + (lineNum * characterSizeData.charactersPerLine)
                }
            }

            offset.x < line.lineStartOffset -> {
                lineNum * characterSizeData.charactersPerLine
            }

            offset.x > line.lineEndOffset -> {
                lineNum + characterSizeData.charactersPerLine
            }

            else -> 0
        }
    }

    fun addLineData(
        lineNumber: Int,
        lineStartOffset: Float,
        lineTopOffset: Float,
        lineEndOffset: Float
    ) {
        lineData[lineNumber] = LineData(lineStartOffset, lineTopOffset, lineEndOffset)
    }

    fun getLineStartOffset(lineNumber: Int): Float {
        return lineData[lineNumber]?.lineStartOffset ?: Float.NaN
    }

    fun getLineEndOffset(lineNumber: Int): Float {
        return lineData[lineNumber]?.lineEndOffset ?: Float.NaN
    }

    fun getLineTopOffset(lineNumber: Int): Float {
        return lineData[lineNumber]?.lineTopOffset ?: Float.NaN
    }
}

private data class LineData(
    val lineStartOffset: Float,
    val lineTopOffset: Float,
    val lineEndOffset: Float
)

private data class HexCharacterData(
    val size: Size,
    val charactersPerLine: Int,
    val bits: Int
)