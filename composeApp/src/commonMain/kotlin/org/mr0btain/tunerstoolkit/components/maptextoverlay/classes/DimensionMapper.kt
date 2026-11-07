package org.mr0btain.tunerstoolkit.components.maptextoverlay.classes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import kotlin.math.ceil
import kotlin.math.roundToInt

class DimensionMapper {
    private var characterHeight = 0f
    private var characterWidth = 0f
    private var baseXOffset = 0f
    private var baseYOffset = 0f
    private var lineCharacterLength = 0
    private val lineOffsetMap = mutableMapOf<Int, Pair<Int, Int>>()
    private var offsetMapper = OffsetMapper("")

    /**
     * Gets the needed values for calculating positions
     */
    fun calculateValues(textLayout: TextLayoutResult) {
        val firstCharBox = textLayout.getBoundingBox(0)
        val secondLineStartBox = textLayout.getBoundingBox(textLayout.getLineStart(1))
        val thirdLineStartBox =  textLayout.getBoundingBox(textLayout.getLineStart(2))

        // Set values
        characterHeight = thirdLineStartBox.top - secondLineStartBox.top
        characterWidth = firstCharBox.width
        // baseXOffset = mSecondLineStartBox.left
        offsetMapper = OffsetMapper(textLayout.layoutInput.text.text)
        lineCharacterLength = textLayout.layoutInput.text.lines().first().length

        // Generate a map used to get line offsets
        generateLineMap(textLayout.layoutInput.text.text.length, lineCharacterLength + 1)
    }

    private fun generateLineMap(length: Int, charsPerLine: Int) {
        for ((index, i) in (0 until length step charsPerLine).withIndex()) {
            lineOffsetMap[index] = Pair(i, i + charsPerLine)
        }
    }

    private fun calculateTotalLength(unformattedLength: Int, charactersPerLine: Int): Int {
        var countTotal = 0

        for (i in 0 until (unformattedLength - 1)) {
            if (i % 2 == 0 && i != 0) {
                countTotal += 1
            }

            if (i % charactersPerLine == 0 && i != 0) {
                countTotal += 1
            }

            countTotal += 1
        }

        return countTotal
    }

    fun getRectBoxForPosition(offset: Int): Rect {
        val lineLocation = getLineForPosition(offset)
        val yPosition = lineLocation * characterHeight
        val lineStartPosition = getLineStart(lineLocation)
        val xPosition = ((offset - lineStartPosition) - 1) * characterWidth

        return Rect(
            topLeft = Offset(x = xPosition, y = yPosition + baseYOffset),
            bottomRight = Offset(x = xPosition + characterWidth, y = yPosition + baseYOffset + characterHeight)
        )
    }

    fun getLineForPosition(offset: Int): Int {
        return offset / (lineCharacterLength + 1)
    }

    fun getLineStart(line: Int): Int {
        return if (lineOffsetMap.containsKey(line)) {
            lineOffsetMap[line]?.first ?: 0
        } else 0
    }

    fun getLineEnd(line: Int): Int {
        return if (lineOffsetMap.containsKey(line)) {
            lineOffsetMap[line]?.second ?: 0
        } else 0
    }

    fun getTransformedOffset(offset: Int): Int {
        return offsetMapper.originalToTransformed(offset)
    }

    fun getOriginalOffset(offset: Int): Int {
        return offsetMapper.transformedToOriginal(offset)
    }

    private fun Rect.inflate(verticalDelta: Float, horizontalDelta: Float) = Rect(
        left = left - horizontalDelta,
        top = top - verticalDelta,
        right = right + horizontalDelta,
        bottom = bottom + verticalDelta
    )
}

fun DimensionMapper.getYOffset(position: Int, density: Density): Int {
    val offset = this.getRectBoxForPosition(position).center.y
    return ceil((offset / density.density).toDouble()).roundToInt()
}