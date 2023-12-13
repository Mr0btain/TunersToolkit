@file:Suppress("FunctionName")

package components.mappopup

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

// TODO: Add ability to check and format the values specific to the map is applicable
fun MapGridOnValueChanged(
    textField: TextFieldValue
): TextFieldValue {
    val mAllowedChars = setOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.')
    var mDecimalCount = 0
    val mResult = StringBuilder()
    val mContents = textField.text.map { it }
    var mSelection = textField.selection.start

    // Make sure we only have allowed characters and one decimal
    mContents.forEachIndexed { index, char ->
        if (char == '.') {
            if (mDecimalCount == 0) {
                if (index == 0) {
                    mDecimalCount++
                    mResult.append('0')
                    mResult.append(char)
                    mSelection++
                } else {
                    mDecimalCount++
                    mResult.append(char)
                }
            } else mSelection--
        } else if (char in mAllowedChars) mResult.append(char)
    }

    return textField.copy(text = mResult.toString(), selection = TextRange(mSelection))
}