@file:Suppress("FunctionName")

package components.mappopup

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

// TODO: Make this look way better

@Composable
fun GridMapTextBox(
    value: Any,
    row: Int,
    column: Int,
    textStyle: TextStyle
) {
    val rememberedValue = remember { mutableStateOf(TextFieldValue(value.toString())) }

    val customTextSelectionColors = TextSelectionColors(
        handleColor = Color.Transparent,
        backgroundColor = MaterialTheme.colorScheme.primary
    )

    // LocalLayoutDirection provides LayoutDirection.Rtl
    CompositionLocalProvider(
        LocalTextSelectionColors provides customTextSelectionColors,
    ) {
        BasicTextField(
            value = rememberedValue.value,
            onValueChange = { rememberedValue.value = MapGridOnValueChanged(it) },
            textStyle = textStyle,
            singleLine = true,
            modifier = Modifier
                .positionInGrid(row, column)
                .wrapContentSize()
                .padding(start = 8.dp, end = 6.dp, top = 4.dp, bottom = 2.dp)
        )
    }

    // LocalFocusManager.current.moveFocus(FocusDirection.Next)
}