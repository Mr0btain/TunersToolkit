package components.custom

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.TextDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

@Composable
fun AutoSizeTextField(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    textStyle: TextStyle,
    onValueChanged: (TextFieldValue) -> Unit,
    visualTransformation: VisualTransformation,
    onTextLayoutResult: (TextLayoutResult) -> Unit
    ) {
    val mTextSize = remember { mutableStateOf(TextUnit.Unspecified) }

    BoxWithConstraints(
        modifier = modifier
    ) {
        BasicTextField(
            value = value,
            onValueChange = { onValueChanged(it) },
            textStyle = textStyle.copy(fontSize = mTextSize.value),
            onTextLayout = {  layoutResult ->
                fun constrain() {
                    val mReducedSize = mTextSize.value * 0.9f

                }
            }
        )
    }


}

@OptIn(InternalFoundationTextApi::class)
@Composable
fun BoxWithConstraintsScope.shouldShrink(
    text: AnnotatedString,
    textStyle: TextStyle,
    maxLines: Int
): Boolean {
    val mDensity = LocalDensity.current

    val mTestDelegate = TextDelegate(
        text = text,
        style = textStyle,
        maxLines = maxLines,
        softWrap = true,
        overflow = TextOverflow.Clip,
        density = mDensity,
        fontFamilyResolver = LocalFontFamilyResolver.current
    )

    val mTextLayoutResult = mTestDelegate.layout(constraints, LocalLayoutDirection.current)

    return mTextLayoutResult.hasVisualOverflow
}