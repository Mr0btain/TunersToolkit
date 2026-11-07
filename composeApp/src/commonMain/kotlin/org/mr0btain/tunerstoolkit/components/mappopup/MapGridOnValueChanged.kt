@file:Suppress("FunctionName")

package org.mr0btain.tunerstoolkit.components.mappopup
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

// TODO: Add ability to check and format the values specific to the map is applicable
fun MapGridOnValueChanged(
    textField: TextFieldValue
): TextFieldValue {
    val allowedChars = setOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.')
    var decimalCount = 0
    val result = StringBuilder()
    val contents = textField.text.map { it }
    var selection = textField.selection.start

    // Make sure we only have allowed characters and one decimal
    contents.forEachIndexed { index, char ->
        if (char == '.') {
            if (decimalCount == 0) {
                if (index == 0) {
                    decimalCount++
                    result.append('0')
                    result.append(char)
                    selection++
                } else {
                    decimalCount++
                    result.append(char)
                }
            } else selection--
        } else if (char in allowedChars) result.append(char)
    }

    return textField.copy(text = result.toString(), selection = TextRange(selection))
}