package components.utilitytab.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.datastorage.components.BasicDialog
import components.utilitytab.buttonbartab.components.UtilityIconButton
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import ui.theme.TunerToolkitTheme

@OptIn(ExperimentalResourceApi::class)
@Composable
fun UtilityDialog(
    dismissState: () -> Unit,
    label: String,
    textStyle: TextStyle,
    onClick: (Double) -> Unit,
    cancelClick: () -> Unit
) {
    val textFieldText = remember { mutableStateOf(TextFieldValue()) }

    TunerToolkitTheme {
        BasicDialog(
            onDismissRequest = dismissState
        ) {
            Column(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            18.dp
                        )
                    )
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                    .padding(
                        top = 24.dp,
                        bottom = 20.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                18.dp
                            )
                        )
                        .align(
                            Alignment.CenterHorizontally
                        )
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = label,
                        fontSize = textStyle.fontSize,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = textStyle.fontFamily,
                        color = textStyle.color,
                        modifier = Modifier
                            .align(
                                Alignment.CenterVertically
                            )
                    )

                    Spacer(
                        Modifier
                            .padding(
                                8.dp
                            )
                    )

                    OutlinedTextField(
                        textFieldText.value,
                        onValueChange = {
                            textFieldText.value = it
                        },
                        textStyle = textStyle,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        modifier = Modifier
                            .align(
                                Alignment.CenterVertically
                            )
                            .width(
                                70.dp
                            )
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                    )

                    Spacer(
                        Modifier.padding(
                            14.dp
                        )
                    )

                    UtilityIconButton(
                        modifier = Modifier
                            .align(
                                Alignment.CenterVertically
                            )
                            .height(50.dp)
                            .width(50.dp),
                        icon = painterResource("icons/done_all.png"),
                    ) { onClick(textFieldText.value.text.toDouble()) }
                }

                Spacer(
                    modifier = Modifier
                        .padding(
                            4.dp
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Cancel",
                        color = textStyle.color,
                        fontFamily = textStyle.fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable(
                                onClick = cancelClick
                            )
                            .padding(
                                top = 8.dp,
                                bottom = 4.dp,
                                end = 8.dp
                            )
                    )
                }
            }
        }
    }
}