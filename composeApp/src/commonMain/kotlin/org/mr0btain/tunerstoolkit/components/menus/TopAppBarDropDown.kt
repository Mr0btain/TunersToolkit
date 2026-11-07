package org.mr0btain.tunerstoolkit.components.menus
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import org.mr0btain.tunerstoolkit.MainViewModel
@Composable
fun TopAppBarDropDown(
    expanded: MutableState<Boolean>,
    viewModel: MainViewModel
) {
    DropdownMenu(
        expanded = expanded.value,
        onDismissRequest = { expanded.value = false }
    ) {
        DropdownMenuItem(
            text = { Text("Save") },
            onClick = { expanded.value = false }
        )
        DropdownMenuItem(
            text = { Text("Open Version") },
            onClick = {
                expanded.value = false
                viewModel.showOpenVersionDialog.value = true
            }
        )
        DropdownMenuItem(
            text = { Text("New Version") },
            onClick = {
                expanded.value = false
                viewModel.showNewVersionDialog.value = true
            }
        )
        DropdownMenuItem(
            text = { Text("Open Project") },
            onClick = {
                expanded.value = false
                viewModel.showOpenProjectDialog.value = true
            }
        )
        DropdownMenuItem(
            text = { Text("New Project") },
            onClick = {
                expanded.value = false
                viewModel.showNewProjectDialog.value = true
            }
        )
        DropdownMenuItem(
            text = { Text("Delete Project") },
            onClick = {
                expanded.value = false
                viewModel.showDeleteProjectDialog.value = true
            }
        )
        DropdownMenuItem(
            text = { Text("Delete Version") },
            onClick = {
                expanded.value = false
                viewModel.showDeleteVersionDialog.value = true
            }
        )
    }
}