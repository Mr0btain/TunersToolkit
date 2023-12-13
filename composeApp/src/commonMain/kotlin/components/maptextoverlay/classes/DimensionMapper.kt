package components.maptextoverlay.classes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density

class DimensionMapper {
    private var mCharacterHeight = 0f
    private var mCharacterWidth = 0f
    private var mBaseXOffset = 0f
    private var mBaseYOffset = 0f
    private var mLineCharacterLength = 0
    private val mLineOffsetMap = mutableMapOf<Int, Pair<Int, Int>>()
    private var mOffsetMapper = OffsetMapper("")

    /**
     * Gets the needed values for calculating positions
     */
    fun calculateValues(textLayout: TextLayoutResult) {
        val mFirstCharBox = textLayout.getBoundingBox(0)
        val mSecondLineStartBox = textLayout.getBoundingBox(textLayout.getLineStart(1))
        val mThirdLineStartBox =  textLayout.getBoundingBox(textLayout.getLineStart(2))

        // Set values
        mCharacterHeight = mThirdLineStartBox.top - mSecondLineStartBox.top
        mCharacterWidth = mFirstCharBox.width
        // mBaseXOffset = mSecondLineStartBox.left
        mOffsetMapper = OffsetMapper(textLayout.layoutInput.text.text)
        mLineCharacterLength = textLayout.layoutInput.text.lines().first().length

        // Generate a map used to get line offsets
        generateLineMap(textLayout.layoutInput.text.text.length, mLineCharacterLength + 1)
    }

    private fun generateLineMap(length: Int, charsPerLine: Int) {
        for ((index, i) in (0 until length step charsPerLine).withIndex()) {
            mLineOffsetMap[index] = Pair(i, i + charsPerLine)
        }
    }

    private fun calculateTotalLength(unformattedLength: Int, charactersPerLine: Int): Int {
        var mCountTotal = 0

        for (i in 0 until (unformattedLength - 1)) {
            if (i % 2 == 0 && i != 0) {
                mCountTotal += 1
            }

            if (i % charactersPerLine == 0 && i != 0) {
                mCountTotal += 1
            }

            mCountTotal += 1
        }

        return mCountTotal
    }

    fun getRectBoxForPosition(offset: Int): Rect {
        val mLineLocation = getLineForPosition(offset)
        val mYPosition = mLineLocation * mCharacterHeight
        val mLineStartPosition = getLineStart(mLineLocation)
        val mXPosition = ((offset - mLineStartPosition) - 1) * mCharacterWidth

        return Rect(
            topLeft = Offset(x = mXPosition, y = mYPosition + mBaseYOffset),
            bottomRight = Offset(x = mXPosition + mCharacterWidth, y = mYPosition + mBaseYOffset + mCharacterHeight)
        )
    }

    fun getLineForPosition(offset: Int): Int {
        return offset / (mLineCharacterLength + 1)
    }

    fun getLineStart(line: Int): Int {
        return if (mLineOffsetMap.containsKey(line)) {
            mLineOffsetMap[line]?.first ?: 0
        } else 0
    }

    fun getLineEnd(line: Int): Int {
        return if (mLineOffsetMap.containsKey(line)) {
            mLineOffsetMap[line]?.second ?: 0
        } else 0
    }

    fun getTransformedOffset(offset: Int): Int {
        return mOffsetMapper.originalToTransformed(offset)
    }

    fun getOriginalOffset(offset: Int): Int {
        return mOffsetMapper.transformedToOriginal(offset)
    }

    private fun Rect.inflate(verticalDelta: Float, horizontalDelta: Float) = Rect(
        left = left - horizontalDelta,
        top = top - verticalDelta,
        right = right + horizontalDelta,
        bottom = bottom + verticalDelta
    )
}