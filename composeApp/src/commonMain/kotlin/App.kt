@file:Suppress("FunctionName")

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import injection.mainModule
import navigation.drawer.DrawerNavigation
import navigation.drawer.SideMenuDrawer
import org.koin.compose.KoinApplication
import ui.theme.TunerToolkitTheme

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun App() {
    KoinApplication(application = { modules(mainModule) }) {
        val windowSizeClass = calculateWindowSizeClass()
        val viewModel = remember { MainViewModel() }
        val navigator = rememberNavController()
        viewModel.windowSizeClass.value = calculateWindowSizeClass().widthSizeClass

        TunerToolkitTheme(
            useDarkTheme = viewModel.settings.value.alwaysDarkMode
        ) {
            if (getPlatform().name == "Java") {
                SideMenuDrawer(
                    viewModel = viewModel,
                    windowSizeClass = windowSizeClass,
                    navHostController = navigator
                )
            } else {
                if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                    DrawerNavigation(
                        viewModel = viewModel,
                        windowSizeClass = windowSizeClass,
                        navHostController = navigator
                    )
                } else {
                    SideMenuDrawer(
                        viewModel = viewModel,
                        windowSizeClass = windowSizeClass,
                        navHostController = navigator
                    )
                }
            }
        }
    }
}