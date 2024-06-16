@file:Suppress("FunctionName")

package navigation.appbar

import MainViewModel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import components.menus.TopAppBarDropDown
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.arrow_back
import tunerstoolkit.composeapp.generated.resources.menu
import tunerstoolkit.composeapp.generated.resources.tool_menu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopAppBar(
    navButtonOnClick: () -> Unit,
    viewModel: MainViewModel,
    navHostController: NavHostController
) {
    val isLoading = viewModel.isLoading
    val showDropDown = remember { mutableStateOf(false) }
    val currentDestination = navHostController.currentDestination?.route ?: ""

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
            } else if (currentDestination == "SETTINGS") {
                IconButton(
                    onClick = { navHostController.popBackStack() }
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