package components.hextexteditor.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.InputTransformation
import androidx.compose.foundation.text2.input.TextFieldBuffer
import androidx.compose.foundation.text2.input.TextFieldCharSequence

@OptIn(ExperimentalFoundationApi::class)
class AsciiTextInputTransformation: InputTransformation {
    override fun transformInput(originalValue: TextFieldCharSequence, valueWithChanges: TextFieldBuffer) {
        TODO("Not yet implemented")
    }
}