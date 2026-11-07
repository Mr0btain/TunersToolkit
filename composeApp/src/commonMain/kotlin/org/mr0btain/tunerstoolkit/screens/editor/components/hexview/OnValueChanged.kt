package org.mr0btain.tunerstoolkit.screens.editor.components.hexview
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

@OptIn(ExperimentalStdlibApi::class)
fun String.getByteLocations(digitsPer: Int, index: Int): String {
    val lines = this.length / (digitsPer * 2) // split the input string into lines
    val byteLocations = mutableListOf<String>() // create an empty list to store byte locations
    var offset = (this.length / 2) * index // initialize the offset to 0

    for (line in 0..lines) {
        byteLocations.add(offset.toHexString(HexFormat.UpperCase).removeRange(0, 2)) //  String.format("%04X", offset) add the current offset as a string to the list
        offset += (digitsPer * 2) // increment the offset by half the length of the line
        // (assuming each byte is represented by two characters)
    }

    return byteLocations.joinToString("\n") // join the byte locations into a
    // single string with line breaks
}

fun asciiOnValueChanged(newTextField: TextFieldValue, oldTextField: TextFieldValue): TextFieldValue {
    val newLength = newTextField.text.length
    val oldLength = oldTextField.text.length
    val selectionStart = newTextField.selection.start

    return when {
        newLength > oldLength -> {
            when {
                selectionStart <= oldLength -> {
                    val inputChar = newTextField.text[newTextField.selection.start - 1]

                    when {
                        inputChar.isLetterOrDigit() -> {
                            newTextField.copy(
                                text = oldTextField.text.replaceRange(
                                    newTextField.selection.start - 1,
                                    newTextField.selection.end,
                                    inputChar.toString()
                                )
                            )
                        }

                        else -> {
                            oldTextField.copy(
                                selection = TextRange(
                                    newTextField.selection.start - 1,
                                    newTextField.selection.end - 1
                                )
                            )
                        }
                    }
                }

                else -> {
                    oldTextField.copy(
                        selection = newTextField.selection
                    )
                }
            }
        }

        newLength < oldLength -> {
            newTextField.copy(
                text = oldTextField.text.replaceRange(
                    newTextField.selection.start,
                    newTextField.selection.end + 1,
                    "ÿ"
                    // FF is equal to ÿ
                )
            )
        }

        else -> {
            newTextField
        }
    }
}





