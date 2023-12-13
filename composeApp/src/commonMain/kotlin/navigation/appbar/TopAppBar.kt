package navigation.appbar

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import components.menus.TopAppBarDropDown
import moe.tlaster.precompose.navigation.Navigator
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import screens.editor.components.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun CustomTopAppBar(
    navButtonOnClick: () -> Unit,
    viewModel: MainViewModel,
    navigator: Navigator
) {
    val mIsLoading = viewModel.isLoading
    val mShowDropDown = remember { mutableStateOf(false) }
    val mCurrentDestination = navigator.currentEntry.collectAsState(initial = null)

    CenterAlignedTopAppBar(
        title = { Text(viewModel.appBarTitle.value) },
        navigationIcon = {
            IconButton(
                onClick = navButtonOnClick
            ) {
                Icon(painterResource("icons/menu.png"), "Open Nav Drawer")
            }
        },
        actions = {
            if (mIsLoading.value) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
            } else if (mCurrentDestination.value?.route?.route == "settings") {
                IconButton(
                    onClick = { navigator.goBack() }
                ) {
                    Icon(
                        painter = painterResource("icons/arrow_back.png"),
                        "Return"
                    )
                }
            } else {
                    IconButton(
                        onClick = { mShowDropDown.value = true }
                    ) {
                        Icon(painterResource("icons/tool_menu.png"), "")

                        TopAppBarDropDown(
                            expanded = mShowDropDown,
                            viewModel = viewModel
                        )
                    }
            }
        }
    )
}