package screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import screens.editor.components.MainViewModel
import screens.settings.classes.SettingsData
import screens.settings.components.SettingItem
import screens.settings.components.SettingType
import ui.theme.TunerToolkitTheme

@Composable
fun CompactSettings(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings = viewModel.settings

    SettingsScreenBase(
        settings = settings.value,
        modifier = modifier,
        onAlwaysDarkChange = {
            viewModel.updateSettings(
                settings.value.copy(
                    alwaysDarkMode = !settings.value.alwaysDarkMode
                )
            ) },
        onShowTopTabCheckChange = {
            viewModel.updateSettings(
                settings.value.copy(
                    showTopUtilityTabBar = !settings.value.showTopUtilityTabBar
                )
            )
        },
        onHidePagerChange = {
            viewModel.updateSettings(
                settings.value.copy(
                    hidePagerOnKeyUp = !settings.value.hidePagerOnKeyUp
                )
            )
        },
        onByteSizeChange = {
            viewModel.updateSettings(
                settings.value.copy(
                    byteSize = it.toInt()
                )
            )
        },
        onDigitsPerChange = {
            viewModel.updateSettings(
                settings.value.copy(
                    digitsPerLine = it
                )
            )
        },
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
    )
}

@Composable
fun SettingsScreenBase(
    settings: SettingsData,
    modifier: Modifier,
    onShowTopTabCheckChange: (Boolean) -> Unit,
    onHidePagerChange: (Boolean) -> Unit,
    onAlwaysDarkChange: (Boolean) -> Unit,
    onDigitsPerChange: (Int) -> Unit,
    onByteSizeChange: (String) -> Unit,
    textStyle: TextStyle
) {
    TunerToolkitTheme {
        Column(
            modifier = modifier.fillMaxSize(),
        ) {
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontFamily = textStyle.fontFamily,
                fontWeight = FontWeight.Bold,
                color = textStyle.color,
                modifier = Modifier.padding(4.dp).align(Alignment.CenterHorizontally)
            )

            Text(
                text = "Ui",
                fontSize = 20.sp,
                fontFamily = textStyle.fontFamily,
                fontWeight = FontWeight.SemiBold,
                color = textStyle.color,
                modifier = Modifier.padding(4.dp).align(Alignment.CenterHorizontally)
            )

            // Always dark mode
            SettingItem(
                modifier = Modifier.padding(2.dp),
                settingType = SettingType.CheckBox,
                text = "Always Dark Mode",
                textStyle = textStyle,
                checked = settings.alwaysDarkMode,
                onCheckChange = onAlwaysDarkChange
            )

            // Show Top Tab Bar
            SettingItem(
                modifier = Modifier.padding(2.dp),
                settingType = SettingType.CheckBox,
                text = "Show Utility tab bar",
                textStyle = textStyle,
                checked = settings.showTopUtilityTabBar,
                onCheckChange = onShowTopTabCheckChange
            )

            // Show Top Tab Bar
            SettingItem(
                modifier = Modifier.padding(2.dp),
                settingType = SettingType.CheckBox,
                text = "Hide Utility pager on keyboard up",
                textStyle = textStyle,
                checked = settings.hidePagerOnKeyUp,
                onCheckChange = onHidePagerChange
            )

            Divider(modifier = Modifier.padding(4.dp), thickness = 1.dp)

            Text(
                text = "Hex",
                fontSize = 20.sp,
                fontFamily = textStyle.fontFamily,
                fontWeight = FontWeight.SemiBold,
                color = textStyle.color,
                modifier = Modifier.padding(4.dp).align(Alignment.CenterHorizontally)
            )

            // Byte size
            SettingItem(
                modifier = Modifier.padding(2.dp),
                settingType = SettingType.DropDown,
                text = "Byte Size",
                textStyle = textStyle,
                dropdownOptions = listOf("8", "16", "32"),
                dropDownOnCLick = onByteSizeChange,
                initialText = settings.byteSize.toString()
            )

            // Digits per line
            SettingItem(
                modifier = Modifier.padding(2.dp),
                settingType = SettingType.Increment,
                text = "Hex digits per line",
                textStyle = textStyle,
                initialText = settings.digitsPerLine.toString(),
                incrementChange = onDigitsPerChange
            )
        }
    }
}