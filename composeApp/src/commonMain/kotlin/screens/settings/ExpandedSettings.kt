package screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import MainViewModel

// TODO setup layout for this
@Composable
fun ExpandedSettings(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    SettingsScreenBase(
        settings = viewModel.settings.value,
        modifier = modifier,
        onAlwaysDarkChange = {
            viewModel.updateSettings(
                viewModel.settings.value.copy(
                    alwaysDarkMode = !viewModel.settings.value.alwaysDarkMode
                )
            ) },
        onShowTopTabCheckChange = {
            viewModel.updateSettings(
                viewModel.settings.value.copy(
                    showTopUtilityTabBar = !viewModel.settings.value.showTopUtilityTabBar
                )
            )
        },
        onHidePagerChange = {
            viewModel.updateSettings(
                viewModel.settings.value.copy(
                    hidePagerOnKeyUp = !viewModel.settings.value.hidePagerOnKeyUp
                )
            )
        },
        onByteSizeChange = {
            viewModel.updateSettings(
                viewModel.settings.value.copy(
                    byteSize = it.toInt()
                )
            )
        },
        onDigitsPerChange = {
            viewModel.updateSettings(
                viewModel.settings.value.copy(
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