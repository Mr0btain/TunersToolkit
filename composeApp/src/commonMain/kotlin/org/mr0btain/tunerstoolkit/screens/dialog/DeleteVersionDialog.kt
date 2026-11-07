package org.mr0btain.tunerstoolkit.screens.dialog
import androidx.compose.runtime.Composable
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.A2lData
import org.mr0btain.tunerstoolkit.components.custom.AlertDialogView
import org.mr0btain.tunerstoolkit.components.datastorage.ProjectFilesManager
import org.mr0btain.tunerstoolkit.components.maptextoverlay.classes.MapValuesSorted
import org.mr0btain.tunerstoolkit.components.utilitytab.projecttab.classes.ProjectInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.mr0btain.tunerstoolkit.MainViewModel
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.delete

@OptIn(ExperimentalResourceApi::class)
@Composable
fun DeleteVersionDialog(
    onDismissRequest: () -> Unit,
    projectName: String,
    versionName: String,
    filesManager: ProjectFilesManager,
    viewModel: MainViewModel, scope: CoroutineScope
) {
    AlertDialogView(
        onDismissRequest = { onDismissRequest() },
        onConfirmation = {
            scope.launch(Dispatchers.Default) {
                viewModel.showDeleteVersionDialog.value = false
                viewModel.isLoading.value = true
                filesManager.removeProjectVersion(name = projectName, version = versionName)
                viewModel.isLoading.value = false
            }
        },
        dialogTitle = "Delete Version?",
        dialogText = "Are you sure you want to delete this version: $versionName?",
        icon = painterResource(Res.drawable.delete)
    )
}

private fun clearProjectData(viewModel: MainViewModel) {
    viewModel.hexDataSegments.clear()
    viewModel.projectInfo.value = ProjectInfo()
    viewModel.mapListItems.clear()
    viewModel.a2lData.value = A2lData()
    viewModel.mapData.value = MapValuesSorted()
}