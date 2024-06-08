@file:Suppress("FunctionName")

package components.hextexteditor

import MainViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.*
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.hextexteditor.components.*
import components.parsers.hex.hexToAscii
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HexTextEditor(
    modifier: Modifier,
    input: String,
    textStyle: TextStyle = TextStyle(
        fontFamily = FontFamily(Font(Res.font.jbmono_reg)),
        fontSize = 12.sp
    ),
    viewModel: MainViewModel
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current.density

    LaunchedEffect(input) {
        // Set Hex editor text in the hoisted state
        viewModel.setHexEditorInitialText(input)

        // Set ascii editor text in the hoisted state
        viewModel.setAsciiInitialText(input)
    }

    // Use BoxWithConstraints so we can get the size of our container to properly set the font size.
    // NOTE: you MUST specify an initial text size in textStyle, don't rely on the default as it will be 0 and it WILL crash
    BoxWithConstraints(
        modifier = modifier.wrapContentSize()
    ) {
        val textSize = ((maxWidth / 440) * textStyle.fontSize.value * density).value.sp

        Row {
            // The Offset text box
            OffsetTextField(
                input = input,
                textStyle = textStyle.copy(fontSize = textSize),
                viewModel = viewModel,
                scrollState = scrollState
            )

            Spacer(modifier = Modifier.padding(6.dp))
            VerticalDivider(modifier = Modifier, thickness = 2.dp)
            Spacer(modifier = Modifier.padding(6.dp))

            // The Hex editor box
            BasicTextField2(
                modifier = Modifier,
                state = viewModel.hexEditorData,
                textStyle = textStyle.copy(fontSize = textSize, textAlign = TextAlign.Center),
                inputTransformation = HexTextInputTransformation(),
                scrollState = scrollState
            )

            Spacer(modifier = Modifier.padding(6.dp))
            VerticalDivider(modifier = Modifier, thickness = 2.dp)

            // The ASCII representation/editor of the Hex
            AsciiTextEditor(
                textStyle = textStyle.copy(fontSize = textSize),
                viewModel = viewModel,
                scrollState = scrollState
            )
        }
    }
}