package components.mappopup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import components.calibrationdata.classes.MapDataValues
import components.calibrationdata.helpers.formatLength

@Composable
fun MapValuesToGrid(
    map: MapDataValues,
    modifier: Modifier = Modifier,
    axisTextStyle: TextStyle,
    dataTextStyle: TextStyle
) {
    Box(
        modifier = modifier
            .wrapContentSize()
            .padding(2.dp)
            .border(
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSecondaryContainer),
                shape = RoundedCornerShape(6.dp)
            )
    ) {
        // Get all the Characteristic value boxes
        val mGridTextBoxes = @Composable {
            map.charValues.values.forEach {
                GridMapTextBox(
                    value = it.formattedValue,
                    row = it.row,
                    column = it.column,
                    textStyle = dataTextStyle
                )
            }
        }

        // Get X Axis Values
        val mAxisXBoxes = @Composable {
            if (map.axisValues.isNotEmpty()) {
                map.axisValues[0].values.forEach {
                    GridAxisTextBox(
                        value = it.formattedValue.toString(),
                        row = 0,
                        column = it.position,
                        textStyle = axisTextStyle
                    )
                }
            } else {
                GridAxisTextBox(
                    value = "",
                    row = 0,
                    column = 0,
                    textStyle = axisTextStyle
                )
            }
        }

        // Get Y Axis Boxes
        val mAxisYBoxes = @Composable {
            if (map.axisValues.size >= 2) {
                map.axisValues[1].values.forEach {
                    GridAxisTextBox(
                        value = it.formattedValue.toString(),
                        row = it.position,
                        column = 0,
                        textStyle = axisTextStyle
                    )
                }
            } else {
                GridAxisTextBox(
                    value = "",
                    row = 0,
                    column = 0,
                    textStyle = axisTextStyle
                )
            }
        }

        // Get the size of our columns and rows
        var mRowCount = 0
        var mColumnCount = 0

        map.charValues.values.forEach {
            if (it.row > mRowCount) mRowCount = it.row
            if (it.column > mColumnCount) mColumnCount = it.column
        }

        val mRowGridLine = @Composable {
            repeat(mRowCount + 2) {
                Divider(
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        val mColumLine = @Composable {
            repeat(mColumnCount + 1) {
                Divider(
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                )
            }
        }

        Layout(
            contents = listOf(
                mGridTextBoxes,
                mAxisXBoxes,
                mAxisYBoxes,
                mRowGridLine,
                mColumLine
            ),
            modifier = modifier
        ) { (mapValuesMeasureables,
                axisXMeasureables,
                axisYMeasureables,
                rowGridLineMeasurable,
                columnGridLineMeasurable
            ), constraints ->
            val mMapPlacables = mapValuesMeasureables.map { measurable ->
                measurable.measure(constraints)
            }
            val mAxisXPlacables = axisXMeasureables.map { measurable ->
                measurable.measure(constraints)
            }
            val mAxisYPlacables = axisYMeasureables.map { measurable ->
                measurable.measure(constraints)
            }

            // Get position data for each of our values in the grid, excluding the headers
            val mMapPositions = mMapPlacables.map {
                it.parentData as? GridTextBoxParentData ?: GridTextBoxParentData(0, 0)
            }
            val mAxisXPositions = mAxisXPlacables.map {
                it.parentData as? GridTextBoxParentData ?: GridTextBoxParentData(0, 0)
            }
            val mAxisYPositions = mAxisYPlacables.map {
                it.parentData as? GridTextBoxParentData ?: GridTextBoxParentData(0, 0)
            }

            var mItemWidth = 0
            var mItemHeight = 0
            var mBaseXShift = 0

            // Get the largest box width and heights to align all of them
            mMapPlacables.forEach { item ->
                if (item.width > mItemWidth) mItemWidth = item.width
                if (item.height > mItemHeight) mItemHeight = item.height
            }

            mAxisXPlacables.forEach { item ->
                if (item.width > mItemWidth) mItemWidth = item.width
                if (item.height > mItemHeight) mItemHeight = item.height
            }

            mAxisYPlacables.forEach { item ->
                if (item.width > mItemWidth) mItemWidth = item.width
                if (item.width > mBaseXShift) mBaseXShift = item.width
                if (item.height > mItemHeight) mItemHeight = item.height
            }

            val mRowPlaceable = rowGridLineMeasurable.map { measurable ->
                measurable.measure(Constraints(maxWidth = mItemWidth * (mColumnCount + 2)))
            }

            val mColumnPlaceable = columnGridLineMeasurable.map { measurable ->
                measurable.measure(Constraints(maxHeight = mItemHeight * (mRowCount + 2)))
            }

            // Get total Width and Height
            val mWidthTotal = (mBaseXShift + (mItemWidth * (mColumnCount + 1)))
            val mHeightTotal = (mItemHeight * (mRowCount + 2))

            layout(width = mWidthTotal, height = mHeightTotal) {
                // Start with our X Axis, shifting it by one block to line it up with the columns
                mAxisXPlacables.forEachIndexed { index, placeable ->
                    // Add one to colum to shift it right
                    val column = mAxisXPositions[index].column

                    if (index == 0) {
                        placeable.place(
                            x = mBaseXShift + ((mItemWidth - placeable.width) / 2),
                            y = 0
                        )
                    } else {
                        placeable.place(
                            x = (mBaseXShift + ((mItemWidth - placeable.width) / 2) + (column * mItemWidth)),
                            y = 0
                        )
                    }
                }

                // Next place Y Axis Values
                mAxisYPlacables.forEachIndexed { index, placeable ->
                    // Add one to row to shift it down
                    val row = mAxisYPositions[index].row + 1

                    placeable.place(
                        x = 0,
                        y = (row * mItemHeight)
                    )
                }

                // Next Data values
                mMapPlacables.forEachIndexed { index, textBox ->
                    // Both values need shifted here
                    val row = mMapPositions[index].row + 1
                    val column = mMapPositions[index].column

                    textBox.place(
                        x = mBaseXShift + (column * mItemWidth),
                        y = (row * mItemHeight)
                    )
                }

                // Now we need to add our grid lines
                mColumnPlaceable.forEachIndexed { index, line ->
                    line.place(
                        x = mBaseXShift + (index * mItemWidth),
                        y = 0
                    )
                }

                mRowPlaceable.forEachIndexed { index, line ->
                    if (index != 0) {
                        line.place(
                            x = 0,
                            y = (index * mItemHeight)
                        )
                    }
                }
            }
        }
    }
}