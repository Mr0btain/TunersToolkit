package navigation.appbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import getPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import moe.tlaster.precompose.navigation.Navigator
import screens.editor.components.MainViewModel

@Composable
fun AdaptiveAppBar(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    scope: CoroutineScope,
    drawerState: DrawerState,
    content: @Composable () -> Unit,
    windowSizeClass: WindowSizeClass,
    navigator: Navigator
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            if (getPlatform().name == "Java") {
                SideTopAppBar(
                    menuButtonOnClick = { scope.launch { drawerState.open() } },
                    viewModel = viewModel,
                    navigator = navigator
                )
            } else {
                when (windowSizeClass.widthSizeClass) {
                    WindowWidthSizeClass.Compact -> {
                        CustomTopAppBar(
                            navButtonOnClick = { scope.launch { drawerState.open() } },
                            viewModel = viewModel,
                            navigator = navigator
                        )
                    }

                    else -> {
                        SideTopAppBar(
                            menuButtonOnClick = { scope.launch { drawerState.open() } },
                            viewModel = viewModel,
                            navigator = navigator
                        )
                    }
                }
            }
        },
    ) {
        when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> {
                Box(
                    modifier = Modifier
                        .padding(it)
                ) { content() }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .padding(PaddingValues(start = 58.dp)
                    )
                ) { content() }
            }
        }
    }
}