@file:Suppress("FunctionName")

package components.hextexteditor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.rememberTextFieldState
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.hextexteditor.components.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import tunerstoolkit.composeapp.generated.resources.*

/**
 * A Custom made Hex Editor to handle large amounts of data that need to be represented in
 * a certain format. Easier and way more performant to have a custom implementation than to
 * work around TextFields constraints.
 * @param modifier The Modifier, applied to the BoxWithConstraints surrounding the canvas
 * @param input The input to display
 * @param bits The number of hex digits we should display, ex 2 4 6
 * @param digitsPerLine The max number of digits per line
 * @param textStyle How the text should be styled, if none requested defaults will be used
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
    digitsPerLine: Int,
    bits: Int
) {
    val text = input.formatHexStringForEditor(bits, digitsPerLine)
    val scrollState = rememberScrollState()
    val textState = rememberTextFieldState(initialText = text)
    val asciiTextState = rememberTextFieldState(initialText = input.formatHexToAscii(digitsPerLine))
    val offsetTextState = rememberTextFieldState(initialText = input.getOffsetsFromHex(digitsPerLine, bits))
    val density = LocalDensity.current.density

    // Use BoxWithConstraints so we can get the size of our container to properly set the font size.
    // NOTE: you MUST specify an initial text size in textStyle, don't rely on the default as it will be 0 and it WILL crash
    BoxWithConstraints(
        modifier = modifier.wrapContentSize()
    ) {
        val textSize = ((maxWidth / 440) * textStyle.fontSize.value * density).value.sp

        Row {
            // The Offset text box
            BasicTextField2(
                modifier = Modifier,
                state = offsetTextState,
                textStyle = textStyle.copy(fontSize = textSize, textAlign = TextAlign.Right),
                scrollState = scrollState,
                readOnly = true
            )

            Spacer(modifier = Modifier.padding(6.dp))
            VerticalDivider(modifier = Modifier, thickness = 2.dp)
            Spacer(modifier = Modifier.padding(6.dp))

            // The Hex editor box
            BasicTextField2(
                modifier = Modifier,
                state = textState,
                textStyle = textStyle.copy(fontSize = textSize, textAlign = TextAlign.Center),
                inputTransformation = HexTextInputTransformation(),
                scrollState = scrollState
            )

            Spacer(modifier = Modifier.padding(6.dp))
            VerticalDivider(modifier = Modifier, thickness = 2.dp)

            // The ASCII representation/editor of the Hex
            BasicTextField2(
                modifier = Modifier,
                state = asciiTextState,
                textStyle = textStyle.copy(fontSize = textSize, textAlign = TextAlign.Center),
                scrollState = scrollState
            )
        }
    }
}