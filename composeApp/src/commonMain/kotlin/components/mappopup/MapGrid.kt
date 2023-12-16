@file:Suppress("FunctionName")

package components.mappopup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.*
import components.calibrationdata.classes.MapDataValues

@Composable
fun MapGrid(
    mapData: MapDataValues,
    modifier: Modifier = Modifier,
    axisTextStyle: TextStyle,
    dataTextStyle: TextStyle,
    headerTextStyle: TextStyle,
) {
    var axisXUnit = ""
    var axisYUnit = ""

    // Get Units for each Axis
    if (mapData.axisValues.isNotEmpty()) {
        axisXUnit = mapData.axisValues[0].unit
        // Check for Y Axis Header
        if (mapData.axisValues.size >= 2) {
            axisYUnit = mapData.axisValues[1].unit
        }
    }

    // Get X Axis Unit Box (X Axis Header)
    val xHeaderBox = @Composable {
        Text(
            text = axisXUnit,
            style = headerTextStyle,
            modifier = Modifier
                .wrapContentSize()
                .padding(bottom = 4.dp)
        )
    }

    // Get Y Axis Unit Box
    // This box is rotated -90 degrees already
    val yHeaderBox = @Composable {
        Text(
            text = axisYUnit,
            style = headerTextStyle,
            modifier = Modifier
                .wrapContentSize()
                .padding(end = 4.dp)
                .vertical()
                .rotate(-90f)
        )
    }

    val gridView = @Composable {
        MapValuesToGrid(
            map = mapData,
            modifier = Modifier,
            axisTextStyle = axisTextStyle,
            dataTextStyle = dataTextStyle
        )
    }

    Box(
        modifier = modifier.wrapContentSize()
    ) {
        Layout(
            contents = listOf(xHeaderBox, yHeaderBox, gridView),
            modifier = Modifier.wrapContentSize()
        ) { ( mXHeaderMeasurable, mYHeaderMeasurable, mGridViewMeasurable ), constraints ->
            // Measure Items
            val xHeaderPlaceable = mXHeaderMeasurable[0].measure(constraints)
            val yHeaderPlaceable = mYHeaderMeasurable[0].measure(constraints)
            val gridViewPlaceable = mGridViewMeasurable[0].measure(constraints)

            val width = if (axisYUnit.isNotBlank()) {
                gridViewPlaceable.width + yHeaderPlaceable.width
            } else gridViewPlaceable.width
            val height = if (axisYUnit.isNotBlank()) {
                gridViewPlaceable.height + xHeaderPlaceable.height
            } else gridViewPlaceable.height

            var gridX = 0
            var gridY = 0

            layout(height = height, width = width) {
                if (axisXUnit.isNotBlank()) {
                    xHeaderPlaceable.place(
                        x = ((gridViewPlaceable.width / 2) - (xHeaderPlaceable.width / 2)) + yHeaderPlaceable.width,
                        y = 0
                    )
                    gridY = xHeaderPlaceable.height
                }

                if (axisYUnit.isNotBlank()) {
                    yHeaderPlaceable.place(
                        x = 0,
                        y = ((gridViewPlaceable.height / 2) - (yHeaderPlaceable.height / 2)) + xHeaderPlaceable.height
                    )
                    gridX = yHeaderPlaceable.width
                }

                gridViewPlaceable.place(
                    x = gridX,
                    y = gridY
                )
            }
        }
    }
}