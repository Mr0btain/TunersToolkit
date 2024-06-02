package navigation.appbar

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import components.menus.TopAppBarDropDown
import moe.tlaster.precompose.navigation.Navigator
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import MainViewModel
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.arrow_back
import tunerstoolkit.composeapp.generated.resources.menu
import tunerstoolkit.composeapp.generated.resources.tool_menu

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun CustomTopAppBar(
    navButtonOnClick: () -> Unit,
    viewModel: MainViewModel,
    navigator: Navigator
) {
    val isLoading = viewModel.isLoading
    val showDropDown = remember { mutableStateOf(false) }
    val currentDestination = navigator.currentEntry.collectAsState(initial = null)

    CenterAlignedTopAppBar(
        title = { Text(viewModel.appBarTitle.value) },
        navigationIcon = {
            IconButton(
                onClick = navButtonOnClick
            ) {
                Icon(painterResource(Res.drawable.menu), "Open Nav Drawer")
            }
        },
        actions = {
            if (isLoading.value) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
            } else if (currentDestination.value?.route?.route == "settings") {
                IconButton(
                    onClick = { navigator.goBack() }
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.arrow_back),
                        "Return"
                    )
                }
            } else {
                    IconButton(
                        onClick = { showDropDown.value = true }
                    ) {
                        Icon(painterResource(Res.drawable.tool_menu), "")

                        TopAppBarDropDown(
                            expanded = showDropDown,
                            viewModel = viewModel
                        )
                    }
            }
        }
    )
}