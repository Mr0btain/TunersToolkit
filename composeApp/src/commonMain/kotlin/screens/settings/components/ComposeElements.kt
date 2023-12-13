package screens.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import ui.theme.TunerToolkitTheme

@Composable
fun RandomDataSlider(value: Float, onValueChange: (Float) -> Unit) {
    TunerToolkitTheme {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..20000f,
            steps = 1000
        )
    }
}

@Composable
fun BasicText(value: TextFieldValue, modifier: Modifier, onValueChange: (TextFieldValue) -> Unit, textStyle: TextStyle) {
    TunerToolkitTheme {
        Box(
            modifier = modifier,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                modifier = Modifier,
                textStyle = textStyle
            )
        }
    }
}