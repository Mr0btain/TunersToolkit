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
    var mAxisXUnit = ""
    var mAxisYUnit = ""

    // Get Units for each Axis
    if (mapData.axisValues.isNotEmpty()) {
        mAxisXUnit = mapData.axisValues[0].unit
        // Check for Y Axis Header
        if (mapData.axisValues.size >= 2) {
            mAxisYUnit = mapData.axisValues[1].unit
        }
    }

    // Get X Axis Unit Box (X Axis Header)
    val mXHeaderBox = @Composable {
        Text(
            text = mAxisXUnit,
            style = headerTextStyle,
            modifier = Modifier
                .wrapContentSize()
                .padding(bottom = 4.dp)
        )
    }

    // Get Y Axis Unit Box
    // This box is rotated -90 degrees already
    val mYHeaderBox = @Composable {
        Text(
            text = mAxisYUnit,
            style = headerTextStyle,
            modifier = Modifier
                .wrapContentSize()
                .padding(end = 4.dp)
                .vertical()
                .rotate(-90f)
        )
    }

    val mGridView = @Composable {
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
            contents = listOf(mXHeaderBox, mYHeaderBox, mGridView),
            modifier = Modifier.wrapContentSize()
        ) { ( mXHeaderMeasurable, mYHeaderMeasurable, mGridViewMeasurable ), constraints ->
            // Measure Items
            val mXHeaderPlaceable = mXHeaderMeasurable[0].measure(constraints)
            val mYHeaderPlaceable = mYHeaderMeasurable[0].measure(constraints)
            val mGridViewPlaceable = mGridViewMeasurable[0].measure(constraints)

            val mWidth = if (mAxisYUnit.isNotBlank()) {
                mGridViewPlaceable.width + mYHeaderPlaceable.width
            } else mGridViewPlaceable.width
            val mHeight = if (mAxisYUnit.isNotBlank()) {
                mGridViewPlaceable.height + mXHeaderPlaceable.height
            } else mGridViewPlaceable.height

            var mGridX = 0
            var mGridY = 0

            layout(height = mHeight, width = mWidth) {
                if (mAxisXUnit.isNotBlank()) {
                    mXHeaderPlaceable.place(
                        x = ((mGridViewPlaceable.width / 2) - (mXHeaderPlaceable.width / 2)) + mYHeaderPlaceable.width,
                        y = 0
                    )
                    mGridY = mXHeaderPlaceable.height
                }

                if (mAxisYUnit.isNotBlank()) {
                    mYHeaderPlaceable.place(
                        x = 0,
                        y = ((mGridViewPlaceable.height / 2) - (mYHeaderPlaceable.height / 2)) + mXHeaderPlaceable.height
                    )
                    mGridX = mYHeaderPlaceable.width
                }

                mGridViewPlaceable.place(
                    x = mGridX,
                    y = mGridY
                )
            }
        }
    }
}