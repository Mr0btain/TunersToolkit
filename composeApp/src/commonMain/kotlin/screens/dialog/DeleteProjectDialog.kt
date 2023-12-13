package screens.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import components.a2l.classes.data.A2lData
import components.custom.AlertDialogView
import components.datastorage.ProjectFilesManager
import components.maptextoverlay.classes.MapValuesSorted
import components.utilitytab.projecttab.classes.ProjectInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import screens.editor.components.MainViewModel

@OptIn(ExperimentalResourceApi::class)
@Composable
fun DeleteProjectDialog(
    onDismissRequest: () -> Unit,
    projectName: String,
    filesManager: ProjectFilesManager,
    viewModel: MainViewModel,
    scope: CoroutineScope
) {
    val mOnConfirm = mutableStateOf(false)

    if (mOnConfirm.value) {
        scope.launch {
            viewModel.showDeleteProjectDialog.value = false
            viewModel.isLoading.value = true
            filesManager.removeEntireProject(projectName)
            clearProjectData(viewModel)
            viewModel.isLoading.value = false
        }
        mOnConfirm.value = false
    }

    AlertDialogView(
        onDismissRequest = { onDismissRequest() },
        onConfirmation = { mOnConfirm.value = true },
        dialogTitle = "Delete Project?",
        dialogText = "Are you sure you want to delete this project: $projectName?",
        icon = painterResource("icons/delete.png")
    )
}

private fun clearProjectData(viewModel: MainViewModel) {
    viewModel.hexDataSegments.clear()
    viewModel.projectInfo.value = ProjectInfo()
    viewModel.mapListItems.clear()
    viewModel.a2lData.value = A2lData()
    viewModel.mapData.value = MapValuesSorted()
}