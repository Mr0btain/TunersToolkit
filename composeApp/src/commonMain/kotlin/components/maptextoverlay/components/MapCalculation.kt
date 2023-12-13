package components.maptextoverlay.components

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import components.calibrationdata.classes.MapDataValues
import components.maptextoverlay.classes.DimensionMapper
import components.maptextoverlay.classes.PathData
import components.maptextoverlay.classes.PathLine
import components.maptextoverlay.classes.PathStart

fun MapDataValues.calcMapOutlines(
    dimensMap: DimensionMapper,
    textStart: Int,
    windowWidthSizeClass: WindowWidthSizeClass
): PathData {
    // Base Shifts to apply to path to make it look better
    var mRightShift = 0f
    var mLeftShift = 0f
    var mShiftUp = 0f

    when (windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            mRightShift = -2f
            mLeftShift = 6f
            mShiftUp = 6f
        }
        WindowWidthSizeClass.Medium -> {
            mRightShift = 3f
            mLeftShift = 9f
            mShiftUp = 6f
        }
        WindowWidthSizeClass.Expanded -> {
            mRightShift = 8f
            mLeftShift = 22f
            mShiftUp = 4f
        }
    }

    val mPathStartOffset: PathStart
    val mPath = mutableListOf<PathLine>()
    val mEndPosition = this.charValues.actualStartPosition + (this.charValues.values.size * (this.charValues.dataType.size * 2))
    val mStart = dimensMap.getTransformedOffset(this.charValues.actualStartPosition - textStart)
    var mEnd = dimensMap.getTransformedOffset(mEndPosition - textStart) - 1

    // Get variables ued by all conditions
    val mStartLine= dimensMap.getLineForPosition(mStart)
    val mEndLine = dimensMap.getLineForPosition(mEnd)

    // Boolean Checks
    val isSingleLine = mStartLine == mEndLine
    val mStartsOnLineStart = mStart == dimensMap.getLineStart(mStartLine)
    val mEndsOnLineEnd = (mEnd + 1) == dimensMap.getLineEnd(mEndLine)

    // Check if we end on the end of a line, if so we need to shift our position to the left
    // to account for the extra Space that is at the end
    // TODO: Update this maybe when i remove the extra space at the end of the lines?
    if (mEndsOnLineEnd) mEnd -= 1

    val mPathStart = dimensMap.getRectBoxForPosition(mStart + 1)
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

            // Apply shifts to values used more than once to save computations
            val mLeftLineX = mPathStart.left - mLeftShift
            val mRightLineX = mPathEnd.right - mRightShift
            val mTopLine = mPathStart.top - mShiftUp
            val mBottomLine = mPathEnd.bottom - mShiftUp


            // Start Point
            mPathStartOffset = PathStart(mLeftLineX, mTopLine)
            // Top Line
            mPath.add(PathLine(mRightLineX, mTopLine))
            // Right Line
            mPath.add(PathLine(mRightLineX, mBottomLine))
            // Bottom Line
            mPath.add(PathLine(mLeftLineX, mBottomLine))
            // Left Line
            mPath.add(PathLine(mLeftLineX, mTopLine))

            // Return Result
            return  PathData(start = mPathStartOffset, lines = mPath)
        }

        // Top NOT Square, Bottom is
        !mStartsOnLineStart && mEndsOnLineEnd -> {
            val mStartLineStart = dimensMap.getRectBoxForPosition(dimensMap.getLineStart(mStartLine) + 1)

            // Apply shifts to values used more than once to save calculations
            val mRightLineX = mPathEnd.right - mRightShift
            val mFirstCharLeftX = mPathStart.left - mLeftShift
            val mLeftLineX = mStartLineStart.left - mLeftShift
            val mTopLine = mPathStart.top - mShiftUp
            val mBottom = mPathEnd.bottom - mShiftUp
            val mBottomStart = mPathStart.bottom - mShiftUp

            // Start Point
            mPathStartOffset = PathStart(mFirstCharLeftX, mTopLine)
            // Top Line
            mPath.add(PathLine(mRightLineX, mTopLine))
            // Right Line
            mPath.add(PathLine(mRightLineX, mBottom))
            // Bottom Line
            mPath.add(PathLine(mLeftLineX, mBottom))
            // Left Line
            mPath.add(PathLine(mLeftLineX, mBottomStart))
            // Top line back to Start Char Left
            mPath.add(PathLine(mFirstCharLeftX, mBottomStart))
            // Left line back to Start Top
            mPath.add(PathLine(mFirstCharLeftX, mTopLine))

            // Return Result
            return  PathData(start = mPathStartOffset, lines = mPath)
        }

        // Top IS square, Bottom isn't
        mStartsOnLineStart -> {
            val mStartLineEnd = dimensMap.getRectBoxForPosition(dimensMap.getLineEnd(mStartLine) - 2)

            // Apply Shifts to values used more then once, to reduce calculations
            val mLeftLineX = mPathStart.left - mLeftShift
            val mRightLineX = mStartLineEnd.right - mRightShift
            val mEndCharRightX = mPathEnd.right - mRightShift
            val mTopLine = mPathStart.top - mShiftUp
            val mTopEnd = mPathEnd.top - mShiftUp
            val mBottom = mPathEnd.bottom - mShiftUp

            // Start Point
            mPathStartOffset = PathStart(mLeftLineX, mTopLine)
            // Top Line
            mPath.add(PathLine(mRightLineX, mTopLine))
            // Right Line
            mPath.add(PathLine(mRightLineX, mTopEnd))
            // Bottom Line
            mPath.add(PathLine(mEndCharRightX, mTopEnd))
            // Right line around End Char
            mPath.add(PathLine(mEndCharRightX, mBottom))
            // Bottom Line to last line start
            mPath.add(PathLine(mLeftLineX, mBottom))
            // Left line back to Start
            mPath.add(PathLine(mLeftLineX, mTopLine))

            // Return Result
            return  PathData(start = mPathStartOffset, lines = mPath)
        }

        // Neither are square
        else -> {
            val mStartLineEnd = dimensMap.getRectBoxForPosition(dimensMap.getLineEnd(mStartLine) - 2)
            val mStartLineStart = dimensMap.getRectBoxForPosition(dimensMap.getLineStart(mStartLine) + 1)

            // Apply Shifts to values used more than once, to reduce the amount of calcs
            val mFirstCharLeftLineX = mPathStart.left - mLeftShift
            val mRightLineX = mStartLineEnd.right - mRightShift
            val mEndCharRightLineX = mPathEnd.right - mRightShift
            val mLeftLineX = mStartLineStart.left - mLeftShift
            val mTopStart = mPathStart.top - mShiftUp
            val mTopBottom = mPathStart.bottom - mShiftUp
            val mEndTop = mPathEnd.top - mShiftUp
            val mEndBottom = mPathEnd.bottom - mShiftUp

            // Start Point
            mPathStartOffset = PathStart(mFirstCharLeftLineX, mTopStart)
            // Top Line
            mPath.add(PathLine(mRightLineX, mStartLineEnd.top - mShiftUp))
            // Right Line
            mPath.add(PathLine(mRightLineX, mEndTop))
            // Bottom Line
            mPath.add(PathLine(mEndCharRightLineX, mEndTop))
            // Right line around End Char
            mPath.add(PathLine(mEndCharRightLineX, mEndBottom))
            // Bottom Line to last line start
            mPath.add(PathLine(mLeftLineX, mEndBottom))
            // Left line back to Start Bottom
            mPath.add(PathLine(mLeftLineX, mTopBottom))
            // Top line to Start Bottom Left
            mPath.add(PathLine(mFirstCharLeftLineX, mTopBottom))
            // Left Line back to Start position
            mPath.add(PathLine(mFirstCharLeftLineX, mTopStart))

            // Return Result
            return  PathData(start = mPathStartOffset, lines = mPath)
        }
    }
}
