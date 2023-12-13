@file:Suppress("FunctionName")

package screens.editor.components.hexview

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

fun EditorOnValueChanged(newTextField: TextFieldValue, oldTextField: TextFieldValue): TextFieldValue {
    val mNewLength = newTextField.text.length
    val mOldLength = oldTextField.text.length

    return when {
        mNewLength > mOldLength -> {
            when {
                newTextField.selection.start <= mOldLength -> {
                    val inputChar = newTextField.text[newTextField.selection.start - 1].uppercaseChar()

                    when {
                        inputChar.isDigit() || inputChar in setOf('A', 'B', 'C', 'D', 'E', 'F') -> {
                            TextFieldValue(
                                text = oldTextField.text.replaceRange(
                                    newTextField.selection.start - 1,
                                    newTextField.selection.end,
                                    inputChar.toString()
                                ),
                                selection = newTextField.selection
                            )
                        }

                        else -> {
                            TextFieldValue(
                                text = oldTextField.text,
                                selection = TextRange(
                                    newTextField.selection.start - 1,
                                    newTextField.selection.end - 1
                                ),
                            )
                        }
                    }
                }

                else -> {
                    TextFieldValue(
                        text = oldTextField.text,
                        selection = newTextField.selection
                    )
                }
            }
        }

        mNewLength < mOldLength -> {
            TextFieldValue(
                text = oldTextField.text.replaceRange(
                    newTextField.selection.start,
                    newTextField.selection.end + 1,
                    "F"
                ),
                selection = newTextField.selection
            )
        }

        else -> {
            newTextField
        }
    }
}