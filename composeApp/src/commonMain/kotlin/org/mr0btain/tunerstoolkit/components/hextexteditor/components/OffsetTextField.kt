package org.mr0btain.tunerstoolkit.components.hextexteditor.components
import org.mr0btain.tunerstoolkit.MainViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
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

    BasicTextField(
        modifier = Modifier.wrapContentSize(),
        state = offsetTextState,
        textStyle = textStyle.copy(textAlign = TextAlign.Center),
        scrollState = scrollState,
        readOnly = true
    )
}