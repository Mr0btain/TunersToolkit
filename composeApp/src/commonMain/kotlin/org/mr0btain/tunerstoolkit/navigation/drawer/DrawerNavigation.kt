@file:Suppress("FunctionName")

package org.mr0btain.tunerstoolkit.navigation.drawer
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
import org.mr0btain.tunerstoolkit.components.screenconfig.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import org.mr0btain.tunerstoolkit.components.datastorage.ProjectFilesManager
import kotlinx.coroutines.launch
import org.mr0btain.tunerstoolkit.navigation.appbar.AdaptiveAppBar
import org.jetbrains.compose.resources.painterResource
import org.mr0btain.tunerstoolkit.screens.dialog.DeleteProjectDialog
import org.mr0btain.tunerstoolkit.screens.dialog.DeleteVersionDialog
import org.mr0btain.tunerstoolkit.screens.dialog.NewProjectDialog
import org.mr0btain.tunerstoolkit.screens.dialog.OpenProjectDialog
import org.mr0btain.tunerstoolkit.MainViewModel
import androidx.navigation.NavHostController
import org.mr0btain.tunerstoolkit.components.testing.OpenDemoProject
import org.mr0btain.tunerstoolkit.navigation.components.NavigationHost
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.calculate
import tunerstoolkit.composeapp.generated.resources.home
import tunerstoolkit.composeapp.generated.resources.settings
import org.mr0btain.tunerstoolkit.ui.theme.TunerToolkitTheme

@Composable
fun DrawerNavigation(
    viewModel: MainViewModel,
    windowSizeClass: WindowSizeClass,
    navHostController: NavHostController
) {
    val filesManager = ProjectFilesManager()

    TunerToolkitTheme {
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContentColor = MaterialTheme.colorScheme.onBackground,
                    drawerContainerColor = MaterialTheme.colorScheme.background
                ) {
                    Spacer(Modifier.height(12.dp))

                    NavigationDrawerItem(
                        icon = { Icon(painterResource(Res.drawable.home), contentDescription = "Main") },
                        label = { Text("Main") },
                        selected = (navHostController.currentDestination?.route ?: "") == "MAIN",
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                if ((navHostController.currentDestination?.route ?: "") != "MAIN") {
                                    navHostController.navigate(route = "MAIN")
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )

                    NavigationDrawerItem(
                        icon = { Icon(painterResource(Res.drawable.calculate), contentDescription = "Editor") },
                        label = { Text("Editor") },
                        selected = (navHostController.currentDestination?.route ?: "") == "EDITOR",
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                if ((navHostController.currentDestination?.route ?: "") != "EDITOR") {
                                    navHostController.navigate(route = "EDITOR")
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )

                    NavigationDrawerItem(
                        icon = { Icon(painterResource(Res.drawable.settings), contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = (navHostController.currentDestination?.route ?: "") == "SETTINGS",
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                if ((navHostController.currentDestination?.route ?: "") != "SETTINGS") {
                                    navHostController.navigate(route = "SETTINGS")
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            },
            content = {
                //TODO: Remove this test when not needed
                if (viewModel.showDemoProject.value) {
                    OpenDemoProject(navHostController, viewModel, scope)
                }

                if (viewModel.showNewProjectDialog.value) {
                    NewProjectDialog(
                        viewModel = viewModel,
                        onDismissRequest = { viewModel.showNewProjectDialog.value = false },
                        scope = scope,
                        filesManager = filesManager,
                        focusRequester = FocusRequester()
                    )
                }

                if (viewModel.showOpenProjectDialog.value) {
                    OpenProjectDialog(
                        navHostController = navHostController,
                        onDismissRequest = { viewModel.showOpenProjectDialog.value = false },
                        filesManager = filesManager,
                        scope = scope,
                        viewModel = viewModel
                    )
                }

                if (viewModel.showDeleteProjectDialog.value) {
                    DeleteProjectDialog(
                        projectName = viewModel.projectInfo.value.name,
                        onDismissRequest = { viewModel.showDeleteProjectDialog.value = false },
                        filesManager = filesManager,
                        scope = scope,
                        viewModel = viewModel
                    )
                }

                if (viewModel.showDeleteVersionDialog.value) {
                    DeleteVersionDialog(
                        projectName = viewModel.projectInfo.value.name,
                        versionName = viewModel.projectInfo.value.projectVersion,
                        onDismissRequest = { viewModel.showDeleteVersionDialog.value = false },
                        filesManager = filesManager,
                        scope = scope,
                        viewModel = viewModel
                    )
                }

                AdaptiveAppBar(
                    viewModel = viewModel,
                    drawerState = drawerState,
                    scope = scope,
                    content = {
                        NavigationHost(
                            viewModel = viewModel,
                            navHostController = navHostController
                        )
                    },
                    windowSizeClass = windowSizeClass,
                    navHostController = navHostController
                )
            }
        )
    }
}