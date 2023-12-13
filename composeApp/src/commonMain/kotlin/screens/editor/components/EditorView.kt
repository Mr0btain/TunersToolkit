package screens.editor.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import components.calibrationdata.classes.MapDataValues
import components.hex.hexToAscii
import kotlinx.coroutines.CoroutineScope
import screens.editor.components.ascii.AsciiTextView
import screens.editor.components.hexview.EditorTextField
import screens.editor.components.hexview.getByteLocations
import screens.editor.components.offset.OffsetTextView

@Composable
fun EditorView(
    input: Pair<Int, String>,
    charsPerLine: Int,
    unitNumber: Int,
    totalUnits: Int,
    mapValues: List<MapDataValues>,
    textStyle: TextStyle,
    viewModel: MainViewModel,
    focusRequester: FocusRequester,
    coroutineScope: CoroutineScope,
    chunkId: Int
) {
    val hexData = remember(input) { mutableStateOf(TextFieldValue(input.second)) }
    val mOffsetData = remember { mutableStateOf("") }
    val mAsciiData = remember(input) { mutableStateOf(TextFieldValue("")) }
    val mUpdateFromOtherTextView = remember { mutableStateOf(false) }
    var mParentSize by remember { mutableStateOf(IntSize(0,0)) }
    var mPosition by remember { mutableStateOf(Rect(0f,0f,0f,0f)) }
    var mParentPosition by remember { mutableStateOf(Rect(0f,0f,0f,0f)) }

    // Set Offset Values
    LaunchedEffect(
        key1 = input.second.length,
        block = {
            mOffsetData.value = input.second.getByteLocations(
                charsPerLine,
                unitNumber
            )
        }
    )

    // Update Ascii Values
    LaunchedEffect(
        key1 = input.second,
        block = {
            mAsciiData.value = TextFieldValue(hexData.value.text.hexToAscii())
        }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                mParentSize = it.size
            }
            .wrapContentHeight()
    ) {
        // Initialize our Text views
        val mOffsetView = @Composable {
            OffsetTextView(
                value = mOffsetData.value,
                textStyle = textStyle,
                unitNumber = unitNumber,
                totalUnits = totalUnits
            )
        }

        val mHexView = @Composable {
            // The main hex view
            EditorTextField(
                hexData = hexData,
                asciiTextFieldValue = mAsciiData,
                viewModel = viewModel,
                readOnly = false,
                focusRequester = focusRequester,
                mapDataValues = mapValues,
                totalUnits = totalUnits,
                unitNumber = unitNumber,
                coroutineScope = coroutineScope,
                isUpdateFromOther = mUpdateFromOtherTextView,
                chunkId = chunkId,
                textStyle = textStyle
            )
        }

        val mAsciiView = @Composable {
            // The ascii view alongside the hex view, this displays an ASCII representation
            // of the hex data, and they update each other upon change. this will also be the
            // slot for the graphs possibly
            AsciiTextView(
                value = mAsciiData,
                textStyle = textStyle,
                chunksPerLine = charsPerLine,
                hexTextField = hexData,
                unitNumber = unitNumber,
                totalUnits = totalUnits,
                coroutineScope = coroutineScope,
                isUpdateFromOther = mUpdateFromOtherTextView
            )
        }

        // Measure and layout our content
        Layout(
            contents = listOf(mOffsetView, mHexView, mAsciiView)
        ) { (mOffsetMeasurable, mHexMeasurable, mAsciiMeasurable), constraints ->
            // Measure our Placeable
            val mOffsetPlaceable = mOffsetMeasurable.map { measurable -> measurable.measure(constraints) }
            val mHexPlaceable = mHexMeasurable.map { measurable -> measurable.measure(constraints) }
            val mAsciiPlaceable = mAsciiMeasurable.map { measurable -> measurable.measure(constraints) }

            // Get the width of all our items, and use that to get the blank space between them
            val mWidthOfItems = mOffsetPlaceable.first().width + mHexPlaceable.first().width + mAsciiPlaceable.first().width
            val mSpaceBetween = (mParentSize.width - mWidthOfItems) / 4

            // Keep track of our X position
            var xPosition = mSpaceBetween

            layout(
                width = mParentSize.width,
                height = mHexPlaceable.first().height
            ) {
                // Place the first Offset view
                mOffsetPlaceable.first().place(x = xPosition, y = 0)

                // Advance our X position
                xPosition += mOffsetPlaceable.first().width + mSpaceBetween

                // Place the Hex view
                mHexPlaceable.first().place(x = xPosition, y = 0)

                // Advance our X position
                xPosition += mHexPlaceable.first().width + mSpaceBetween

                // Place the final Ascii view
                mAsciiPlaceable.first().place(x = xPosition, y = 0)
            }
        }
    }
}