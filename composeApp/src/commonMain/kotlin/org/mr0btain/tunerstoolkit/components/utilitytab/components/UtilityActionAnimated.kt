package org.mr0btain.tunerstoolkit.components.utilitytab.components
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mr0btain.tunerstoolkit.ui.theme.TunerToolkitTheme

@Composable
fun UtilityActionAnimated(
    label: String,
    textStyle: TextStyle,
    onClick: (Double) -> Unit,
    alwaysShow: Boolean = false,
    isOpen: Boolean = false
) {
    var show = isOpen

    if (alwaysShow) {
        show = true
    }

    if (show) {

        val input = remember { mutableStateOf(TextFieldValue()) }

        // Opening Animation
        val expandTransition = remember {
            expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(300)
            ) + fadeIn(
                animationSpec = tween(300)
            )
        }

        // Closing Animation
        val collapseTransition = remember {
            shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(300)
            ) + fadeOut(
                animationSpec = tween(300)
            )
        }

        AnimatedVisibility(
            isOpen,
            enter = expandTransition,
            exit = collapseTransition,
        ) {
            TunerToolkitTheme {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .fillMaxWidth()
                        .padding(4.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            text = label,
                            fontSize = textStyle.fontSize,
                            fontFamily = textStyle.fontFamily,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(
                            modifier = Modifier.padding(4.dp)
                        )

                        OutlinedTextField(
                            value = input.value,
                            textStyle = textStyle,
                            onValueChange = { input.value = it },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        )

                        Button(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary
                                ),
                            onClick = {
                                onClick(input.value.text.toDouble())
                            },
                        ) {
                            Text(
                                text = "Apply",
                                fontFamily = textStyle.fontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}