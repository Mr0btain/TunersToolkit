@file:Suppress("FunctionName")

package navigation.appbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import getPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import MainViewModel
import androidx.navigation.NavHostController

@Composable
fun AdaptiveAppBar(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    scope: CoroutineScope,
    drawerState: DrawerState,
    content: @Composable () -> Unit,
    windowSizeClass: WindowSizeClass,
    navHostController: NavHostController
) {
    val platform = remember { getPlatform() }

    if (platform.name == "Java") {
        Scaffold(
            modifier = modifier,
            topBar = {
                SideTopAppBar(
                    menuButtonOnClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    },
                    viewModel = viewModel,
                    navHostController = navHostController
                )
            }
        ) {
            Box(
                modifier = Modifier.padding(PaddingValues(start = 58.dp))
            ) { content() }
        }
    } else {
        when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> {
                Scaffold(
                    modifier = modifier,
                    topBar = {
                        CustomTopAppBar(
                            navButtonOnClick = { scope.launch { drawerState.open() } },
                            viewModel = viewModel,
                            navHostController = navHostController
                        )
                    }
                ) {
                    Box(
                        modifier = Modifier.padding(it)
                    ) { content() }
                }
            }

            else -> {
                Scaffold(
                    modifier = modifier,
                    topBar = {
                        SideTopAppBar(
                            menuButtonOnClick = { scope.launch { drawerState.open() } },
                            viewModel = viewModel,
                            navHostController = navHostController
                        )
                    }
                ) {
                    Box(
                        modifier = Modifier.padding(PaddingValues(start = 58.dp))
                    ) { content() }
                }

            }
        }
    }
}