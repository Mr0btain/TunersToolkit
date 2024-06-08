package components.hextexteditor.components

import MainViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text2.BasicTextField2
import androidx.compose.foundation.text2.input.forEachChange
import androidx.compose.foundation.text2.input.insert
import androidx.compose.foundation.text2.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import components.parsers.hex.hexToAscii

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AsciiTextEditor(
    textStyle: TextStyle,
    viewModel: MainViewModel,
    scrollState: ScrollState
) {
    BasicTextField2(
        modifier = Modifier,
        state = viewModel.asciiEditorData,
        textStyle = textStyle.copy(textAlign = TextAlign.Center),
        scrollState = scrollState,
        inputTransformation = { originalValue, valueWithChanges ->
            valueWithChanges.changes.forEachChange { new, orig ->
                val digitInserted = valueWithChanges.charAt(new.start)

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
                                val actualAsciiRange = new.end - (new.end / viewModel.digitsPerLine.intValue)

                                // Insert ÿ (the ascii char for FF) and place cursor before it, update the actual data representation
                                valueWithChanges.insert(new.end, ".")
                                viewModel.asciiActualEditorData.value.replaceRange(
                                    startIndex = actualAsciiRange,
                                    endIndex = actualAsciiRange + 1,
                                    replacement = "ÿ"
                                )

                                // Update the Hex data
                                viewModel.updateHexEditorText("ÿ", IntRange(new.end, new.end + 1), true)

                                // Update cursor position
                                valueWithChanges.placeCursorBeforeCharAt(new.start)
                            }
                        } else {
                            // Revert all changes
                            valueWithChanges.revertAllChanges()
                        }
                    }

                    // Character input
                    (new.start < new.end) -> {
                        val inputChar = valueWithChanges.charAt(new.start)
                        val oldCharInLocation = valueWithChanges.charAt(new.end)

                        // Make sure were not hitting a newline char
                        if (oldCharInLocation != '\n') {
                            // If it's a letter or digit, straight up add it
                            if (digitInserted.isLetterOrDigit() && digitInserted.code < 128) {
                                val actualAsciiRange = new.start - (new.start / viewModel.digitsPerLine.intValue)

                                // Insert the new digit and update actual ascii data
                                valueWithChanges.revertAllChanges()
                                valueWithChanges.replace(new.start, new.end, inputChar.toString())
                                viewModel.asciiActualEditorData.value.replaceRange(
                                    startIndex = actualAsciiRange,
                                    endIndex = actualAsciiRange + 1,
                                    replacement = inputChar.toString()
                                )

                                // Update Hex data
                                viewModel.updateHexEditorText(inputChar.toString(), IntRange(new.start, new.end), true)

                                // Update cursor position
                                valueWithChanges.placeCursorAfterCharAt(new.start)
                            } else {
                                val actualAsciiRange = new.start - (new.start / viewModel.digitsPerLine.intValue)

                                // Insert new digit and replace visual representation with a '.' and update actual data
                                valueWithChanges.revertAllChanges()
                                valueWithChanges.replace(new.start, new.end, ".")
                                viewModel.asciiActualEditorData.value.replaceRange(
                                    startIndex = actualAsciiRange,
                                    endIndex = actualAsciiRange + 1,
                                    replacement = inputChar.toString()
                                )

                                // Update Hex data
                                viewModel.updateHexEditorText(inputChar.toString(), IntRange(new.start, new.end), true)

                                // Update cursor position
                                valueWithChanges.placeCursorAfterCharAt(new.start)
                            }
                        } else {
                            val actualAsciiRange = (new.start + 1) - (new.start + 1 / viewModel.digitsPerLine.intValue)

                            // Insert new digit after the \n character and update actual data
                            valueWithChanges.revertAllChanges()
                            valueWithChanges.replace(new.start + 1, new.end + 1, inputChar.toString())
                            viewModel.asciiActualEditorData.value.replaceRange(
                                startIndex = actualAsciiRange,
                                endIndex = actualAsciiRange + 1,
                                replacement = inputChar.toString()
                            )

                            // Update Hex data
                            viewModel.updateHexEditorText(inputChar.toString(), IntRange(new.start + 1, new.end + 1), true)

                            valueWithChanges.placeCursorAfterCharAt(new.start + 1)
                        }
                    }
                }
            }
        }
    )
}