@file:Suppress("FunctionName")

package org.mr0btain.tunerstoolkit.components.hextexteditor.components
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.forEachChange
import androidx.compose.foundation.text.input.insert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AsciiTextEditor(
    displayText: TextFieldState,
    actualAsciiText: MutableState<String>,
    textStyle: TextStyle,
    digitsPerLine: Int,
    scrollState: ScrollState = rememberScrollState()
) {
    BasicTextField(
        modifier = Modifier,
        state = displayText,
        textStyle = textStyle.copy(textAlign = TextAlign.Center),
        scrollState = scrollState,
        inputTransformation = InputTransformation {
            val valueWithChanges = this
            val originalValue = originalText
            valueWithChanges.changes.forEachChange { new, _ ->
                when {
                    // Backspace
                    new.start == new.end -> {
                        if (originalValue.length > valueWithChanges.length) {
                            val digitDeleted = originalValue[new.start]

                            // Check if the digit deleted is a newline character as we don't want these deleted
                            if (digitDeleted == '\n') {
                                valueWithChanges.revertAllChanges()
                                valueWithChanges.placeCursorBeforeCharAt(new.start)
                            } else {
                                val actualAsciiRange = new.end - (new.end / digitsPerLine)

                                // Insert ÿ (the ascii char for FF) and place cursor before it, update the actual data representation
                                valueWithChanges.insert(new.end, ".")
                                actualAsciiText.value.replaceRange(
                                    startIndex = actualAsciiRange,
                                    endIndex = actualAsciiRange + 1,
                                    replacement = "ÿ"
                                )

                                // Update cursor position
                                valueWithChanges.placeCursorBeforeCharAt(new.start)
                            }
                        } else {
                            // Revert all changes
                            valueWithChanges.revertAllChanges()
                        }
                    }

                    // Character input
                    (new.start < new.end && new.start != originalValue.length) -> {
                        val inputChar = valueWithChanges.charAt(new.start)
                        val oldCharInLocation = valueWithChanges.charAt(new.end)

                        // Make sure were not hitting a newline char
                        if (oldCharInLocation != '\n') {
                            val digitInserted = valueWithChanges.charAt(new.start)

                            // If it's a letter or digit, straight up add it
                            if (digitInserted.isLetterOrDigit() && digitInserted.code < 128) {
                                val actualAsciiRange = new.start - (new.start / digitsPerLine)

                                // Insert the new digit and update actual ascii data
                                valueWithChanges.revertAllChanges()
                                valueWithChanges.replace(new.start, new.end, inputChar.toString())
                                actualAsciiText.value.replaceRange(
                                    startIndex = actualAsciiRange,
                                    endIndex = actualAsciiRange + 1,
                                    replacement = inputChar.toString()
                                )

                                // Update cursor position
                                valueWithChanges.placeCursorAfterCharAt(new.start)
                            } else {
                                val actualAsciiRange = new.start - (new.start / digitsPerLine)

                                // Insert new digit and replace visual representation with a '.' and update actual data
                                valueWithChanges.revertAllChanges()
                                valueWithChanges.replace(new.start, new.end, ".")
                                actualAsciiText.value.replaceRange(
                                    startIndex = actualAsciiRange,
                                    endIndex = actualAsciiRange + 1,
                                    replacement = inputChar.toString()
                                )

                                // Update cursor position
                                valueWithChanges.placeCursorAfterCharAt(new.start)
                            }
                        } else {
                            val actualAsciiRange = (new.start + 1) - (new.start + 1 / digitsPerLine)

                            // Insert new digit after the \n character and update actual data
                            valueWithChanges.revertAllChanges()
                            valueWithChanges.replace(new.start + 1, new.end + 1, inputChar.toString())
                            actualAsciiText.value.replaceRange(
                                startIndex = actualAsciiRange,
                                endIndex = actualAsciiRange + 1,
                                replacement = inputChar.toString()
                            )

                            valueWithChanges.placeCursorAfterCharAt(new.start + 1)
                        }
                    }

                    else -> { valueWithChanges.revertAllChanges() }
                }
            }
        }
    )
}