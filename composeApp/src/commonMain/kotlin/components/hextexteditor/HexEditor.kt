@file:Suppress("FunctionName")

package components.hextexteditor

import MainViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.calibrationdata.classes.MapDataValues
import components.hextexteditor.components.*
import org.jetbrains.compose.resources.Font
import tunerstoolkit.composeapp.generated.resources.*

/**
 * A Custom made Hex Editor to handle large amounts of data that need to be represented in
 * a certain format. Easier and way more performant to have a custom implementation than to
 * work around TextFields constraints.
 * @param modifier The Modifier, applied to the BoxWithConstraints surrounding the canvas
 * @param input The input to display
 * @param viewModel The view model where the data is hoisted
 */

@Composable
fun HexEditor(
    modifier: Modifier,
    input: String,
    textStyle: TextStyle = TextStyle(
        fontFamily = FontFamily(Font(Res.font.jbmono_reg)),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onBackground
    ),
    viewModel: MainViewModel,
    mapDataValues: List<MapDataValues> = listOf()
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current.density
    val currentInput = mutableStateOf("")
    val lineCount = 8000 / viewModel.digitsPerLine.intValue
    var scrollPerLine = 0

    LaunchedEffect(Unit) {
        // Get a section of the data to start with
        currentInput.value = input.substring(0, 8000)
    }

    LaunchedEffect(scrollState.value) {
        println("Scroll Value: ${scrollState.value}")
        println("Max Value: ${scrollState.maxValue}")
        println("Line Count: $lineCount")
        println("Scroll Per Line: ${scrollState.maxValue / lineCount}")

        if (scrollState.value > scrollState.maxValue - ((scrollState.maxValue / lineCount) * 60)) {
            println("Updated Hex Values!!!!!!!!!!!")
            currentInput.value = input.substring(6000, 14000)
        }
    }

    LaunchedEffect(currentInput.value) {
        // Set Hex editor text in the hoisted state
        viewModel.setHexEditorInitialText(currentInput.value)

        // Set ascii editor text in the hoisted state
        viewModel.setAsciiInitialText(currentInput.value)
    }

    // Use BoxWithConstraints so we can get the size of our container to properly set the font size.
    // NOTE: you MUST specify an initial text size in textStyle, don't rely on the default as it will be 0 and it WILL crash
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .wrapContentSize()
            .background(MaterialTheme.colorScheme.secondaryContainer)
    ) {
        val textSize = ((maxWidth / 440) * textStyle.fontSize.value * density).value.sp

        Row(
            modifier = Modifier.padding(top = 6.dp, end = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // The Offset text box
            OffsetTextField(
                input = input,
                textStyle = textStyle.copy(fontSize = textSize),
                viewModel = viewModel,
                scrollState = scrollState
            )

            VerticalDivider(modifier = Modifier.padding(2.dp), thickness = 2.dp, color = textStyle.color)
            Spacer(Modifier.padding(4.dp))

            // The Hex editor box
            HexTextEditor(
                viewModel = viewModel,
                textStyle = textStyle.copy(fontSize = textSize),
                scrollState = scrollState,
                mapDataValues = mapDataValues
            )

            Spacer(Modifier.padding(4.dp))
            VerticalDivider(modifier = Modifier.padding(2.dp), thickness = 2.dp, color = textStyle.color)

            // The ASCII representation/editor of the Hex
            AsciiTextEditor(
                textStyle = textStyle.copy(fontSize = textSize),
                viewModel = viewModel,
                scrollState = scrollState
            )
        }
    }
}