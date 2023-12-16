import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import injection.mainModule
import moe.tlaster.precompose.PreComposeApp
import moe.tlaster.precompose.koin.koinViewModel
import moe.tlaster.precompose.navigation.rememberNavigator
import navigation.drawer.DrawerNavigation
import navigation.drawer.SideMenuDrawer
import org.koin.compose.KoinApplication
import ui.theme.TunerToolkitTheme

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun App() {
    PreComposeApp {
        KoinApplication(application = { modules(mainModule) }) {
            val windowSizeClass = calculateWindowSizeClass()
            val viewModel = koinViewModel(vmClass = MainViewModel::class)
            val navigator = rememberNavigator()
            viewModel.windowSizeClass.value = calculateWindowSizeClass().widthSizeClass

            TunerToolkitTheme(
                useDarkTheme = viewModel.settings.value.alwaysDarkMode
            ) {
                if (getPlatform().name == "Java") {
                    SideMenuDrawer(
                        viewModel = viewModel,
                        windowSizeClass = windowSizeClass,
                        navigator = navigator
                    )
                } else {
                    if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                        DrawerNavigation(
                            viewModel = viewModel,
                            windowSizeClass = windowSizeClass,
                            navigator = navigator
                        )
                    } else {
                        SideMenuDrawer(
                            viewModel = viewModel,
                            windowSizeClass = windowSizeClass,
                            navigator = navigator
                        )
                    }
                }
            }
        }
    }
}