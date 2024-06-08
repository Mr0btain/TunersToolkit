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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import components.calibrationdata.classes.MapDataValues

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
        val gridTextBoxes = @Composable {
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
        val axisXBoxes = @Composable {
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
        val axisYBoxes = @Composable {
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
        var rowCount = 0
        var columnCount = 0

        map.charValues.values.forEach {
            if (it.row > rowCount) rowCount = it.row
            if (it.column > columnCount) columnCount = it.column
        }

        val rowGridLine = @Composable {
            repeat(rowCount + 2) {
                VerticalDivider(
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        val columLine = @Composable {
            repeat(columnCount + 1) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                )
            }
        }

        Layout(
            contents = listOf(
                gridTextBoxes,
                axisXBoxes,
                axisYBoxes,
                rowGridLine,
                columLine
            ),
            modifier = modifier
        ) { (mapValuesMeasureables,
                axisXMeasureables,
                axisYMeasureables,
                rowGridLineMeasurable,
                columnGridLineMeasurable
            ), constraints ->
            val mapPlacables = mapValuesMeasureables.map { measurable ->
                measurable.measure(constraints)
            }
            val axisXPlacables = axisXMeasureables.map { measurable ->
                measurable.measure(constraints)
            }
            val axisYPlacables = axisYMeasureables.map { measurable ->
                measurable.measure(constraints)
            }

            // Get position data for each of our values in the grid, excluding the headers
            val mapPositions = mapPlacables.map {
                it.parentData as? GridTextBoxParentData ?: GridTextBoxParentData(0, 0)
            }
            val axisXPositions = axisXPlacables.map {
                it.parentData as? GridTextBoxParentData ?: GridTextBoxParentData(0, 0)
            }
            val axisYPositions = axisYPlacables.map {
                it.parentData as? GridTextBoxParentData ?: GridTextBoxParentData(0, 0)
            }

            var itemWidth = 0
            var itemHeight = 0
            var baseXShift = 0

            // Get the largest box width and heights to align all of them
            mapPlacables.forEach { item ->
                if (item.width > itemWidth) itemWidth = item.width
                if (item.height > itemHeight) itemHeight = item.height
            }

            axisXPlacables.forEach { item ->
                if (item.width > itemWidth) itemWidth = item.width
                if (item.height > itemHeight) itemHeight = item.height
            }

            axisYPlacables.forEach { item ->
                if (item.width > itemWidth) itemWidth = item.width
                if (item.width > baseXShift) baseXShift = item.width
                if (item.height > itemHeight) itemHeight = item.height
            }

            val rowPlaceable = rowGridLineMeasurable.map { measurable ->
                measurable.measure(Constraints(maxWidth = itemWidth * (columnCount + 2)))
            }

            val columnPlaceable = columnGridLineMeasurable.map { measurable ->
                measurable.measure(Constraints(maxHeight = itemHeight * (rowCount + 2)))
            }

            // Get total Width and Height
            val widthTotal = (baseXShift + (itemWidth * (columnCount + 1)))
            val heightTotal = (itemHeight * (rowCount + 2))

            layout(width = widthTotal, height = heightTotal) {
                // Start with our X Axis, shifting it by one block to line it up with the columns
                axisXPlacables.forEachIndexed { index, placeable ->
                    // Add one to colum to shift it right
                    val column = axisXPositions[index].column

                    if (index == 0) {
                        placeable.place(
                            x = baseXShift + ((itemWidth - placeable.width) / 2),
                            y = 0
                        )
                    } else {
                        placeable.place(
                            x = (baseXShift + ((itemWidth - placeable.width) / 2) + (column * itemWidth)),
                            y = 0
                        )
                    }
                }

                // Next place Y Axis Values
                axisYPlacables.forEachIndexed { index, placeable ->
                    // Add one to row to shift it down
                    val row = axisYPositions[index].row + 1

                    placeable.place(
                        x = 0,
                        y = (row * itemHeight)
                    )
                }

                // Next Data values
                mapPlacables.forEachIndexed { index, textBox ->
                    // Both values need shifted here
                    val row = mapPositions[index].row + 1
                    val column = mapPositions[index].column

                    textBox.place(
                        x = baseXShift + (column * itemWidth),
                        y = (row * itemHeight)
                    )
                }

                // Now we need to add our grid lines
                columnPlaceable.forEachIndexed { index, line ->
                    line.place(
                        x = baseXShift + (index * itemWidth),
                        y = 0
                    )
                }

                rowPlaceable.forEachIndexed { index, line ->
                    if (index != 0) {
                        line.place(
                            x = 0,
                            y = (index * itemHeight)
                        )
                    }
                }
            }
        }
    }
}