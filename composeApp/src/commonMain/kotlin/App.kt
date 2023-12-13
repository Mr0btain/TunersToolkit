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
import screens.editor.components.MainViewModel
import ui.theme.TunerToolkitTheme

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun App() {
    PreComposeApp {
        KoinApplication(application = { modules(mainModule) }) {
            val mWindowSizeClass = calculateWindowSizeClass()
            val mViewModel = koinViewModel(vmClass = MainViewModel::class)
            val mNavigator = rememberNavigator()

            TunerToolkitTheme(
                useDarkTheme = mViewModel.settings.value.alwaysDarkMode
            ) {
                if (getPlatform().name == "Java") {
                    SideMenuDrawer(
                        viewModel = mViewModel,
                        windowSizeClass = mWindowSizeClass,
                        navigator = mNavigator
                    )
                } else {
                    if (mWindowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                        DrawerNavigation(
                            viewModel = mViewModel,
                            windowSizeClass = mWindowSizeClass,
                            navigator = mNavigator
                        )
                    } else {
                        SideMenuDrawer(
                            viewModel = mViewModel,
                            windowSizeClass = mWindowSizeClass,
                            navigator = mNavigator
                        )
                    }
                }
            }
        }
    }
}