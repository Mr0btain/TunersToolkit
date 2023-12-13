package components.maptextoverlay.components

import androidx.compose.ui.graphics.Path
import components.calibrationdata.classes.MapDataValues
import components.maptextoverlay.classes.DimensionMapper


fun MapDataValues.getMapPath(
    dimensMap: DimensionMapper,
    textStart: Int
): Path {
    val mPath = Path()
    val mEndPosition = this.charValues.actualStartPosition + (this.charValues.values.size * (this.charValues.dataType.size * 2))
    var mStart = dimensMap.getTransformedOffset(this.charValues.actualStartPosition - textStart) + 1
    val mEnd = dimensMap.getTransformedOffset(mEndPosition - textStart)


    // Get variables ued by all conditions
    val mStartLine= dimensMap.getLineForPosition(mStart)
    val mEndLine = dimensMap.getLineForPosition(mEnd)

    // Boolean Checks
    val isSingleLine = mStartLine == mEndLine
    val mStartsOnLineStart = mStart == dimensMap.getLineStart(mStartLine)
    val mEndsOnLineEnd = mEnd + 1 == dimensMap.getLineEnd(mEndLine)

    // If we start at the beginning of a line, shift by one space to account for new line character
    if (mStartsOnLineStart) mStart += 1

    val mPathStart = dimensMap.getRectBoxForPosition(mStart)
    val mPathEnd = dimensMap.getRectBoxForPosition(mEnd)

    // TEST
    val mHex = StringBuilder()

    this.charValues.values.forEach {
        mHex.append(it.hexData)
    }

    when {
        // Single Line Rectangle
        (isSingleLine) || (mStartsOnLineStart && mEndsOnLineEnd) -> {
            // Start and End are on same line, simple Rectangle
            val mLeftLineX = mPathStart.left
            val mRightLineX = mPathEnd.right
            val mTopLine = mPathStart.top
            val mBottomLine = mPathEnd.bottom


            // Start Point
            mPath.moveTo(mLeftLineX, mTopLine)
            // Top Line
            mPath.lineTo(mRightLineX, mTopLine)
            // Right Line
            mPath.lineTo(mRightLineX, mBottomLine)
            // Bottom Line
            mPath.lineTo(mLeftLineX, mBottomLine)
            // Left Line
            mPath.lineTo(mLeftLineX, mTopLine)

            // Return Result
            return  mPath
        }

        // Top NOT Square, Bottom is
        !mStartsOnLineStart && mEndsOnLineEnd -> {
            val mStartLineStart = dimensMap.getRectBoxForPosition(dimensMap.getLineStart(mStartLine) + 1)

            // Apply shifts to values used more than once to save calculations
            val mRightLineX = mPathEnd.right
            val mFirstCharLeftX = mPathStart.left
            val mLeftLineX = mStartLineStart.left
            val mTopLine = mPathStart.top
            val mBottom = mPathEnd.bottom
            val mBottomStart = mPathStart.bottom

            // Start Point
            mPath.moveTo(mFirstCharLeftX, mTopLine)
            // Top Line
            mPath.lineTo(mRightLineX, mTopLine)
            // Right Line
            mPath.lineTo(mRightLineX, mBottom)
            // Bottom Line
            mPath.lineTo(mLeftLineX, mBottom)
            // Left Line
            mPath.lineTo(mLeftLineX, mBottomStart)
            // Top line back to Start Char Left
            mPath.lineTo(mFirstCharLeftX, mBottomStart)
            // Left line back to Start Top
            mPath.lineTo(mFirstCharLeftX, mTopLine)

            // Return Result
            return  mPath
        }

        // Top IS square, Bottom isn't
        mStartsOnLineStart -> {
            val mStartLineEnd = dimensMap.getRectBoxForPosition(dimensMap.getLineEnd(mStartLine) - 1)

            // Apply Shifts to values used more then once, to reduce calculations
            val mLeftLineX = mPathStart.left
            val mRightLineX = mStartLineEnd.right
            val mEndCharRightX = mPathEnd.right
            val mTopLine = mPathStart.top
            val mTopEnd = mPathEnd.top
            val mBottom = mPathEnd.bottom

            // Start Point
            mPath.moveTo(mLeftLineX, mTopLine)
            // Top Line
            mPath.lineTo(mRightLineX, mTopLine)
            // Right Line
            mPath.lineTo(mRightLineX, mTopEnd)
            // Bottom Line
            mPath.lineTo(mEndCharRightX, mTopEnd)
            // Right line around End Char
            mPath.lineTo(mEndCharRightX, mBottom)
            // Bottom Line to last line start
            mPath.lineTo(mLeftLineX, mBottom)
            // Left line back to Start
            mPath.lineTo(mLeftLineX, mTopLine)

            // Return Result
            return  mPath
        }

        // Neither are square
        else -> {
            val mStartLineEnd = dimensMap.getRectBoxForPosition(dimensMap.getLineEnd(mStartLine) - 1)
            val mStartLineStart = dimensMap.getRectBoxForPosition(dimensMap.getLineStart(mStartLine) + 1)

            // Apply Shifts to values used more than once, to reduce the amount of calcs
            val mFirstCharLeftLineX = mPathStart.left
            val mRightLineX = mStartLineEnd.right
            val mEndCharRightLineX = mPathEnd.right
            val mLeftLineX = mStartLineStart.left
            val mTopStart = mPathStart.top
            val mTopBottom = mPathStart.bottom
            val mEndTop = mPathEnd.top
            val mEndBottom = mPathEnd.bottom

            // Start Point
            mPath.moveTo(mFirstCharLeftLineX, mTopStart)
            // Top Line
            mPath.lineTo(mRightLineX, mStartLineEnd.top)
            // Right Line
            mPath.lineTo(mRightLineX, mEndTop)
            // Bottom Line
            mPath.lineTo(mEndCharRightLineX, mEndTop)
            // Right line around End Char
            mPath.lineTo(mEndCharRightLineX, mEndBottom)
            // Bottom Line to last line start
            mPath.lineTo(mLeftLineX, mEndBottom)
            // Left line back to Start Bottom
            mPath.lineTo(mLeftLineX, mTopBottom)
            // Top line to Start Bottom Left
            mPath.lineTo(mFirstCharLeftLineX, mTopBottom)
            // Left Line back to Start position
            mPath.lineTo(mFirstCharLeftLineX, mTopStart)

            // Return Result
            return  mPath
        }
    }
}