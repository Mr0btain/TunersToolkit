@file:Suppress("FunctionName")

package screens.editor.components.ascii

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import components.parsers.hex.asciiToHex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import screens.editor.components.hexview.asciiOnValueChanged

@Composable
fun AsciiTextView(
    value: MutableState<TextFieldValue>,
    hexTextField: MutableState<TextFieldValue>,
    isUpdateFromOther: MutableState<Boolean>,
    coroutineScope: CoroutineScope,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    chunksPerLine: Int,
    unitNumber: Int,
    totalUnits: Int
) {
    BoxWithConstraints(
        modifier = modifier
    ) {
        BasicTextField(
            value = value.value,
            onValueChange = {
                value.value = asciiOnValueChanged(it, value.value)

                // If it's not an update from another text view, update the other
                if (!isUpdateFromOther.value) {

                    // Update the value, then reset value
                    coroutineScope.launch {
                        // Set update from other view value so HexTextView doesn't send another update back
                        isUpdateFromOther.value = true
                        hexTextField.value =
                            hexTextField.value.copy(text = value.value.text.asciiToHex())
                        isUpdateFromOther.value = false
                    }
                }
            },
            textStyle = textStyle.copy(textAlign = TextAlign.Center),
            visualTransformation = AsciiVisualTransformation(chunksPerLine),
            modifier = when {
                (unitNumber == 0) -> {
                    modifier
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                }

                unitNumber == totalUnits -> {
                    modifier
                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                }

                else -> modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            }
        )
    }
}