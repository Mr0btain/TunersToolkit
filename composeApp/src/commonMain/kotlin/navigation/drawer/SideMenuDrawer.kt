package navigation.drawer

import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import components.datastorage.ProjectFilesManager
import kotlinx.coroutines.launch
import moe.tlaster.precompose.navigation.Navigator
import navigation.NavigationManager
import navigation.appbar.AdaptiveAppBar
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import screens.dialog.DeleteProjectDialog
import screens.dialog.DeleteVersionDialog
import screens.dialog.ImportDialog
import screens.dialog.NewProjectDialog
import screens.dialog.OpenProjectDialog
import MainViewModel
import ui.theme.TunerToolkitTheme

@OptIn(ExperimentalResourceApi::class)
@Composable
fun SideMenuDrawer(
    viewModel: MainViewModel,
    windowSizeClass: WindowSizeClass,
    navigator: Navigator
) {
    val mDrawerState = rememberDrawerState(DrawerValue.Closed)
    val mScope = rememberCoroutineScope()
    val mFilesManager = ProjectFilesManager()
    val mSelectedItem = remember { mutableStateOf(0) }

    val mProjectSubItems = listOf(
        SideMenuDrawerItemChildren(
            label = "Import",
            onClick = {
                mScope.launch { mDrawerState.close() }
                viewModel.showImportDialog.value = true
            }
        ),
        SideMenuDrawerItemChildren(
            label = "Export",
            onClick = { mScope.launch { mDrawerState.close() } }
        )
    )
    val mFileSubItems = listOf(
        SideMenuDrawerItemChildren(
            label = "New Version",
            onClick = {
                mScope.launch { mDrawerState.close() }
                viewModel.showNewVersionDialog.value = true
            }
        ),
        SideMenuDrawerItemChildren(
            label = "Open Version",
            onClick = {
                mScope.launch { mDrawerState.close() }
                viewModel.showOpenVersionDialog.value = true
            }
        ),
        SideMenuDrawerItemChildren(
            label = "Open Project",
            onClick = {
                mScope.launch { mDrawerState.close() }
                viewModel.showOpenProjectDialog.value = true
            }
        ),
        SideMenuDrawerItemChildren(
            label = "New Project",
            onClick = {
                mScope.launch { mDrawerState.close() }
                viewModel.showNewProjectDialog.value = true
            }
        ),
        SideMenuDrawerItemChildren(
            label = "Delete Project",
            onClick = {
                mScope.launch { mDrawerState.close() }
                viewModel.showDeleteProjectDialog.value = true
            }
        )
    )
    val mEditSubItems = listOf<SideMenuDrawerItemChildren>()

    if (mDrawerState.isClosed) mSelectedItem.value = 0

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
            drawerState = mDrawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContentColor = MaterialTheme.colorScheme.onBackground,
                    drawerContainerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier.clickable(
                        interactionSource = MutableInteractionSource(),
                        indication = null
                    ) { mSelectedItem.value = 0 }
                ) {
                    Spacer(Modifier.height(12.dp))

                    SideMenuDrawerItem(
                        icon = painterResource("icons/assignment.png"),
                        label = "Project",
                        isSelected = mSelectedItem.value == 1,
                        onClick = {
                            mSelectedItem.value = if (mSelectedItem.value == 1) 0 else 1
                            mScope.launch {
                                // TODO
                            }
                        },
                        enterTransition = expandTransition,
                        exitTransition = collapseTransition,
                        items = mProjectSubItems
                    )

                    SideMenuDrawerItem(
                        icon = painterResource("icons/source.png"),
                        label = "File",
                        isSelected = mSelectedItem.value == 2,
                        onClick = {
                            mSelectedItem.value = if (mSelectedItem.value == 2) 0 else 2
                            mScope.launch {
                                // TODO
                            }
                        },
                        enterTransition = expandTransition,
                        exitTransition = collapseTransition,
                        items = mFileSubItems
                    )

                    SideMenuDrawerItem(
                        icon = painterResource("icons/create.png"),
                        label = "Edit",
                        isSelected = mSelectedItem.value == 3,
                        onClick = {
                            mSelectedItem.value = if (mSelectedItem.value == 3) 0 else 3
                            mScope.launch {
                                // TODO
                            }
                        },
                        enterTransition = expandTransition,
                        exitTransition = collapseTransition,
                        items = mEditSubItems
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

                if (viewModel.showImportDialog.value) {
                    ImportDialog(
                        onDismissRequest = { viewModel.showImportDialog.value = false },
                        viewModel = viewModel,
                        scope = mScope,
                        focusRequester = FocusRequester()
                    )
                }

                AdaptiveAppBar(
                    viewModel = viewModel,
                    drawerState = mDrawerState,
                    scope = mScope,
                    content = {
                        NavigationManager(
                            viewModel,
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