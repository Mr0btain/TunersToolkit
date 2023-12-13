package screens.settings.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import components.custom.LargeDropdownMenu
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * A Setting item to keep everything the same without repeating all the code
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun SettingItem(
    modifier: Modifier = Modifier,
    settingType: SettingType,
    text: String,
    dropdownOptions: List<String> = listOf(),
    onCheckChange: (Boolean) -> Unit = {},
    checked: Boolean = false,
    initialText: String = "",
    onTextChange: (String) -> Unit = {},
    dropDownOnCLick: (String) -> Unit = {},
    incrementChange: (Int) -> Unit = {},
    textStyle: TextStyle
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            fontSize = textStyle.fontSize,
            fontFamily = textStyle.fontFamily,
            fontWeight = textStyle.fontWeight,
            color = textStyle.color,
            modifier = Modifier.padding(4.dp)
        )

        when (settingType) {
            SettingType.CheckBox -> {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckChange
                )
            }

            SettingType.DropDown -> {
                var selectedIndex by remember { mutableIntStateOf(initialText.toInt()) }

                LargeDropdownMenu(
                    modifier = Modifier.width(100.dp),
                    label = "",
                    items = dropdownOptions,
                    selectedIndex = selectedIndex,
                    onItemSelected = { index, item ->
                        selectedIndex = index
                        dropDownOnCLick(item)
                    }
                )
            }

            SettingType.TextInput -> {
                TextField(
                    value = initialText,
                    onValueChange = onTextChange,
                    textStyle = TextStyle(
                        fontSize = textStyle.fontSize,
                        fontFamily = textStyle.fontFamily,
                        color = textStyle.color
                    )
                )
            }

            SettingType.Increment -> {
                var mIncrement by remember { mutableIntStateOf(initialText.toInt()) }

                Row(
                    modifier = Modifier.wrapContentSize()
                ) {
                    IconButton(
                        onClick = {
                            mIncrement--
                            incrementChange(mIncrement)
                        },
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Icon(painterResource("icons/arrow_downward.png"), "Decrease")
                    }

                    Text(
                        text = mIncrement.toString(),
                        style = textStyle,
                        modifier = Modifier.padding(2.dp).align(Alignment.CenterVertically),
                        textAlign = TextAlign.Center
                    )

                    IconButton(
                        onClick = {
                            mIncrement++
                            incrementChange(mIncrement)
                        },
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Icon(painterResource("icons/arrow_upward.png"), "Increase")
                    }
                }
            }
        }
    }
}