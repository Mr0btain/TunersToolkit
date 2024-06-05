package components.hextexteditor.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.*

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
                        // Insert 'F' and place cursor before it
                        valueWithChanges.insert(new.end, "F")
                        valueWithChanges.placeCursorBeforeCharAt(new.start)
                    } else {
                        // Revert all changes
                        valueWithChanges.revertAllChanges()
                    }
                }

                (new.start < new.end) -> {
                    // Get the input
                    val inputChar = valueWithChanges.charAt(new.start)

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
                }
            }
        }
    }
}