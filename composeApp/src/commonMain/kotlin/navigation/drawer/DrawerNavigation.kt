package navigation.drawer

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import components.datastorage.ProjectFilesManager
import kotlinx.coroutines.launch
import moe.tlaster.precompose.navigation.NavOptions
import moe.tlaster.precompose.navigation.Navigator
import moe.tlaster.precompose.navigation.PopUpTo
import navigation.NavigationManager
import navigation.appbar.AdaptiveAppBar
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import screens.dialog.DeleteProjectDialog
import screens.dialog.DeleteVersionDialog
import screens.dialog.NewProjectDialog
import screens.dialog.OpenProjectDialog
import MainViewModel
import ui.theme.TunerToolkitTheme

@OptIn(ExperimentalResourceApi::class)
@Composable
fun DrawerNavigation(
    viewModel: MainViewModel,
    windowSizeClass: WindowSizeClass,
    navigator: Navigator
) {
    val mFilesManager = ProjectFilesManager()
    val mCurrentDestination = navigator.currentEntry.collectAsState(initial = null)

    TunerToolkitTheme {
        val mDrawerState = rememberDrawerState(DrawerValue.Closed)
        val mScope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = mDrawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContentColor = MaterialTheme.colorScheme.onBackground,
                    drawerContainerColor = MaterialTheme.colorScheme.background
                ) {
                    Spacer(Modifier.height(12.dp))

                    NavigationDrawerItem(
                        icon = { Icon(painterResource("icons/home.png"), contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = mCurrentDestination.value?.route?.route == "home",
                        onClick = {
                            mScope.launch {
                                mDrawerState.close()
                                if (mCurrentDestination.value?.route?.route != "home") {
                                    navigator.navigate(
                                        route = "home",
                                        NavOptions(launchSingleTop = true)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )

                    NavigationDrawerItem(
                        icon = { Icon(painterResource("icons/calculate.png"), contentDescription = "Editor") },
                        label = { Text("Editor") },
                        selected = mCurrentDestination.value?.route?.route == "editor",
                        onClick = {
                            mScope.launch {
                                mDrawerState.close()
                                if (mCurrentDestination.value?.route?.route != "editor") {
                                    navigator.navigate(
                                        route = "editor",
                                        NavOptions(
                                            launchSingleTop = true,
                                            popUpTo = PopUpTo(
                                                route = "home",
                                                inclusive = true
                                            )
                                        )
                                    )
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )

                    NavigationDrawerItem(
                        icon = { Icon(painterResource("icons/settings.png"), contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = mCurrentDestination.value?.route?.route == "settings",
                        onClick = {
                            mScope.launch {
                                mDrawerState.close()
                                if (mCurrentDestination.value?.route?.route != "settings") {
                                    navigator.navigate(
                                        route = "settings",
                                        NavOptions(launchSingleTop = true)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            },
            content = {
                if (viewModel.showNewProjectDialog.value) {
                    NewProjectDialog(
                        viewModel = viewModel,
                        onDismissRequest = { viewModel.showNewProjectDialog.value = false },
                        scope = mScope,
                        filesManager = mFilesManager,
                        focusRequester = FocusRequester()
                    )
                }

                if (viewModel.showOpenProjectDialog.value) {
                    OpenProjectDialog(
                        navigator = navigator,
                        onDismissRequest = { viewModel.showOpenProjectDialog.value = false },
                        filesManager = mFilesManager,
                        scope = mScope,
                        viewModel = viewModel
                    )
                }

                if (viewModel.showDeleteProjectDialog.value) {
                    DeleteProjectDialog(
                        projectName = viewModel.projectInfo.value.name,
                        onDismissRequest = { viewModel.showDeleteProjectDialog.value = false },
                        filesManager = mFilesManager,
                        scope = mScope,
                        viewModel = viewModel
                    )
                }

                if (viewModel.showDeleteVersionDialog.value) {
                    DeleteVersionDialog(
                        projectName = viewModel.projectInfo.value.name,
                        versionName = viewModel.projectInfo.value.projectVersion,
                        onDismissRequest = { viewModel.showDeleteVersionDialog.value = false },
                        filesManager = mFilesManager,
                        scope = mScope,
                        viewModel = viewModel
                    )
                }

                AdaptiveAppBar(
                    viewModel = viewModel,
                    drawerState = mDrawerState,
                    scope = mScope,
                    content = {
                        NavigationManager(
                            viewModel = viewModel,
                            initialRoute = "home",
                            navigator = navigator
                        )
                    },
                    windowSizeClass = windowSizeClass,
                    navigator = navigator
                )
            }
        )
    }
}