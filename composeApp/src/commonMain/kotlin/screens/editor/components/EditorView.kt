package screens.editor.components

import MainViewModel
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import components.calibrationdata.classes.MapDataValues
import components.parsers.hex.hexToAscii
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
    val offsetData = remember { mutableStateOf("") }
    val asciiData = remember(input) { mutableStateOf(TextFieldValue("")) }
    val updateFromOtherTextView = remember { mutableStateOf(false) }
    var parentSize by remember { mutableStateOf(IntSize(0,0)) }
    var position by remember { mutableStateOf(Rect(0f,0f,0f,0f)) }
    var parentPosition by remember { mutableStateOf(Rect(0f,0f,0f,0f)) }

    // Set Offset Values
    LaunchedEffect(
        key1 = input.second.length,
        block = {
            offsetData.value = input.second.getByteLocations(
                charsPerLine,
                unitNumber
            )
        }
    )

    // Update Ascii Values
    LaunchedEffect(
        key1 = input.second,
        block = {
            asciiData.value = TextFieldValue(hexData.value.text.hexToAscii())
        }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                parentSize = it.size
            }
            .wrapContentHeight()
    ) {
        // Initialize our Text views
        val offsetView = @Composable {
            OffsetTextView(
                value = offsetData.value,
                textStyle = textStyle,
                unitNumber = unitNumber,
                totalUnits = totalUnits
            )
        }

        val hexView = @Composable {
            // The main hex view
            EditorTextField(
                hexData = hexData,
                asciiTextFieldValue = asciiData,
                viewModel = viewModel,
                readOnly = false,
                focusRequester = focusRequester,
                mapDataValues = mapValues,
                totalUnits = totalUnits,
                unitNumber = unitNumber,
                coroutineScope = coroutineScope,
                isUpdateFromOther = updateFromOtherTextView,
                chunkId = chunkId,
                textStyle = textStyle
            )
        }

        val asciiView = @Composable {
            // The ascii view alongside the hex view, this displays an ASCII representation
            // of the hex data, and they update each other upon change. this will also be the
            // slot for the graphs possibly
            AsciiTextView(
                value = asciiData,
                textStyle = textStyle,
                chunksPerLine = charsPerLine,
                hexTextField = hexData,
                unitNumber = unitNumber,
                totalUnits = totalUnits,
                coroutineScope = coroutineScope,
                isUpdateFromOther = updateFromOtherTextView
            )
        }

        // Measure and layout our content
        Layout(
            contents = listOf(offsetView, hexView, asciiView)
        ) { (offsetMeasurable, hexMeasurable, asciiMeasurable), constraints ->
            // Measure our Placeable
            val offsetPlaceable = offsetMeasurable.map { measurable -> measurable.measure(constraints) }
            val hexPlaceable = hexMeasurable.map { measurable -> measurable.measure(constraints) }
            val asciiPlaceable = asciiMeasurable.map { measurable -> measurable.measure(constraints) }

            // Get the width of all our items, and use that to get the blank space between them
            val widthOfItems = offsetPlaceable.first().width + hexPlaceable.first().width + asciiPlaceable.first().width
            val spaceBetween = (parentSize.width - widthOfItems) / 4

            // Keep track of our X position
            var xPosition = spaceBetween

            layout(
                width = parentSize.width,
                height = hexPlaceable.first().height
            ) {
                // Place the first Offset view
                offsetPlaceable.first().place(x = xPosition, y = 0)

                // Advance our X position
                xPosition += offsetPlaceable.first().width + spaceBetween

                // Place the Hex view
                hexPlaceable.first().place(x = xPosition, y = 0)

                // Advance our X position
                xPosition += hexPlaceable.first().width + spaceBetween

                // Place the final Ascii view
                asciiPlaceable.first().place(x = xPosition, y = 0)
            }
        }
    }
}