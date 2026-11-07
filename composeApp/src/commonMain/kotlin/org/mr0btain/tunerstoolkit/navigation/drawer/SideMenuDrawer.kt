@file:Suppress("FunctionName")

package org.mr0btain.tunerstoolkit.navigation.drawer
import org.mr0btain.tunerstoolkit.MainViewModel
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import org.mr0btain.tunerstoolkit.components.screenconfig.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.mr0btain.tunerstoolkit.components.datastorage.ProjectFilesManager
import kotlinx.coroutines.launch
import org.mr0btain.tunerstoolkit.navigation.appbar.AdaptiveAppBar
import org.mr0btain.tunerstoolkit.navigation.components.NavigationHost
import org.jetbrains.compose.resources.painterResource
import org.mr0btain.tunerstoolkit.screens.dialog.*
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.assessment
import tunerstoolkit.composeapp.generated.resources.create
import tunerstoolkit.composeapp.generated.resources.source
import org.mr0btain.tunerstoolkit.ui.theme.TunerToolkitTheme

@Composable
fun SideMenuDrawer(
    viewModel: MainViewModel,
    windowSizeClass: WindowSizeClass,
    navHostController: NavHostController
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val filesManager = ProjectFilesManager()
    val selectedItem = remember { mutableStateOf(0) }

    val projectSubItems = listOf(
        SideMenuDrawerItemChildren(
            label = "Import",
            onClick = {
                scope.launch { drawerState.close() }
                viewModel.showImportDialog.value = true
            }
        ),
        SideMenuDrawerItemChildren(
            label = "Export",
            onClick = { scope.launch { drawerState.close() } }
        )
    )
    val fileSubItems = listOf(
        // New Version Tab
        SideMenuDrawerItemChildren(
            label = "New Version",
            onClick = {
                scope.launch { drawerState.close() }
                viewModel.showNewVersionDialog.value = true
            }
        ),

        // Open Version Tab Should only be shown if a project is currently open?
        SideMenuDrawerItemChildren(
            label = "Open Version",
            onClick = {
                scope.launch { drawerState.close() }
                viewModel.showOpenVersionDialog.value = true
            }
        ),

        // Open Project tab
        SideMenuDrawerItemChildren(
            label = "Open Project",
            onClick = {
                scope.launch { drawerState.close() }
                viewModel.showOpenProjectDialog.value = true
            }
        ),

        // Start a new project
        SideMenuDrawerItemChildren(
            label = "New Project",
            onClick = {
                scope.launch { drawerState.close() }
                viewModel.showNewProjectDialog.value = true
            }
        ),

        // TODO Flesh this out
        // Delete a project? not sure where this should be. maybe add Manage projects item?
        SideMenuDrawerItemChildren(
            label = "Delete Project",
            onClick = {
                scope.launch { drawerState.close() }
                viewModel.showDeleteProjectDialog.value = true
            }
        ),

        // TODO: Remove when not needed
        // Open the demo project's hex and A2L
        SideMenuDrawerItemChildren(
            label = "Open demo project",
            onClick = {
                scope.launch { drawerState.close() }
                viewModel.showDemoProject.value = true
            }
        )

    )
    val editSubItems = listOf<SideMenuDrawerItemChildren>()

    if (drawerState.isClosed) selectedItem.value = 0

    // Opening Animation
    val expandTransition = remember {
        expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(300)
        ) + fadeIn(
            animationSpec = tween(300)
        )
    }

    // Closing Animation
    val collapseTransition = remember {
        shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(300)
        ) + fadeOut(
            animationSpec = tween(300)
        )
    }

    TunerToolkitTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContentColor = MaterialTheme.colorScheme.onBackground,
                    drawerContainerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier.clickable(
                        interactionSource = MutableInteractionSource(),
                        indication = null
                    ) { selectedItem.value = 0 }
                ) {
                    Spacer(Modifier.height(12.dp))

                    SideMenuDrawerItem(
                        icon = painterResource(Res.drawable.assessment),
                        label = "Project",
                        isSelected = selectedItem.value == 1,
                        onClick = {
                            selectedItem.value = if (selectedItem.value == 1) 0 else 1
                            scope.launch {
                                // TODO
                            }
                        },
                        enterTransition = expandTransition,
                        exitTransition = collapseTransition,
                        items = projectSubItems
                    )

                    Spacer(Modifier.height(6.dp))

                    SideMenuDrawerItem(
                        icon = painterResource(Res.drawable.source),
                        label = "File",
                        isSelected = selectedItem.value == 2,
                        onClick = {
                            selectedItem.value = if (selectedItem.value == 2) 0 else 2
                            scope.launch {
                                // TODO
                            }
                        },
                        enterTransition = expandTransition,
                        exitTransition = collapseTransition,
                        items = fileSubItems
                    )

                    Spacer(Modifier.height(6.dp))

                    SideMenuDrawerItem(
                        icon = painterResource(Res.drawable.create),
                        label = "Edit",
                        isSelected = selectedItem.value == 3,
                        onClick = {
                            selectedItem.value = if (selectedItem.value == 3) 0 else 3
                            scope.launch {
                                // TODO
                            }
                        },
                        enterTransition = expandTransition,
                        exitTransition = collapseTransition,
                        items = editSubItems
                    )
                }
            },
            content = {
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

                if (viewModel.showImportDialog.value) {
                    ImportDialog(
                        onDismissRequest = { viewModel.showImportDialog.value = false },
                        viewModel = viewModel,
                        scope = scope,
                        focusRequester = FocusRequester()
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