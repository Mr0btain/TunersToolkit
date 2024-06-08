package components.hextexteditor.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.*

/**
 * The Input Transformation for the Hex Editor TextField that takes the users typed input and
 * depending on what character or action it was, will overwrite the char in its place with either
 * the char or F if backspace is pressed. Also routes actions around our spaces and line breaks to
 * preserve the positions.
 */

@OptIn(ExperimentalFoundationApi::class)
class HexTextInputTransformation: InputTransformation {
    private val allowedCharacters = setOf('A', 'B', 'C', 'D', 'E', 'F', 'a', 'b', 'c', 'd', 'e', 'f')

    override fun transformInput(originalValue: TextFieldCharSequence, valueWithChanges: TextFieldBuffer) {
        valueWithChanges.changes.forEachChange { new, _ ->
            when {
                // If we've hit backspace, replace character with F
                (new.start == new.end) -> {
                    // Check length to make sure were not at 0, 0
                    if (originalValue.length > valueWithChanges.length) {
                        val digitDeleted = originalValue[new.start]

                        // Check if the digit deleted is a space or a newline character as we don't want these deleted
                        if (!digitDeleted.isLetterOrDigit()) {
                            valueWithChanges.revertAllChanges()
                            valueWithChanges.placeCursorBeforeCharAt(new.start)
                        } else {
                            // Insert 'F' and place cursor before it
                            valueWithChanges.insert(new.end, "F")
                            valueWithChanges.placeCursorBeforeCharAt(new.start)
                        }
                    } else {
                        // Revert all changes
                        valueWithChanges.revertAllChanges()
                    }
                }

                (new.start < new.end) -> {
                    // Get the input and char at the old location
                    val inputChar = valueWithChanges.charAt(new.start)
                    val oldCharInLocation = valueWithChanges.charAt(new.end)

                    if (oldCharInLocation.isLetterOrDigit()) {
                        // Check if it's an allowed character
                        if (inputChar.isDigit() || inputChar in allowedCharacters)  {
                            // Revert changes, replace the character with the new one and move cursor after it
                            valueWithChanges.revertAllChanges()
                            valueWithChanges.replace(new.start, new.end, inputChar.toString().uppercase())
                            valueWithChanges.placeCursorAfterCharAt(new.start)
                        } else {
                            // Revert all changes
                            valueWithChanges.revertAllChanges()
                        }
                    } else {
                        if (inputChar.isDigit() || inputChar in allowedCharacters)  {
                            // Revert changes, replace the character with the new one and move cursor after it
                            valueWithChanges.revertAllChanges()
                            valueWithChanges.replace(new.start + 1, new.end + 1, inputChar.toString().uppercase())
                            valueWithChanges.placeCursorAfterCharAt(new.start + 1)
                        } else {
                            // Revert all changes
                            valueWithChanges.revertAllChanges()
                        }
                    }
                }

                else -> {
                    println(valueWithChanges.changes)
                }
            }
        }
    }
}