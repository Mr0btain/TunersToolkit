@file:Suppress("FunctionName")

package navigation.components

import MainViewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import components.screenconfig.AdaptiveScreen
import screens.NavigationItem
import screens.editor.CompactEditor
import screens.editor.ExpandedEditor
import screens.main.CompactMain
import screens.main.ExpandedMain
import screens.settings.CompactSettings
import screens.settings.ExpandedSettings

@Composable
fun NavigationHost(
    modifier: Modifier = Modifier,
    navHostController: NavHostController,
    startDestination: String = NavigationItem.Main.route,
    viewModel: MainViewModel
) {
    NavHost(
        modifier = modifier,
        navController = navHostController,
        startDestination = startDestination
    ) {
        composable(NavigationItem.Main.route) {
            AdaptiveScreen(
                compactContent = { CompactMain(modifier) },
                expandedContent = { ExpandedMain(viewModel = viewModel) }
            )
        }

        composable(NavigationItem.Editor.route) {
            AdaptiveScreen(
                modifier = modifier,
                compactContent = { CompactEditor(viewModel) },
                expandedContent = { ExpandedEditor(viewModel) }
            )
        }

        composable(NavigationItem.Settings.route) {
            AdaptiveScreen(
                modifier = modifier,
                compactContent = { CompactSettings(viewModel) },
                expandedContent = { ExpandedSettings(viewModel) }
            )
        }
    }
}