@file:Suppress("FunctionName")

package org.mr0btain.tunerstoolkit.navigation.components
import org.mr0btain.tunerstoolkit.MainViewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.mr0btain.tunerstoolkit.components.screenconfig.AdaptiveScreen
import org.mr0btain.tunerstoolkit.screens.NavigationItem
import org.mr0btain.tunerstoolkit.screens.editor.CompactEditor
import org.mr0btain.tunerstoolkit.screens.editor.ExpandedEditor
import org.mr0btain.tunerstoolkit.screens.main.CompactMain
import org.mr0btain.tunerstoolkit.screens.main.ExpandedMain
import org.mr0btain.tunerstoolkit.screens.settings.CompactSettings
import org.mr0btain.tunerstoolkit.screens.settings.ExpandedSettings

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