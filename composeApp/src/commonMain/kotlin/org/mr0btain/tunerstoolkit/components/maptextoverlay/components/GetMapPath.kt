package org.mr0btain.tunerstoolkit.components.maptextoverlay.components
import androidx.compose.ui.graphics.Path
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.MapDataValues
import org.mr0btain.tunerstoolkit.components.maptextoverlay.classes.DimensionMapper


fun MapDataValues.getMapPath(
    dimensMap: DimensionMapper,
    textStart: Int
): Path {
    val path = Path()
    val endPosition = this.charValues.actualStartPosition + (this.charValues.values.size * (this.charValues.dataType.size * 2))
    var start = dimensMap.getTransformedOffset(this.charValues.actualStartPosition - textStart) + 1
    val end = dimensMap.getTransformedOffset(endPosition - textStart)


    // Get variables ued by all conditions
    val startLine= dimensMap.getLineForPosition(start)
    val endLine = dimensMap.getLineForPosition(end)

    // Boolean Checks
    val isSingleLine = startLine == endLine
    val startsOnLineStart = start == dimensMap.getLineStart(startLine)
    val endsOnLineEnd = end + 1 == dimensMap.getLineEnd(endLine)

    // If we start at the beginning of a line, shift by one space to account for new line character
    if (startsOnLineStart) start += 1

    val pathStart = dimensMap.getRectBoxForPosition(start)
    val pathEnd = dimensMap.getRectBoxForPosition(end)

    // TEST
    val hex = StringBuilder()

    this.charValues.values.forEach {
        hex.append(it.hexData)
    }

    when {
        // Single Line Rectangle
        (isSingleLine) || (startsOnLineStart && endsOnLineEnd) -> {
            // Start and End are on same line, simple Rectangle
            val leftLineX = pathStart.left
            val rightLineX = pathEnd.right
            val topLine = pathStart.top
            val bottomLine = pathEnd.bottom


            // Start Point
            path.moveTo(leftLineX, topLine)
            // Top Line
            path.lineTo(rightLineX, topLine)
            // Right Line
            path.lineTo(rightLineX, bottomLine)
            // Bottom Line
            path.lineTo(leftLineX, bottomLine)
            // Left Line
            path.lineTo(leftLineX, topLine)

            // Return Result
            return  path
        }

        // Top NOT Square, Bottom is
        !startsOnLineStart && endsOnLineEnd -> {
            val startLineStart = dimensMap.getRectBoxForPosition(dimensMap.getLineStart(startLine) + 1)

            // Apply shifts to values used more than once to save calculations
            val rightLineX = pathEnd.right
            val firstCharLeftX = pathStart.left
            val leftLineX = startLineStart.left
            val topLine = pathStart.top
            val bottom = pathEnd.bottom
            val bottomStart = pathStart.bottom

            // Start Point
            path.moveTo(firstCharLeftX, topLine)
            // Top Line
            path.lineTo(rightLineX, topLine)
            // Right Line
            path.lineTo(rightLineX, bottom)
            // Bottom Line
            path.lineTo(leftLineX, bottom)
            // Left Line
            path.lineTo(leftLineX, bottomStart)
            // Top line back to Start Char Left
            path.lineTo(firstCharLeftX, bottomStart)
            // Left line back to Start Top
            path.lineTo(firstCharLeftX, topLine)

            // Return Result
            return  path
        }

        // Top IS square, Bottom isn't
        startsOnLineStart -> {
            val startLineEnd = dimensMap.getRectBoxForPosition(dimensMap.getLineEnd(startLine) - 1)

            // Apply Shifts to values used more then once, to reduce calculations
            val leftLineX = pathStart.left
            val rightLineX = startLineEnd.right
            val endCharRightX = pathEnd.right
            val topLine = pathStart.top
            val topEnd = pathEnd.top
            val bottom = pathEnd.bottom

            // Start Point
            path.moveTo(leftLineX, topLine)
            // Top Line
            path.lineTo(rightLineX, topLine)
            // Right Line
            path.lineTo(rightLineX, topEnd)
            // Bottom Line
            path.lineTo(endCharRightX, topEnd)
            // Right line around End Char
            path.lineTo(endCharRightX, bottom)
            // Bottom Line to last line start
            path.lineTo(leftLineX, bottom)
            // Left line back to Start
            path.lineTo(leftLineX, topLine)

            // Return Result
            return  path
        }

        // Neither are square
        else -> {
            val startLineEnd = dimensMap.getRectBoxForPosition(dimensMap.getLineEnd(startLine) - 1)
            val startLineStart = dimensMap.getRectBoxForPosition(dimensMap.getLineStart(startLine) + 1)

            // Apply Shifts to values used more than once, to reduce the amount of calcs
            val firstCharLeftLineX = pathStart.left
            val rightLineX = startLineEnd.right
            val endCharRightLineX = pathEnd.right
            val leftLineX = startLineStart.left
            val topStart = pathStart.top
            val topBottom = pathStart.bottom
            val endTop = pathEnd.top
            val endBottom = pathEnd.bottom

            // Start Point
            path.moveTo(firstCharLeftLineX, topStart)
            // Top Line
            path.lineTo(rightLineX, startLineEnd.top)
            // Right Line
            path.lineTo(rightLineX, endTop)
            // Bottom Line
            path.lineTo(endCharRightLineX, endTop)
            // Right line around End Char
            path.lineTo(endCharRightLineX, endBottom)
            // Bottom Line to last line start
            path.lineTo(leftLineX, endBottom)
            // Left line back to Start Bottom
            path.lineTo(leftLineX, topBottom)
            // Top line to Start Bottom Left
            path.lineTo(firstCharLeftLineX, topBottom)
            // Left Line back to Start position
            path.lineTo(firstCharLeftLineX, topStart)

            // Return Result
            return  path
        }
    }
}