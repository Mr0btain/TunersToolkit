package components.hextexteditor.components

import MainViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OffsetTextField(
    input: String,
    textStyle: TextStyle,
    viewModel: MainViewModel,
    scrollState: ScrollState
) {
    val offsetTextState = rememberTextFieldState(initialText = input.getOffsetsFromHex(viewModel.digitsPerLine.intValue, viewModel.bitEncoding.value))

    BasicTextField2(
        modifier = Modifier,
        state = offsetTextState,
        textStyle = textStyle.copy(textAlign = TextAlign.Right),
        scrollState = scrollState,
        readOnly = true
    )
}