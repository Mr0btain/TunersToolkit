@file:Suppress("FunctionName")

package org.mr0btain.tunerstoolkit

import org.mr0btain.tunerstoolkit.components.screenconfig.WindowWidthSizeClass
import org.mr0btain.tunerstoolkit.components.screenconfig.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import org.mr0btain.tunerstoolkit.injection.mainModule
import org.mr0btain.tunerstoolkit.navigation.drawer.DrawerNavigation
import org.mr0btain.tunerstoolkit.navigation.drawer.SideMenuDrawer
import org.koin.compose.KoinApplication
import org.mr0btain.tunerstoolkit.ui.theme.TunerToolkitTheme


/**
 * The app as it was before the overhaul: its drawer, editor, map list and map grid. Shown in the new app's Legacy
 * place until Phases 3 and 4 replace what it does (docs/PLAN.md)
 */
@Composable
fun LegacyApp() {
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