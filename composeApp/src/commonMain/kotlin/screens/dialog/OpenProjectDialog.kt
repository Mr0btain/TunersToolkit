package screens.dialog

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import components.a2l.classes.data.A2lData
import components.datastorage.ProjectFilesManager
import components.datastorage.components.processFileData
import components.maptextoverlay.classes.MapValuesSorted
import components.utilitytab.projecttab.classes.ProjectInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import moe.tlaster.precompose.navigation.NavOptions
import moe.tlaster.precompose.navigation.Navigator
import moe.tlaster.precompose.navigation.PopUpTo
import screens.editor.components.MainViewModel

@Composable
fun OpenProjectDialog(
    modifier: Modifier = Modifier,
    navigator: Navigator,
    onDismissRequest: () -> Unit,
    filesManager: ProjectFilesManager,
    viewModel: MainViewModel,
    scope: CoroutineScope
) {
    val mProjects = filesManager.getProjectList()
    val mCurrentScreen = navigator.currentEntry.collectAsState(initial = null)

    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp)
            ) {
                Text(text = "Pick a Project:", textAlign = TextAlign.Center)

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .wrapContentSize()
                        .scrollable(orientation = Orientation.Vertical, state = rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    mProjects.forEach {
                        if (it != "settings.sts") {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        viewModel.showOpenProjectDialog.value = false
                                        viewModel.isLoading.value = true
                                        clearProjectData(viewModel)
                                        val versions = filesManager.getProjectVersionsList(it)
                                        filesManager.processFileData(it, versions.last(), viewModel)
                                        viewModel.isLoading.value = false
                                        if (mCurrentScreen.value?.route?.route != "editor") {
                                            navigator.navigate(
                                                route = "editor",
                                                NavOptions(
                                                    popUpTo = PopUpTo(
                                                        route = "editor",
                                                        inclusive = true
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                            ) { Text(it) }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.padding(4.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.wrapContentHeight().padding(8.dp)
            ) {
                TextButton(
                    onClick = { onDismissRequest() }
                ) { Text("Dismiss") }
            }
        }
    }
}

private fun clearProjectData(viewModel: MainViewModel) {
    viewModel.hexDataSegments.clear()
    viewModel.projectInfo.value = ProjectInfo()
    viewModel.mapListItems.clear()
    viewModel.a2lData.value = A2lData()
    viewModel.mapData.value = MapValuesSorted()
}