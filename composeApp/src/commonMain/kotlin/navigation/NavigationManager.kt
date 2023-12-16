package navigation

import androidx.compose.runtime.Composable
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.Navigator
import moe.tlaster.precompose.navigation.transition.NavTransition
import components.screenconfig.AdaptiveScreen
import screens.editor.CompactEditor
import screens.editor.ExpandedEditor
import MainViewModel
import screens.main.CompactMain
import screens.main.ExpandedMain
import screens.settings.CompactSettings
import screens.settings.ExpandedSettings

@Composable
fun NavigationManager(
    viewModel: MainViewModel,
    navigator: Navigator,
    initialRoute: String
) {
    NavHost(
        navigator = navigator,
        initialRoute = initialRoute
    ) {
        scene(
            route = "home",
            navTransition = NavTransition()
        ) {
            AdaptiveScreen(
                compactContent = { CompactMain() },
                expandedContent = { ExpandedMain() }
            )
        }

        scene(
            route = "editor",
            navTransition = NavTransition()
        ) {
            AdaptiveScreen(
                compactContent = { CompactEditor(viewModel = viewModel) },
                expandedContent = { ExpandedEditor(viewModel = viewModel) }
            )
        }

        scene(
            route = "settings",
            navTransition = NavTransition()
        ) {
            AdaptiveScreen(
                compactContent = {
                    CompactSettings(viewModel = viewModel) },
                expandedContent = {
                    ExpandedSettings(viewModel = viewModel)
                }
            )
        }
    }
}