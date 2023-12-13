package components.utilitytab.projecttab.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import components.utilitytab.projecttab.components.InputTextOption
import screens.editor.components.MainViewModel

// TODO: Make version a drop down menu with all current versions and maybe add version?
@Composable
fun ProjectField(
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    name: String,
    version: String,
    viewModel: MainViewModel
) {
    val mName = remember(viewModel.projectInfo.value.name) { mutableStateOf(name) }
    val mVersion = remember(viewModel.projectInfo.value.projectVersion) { mutableStateOf(version) }
    val isNameChanged = derivedStateOf { viewModel.projectInfo.value.name != mName.value }
    val isVersionChanged = derivedStateOf { viewModel.projectInfo.value.projectVersion != mVersion.value }

    if (isNameChanged.value) { viewModel.projectInfo.value = viewModel.projectInfo.value.copy(name = mName.value) }
    if (isVersionChanged.value) { viewModel.projectInfo.value = viewModel.projectInfo.value.copy(projectVersion = mVersion.value) }

    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .wrapContentHeight()
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "Project",
                style = TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = textStyle.fontSize.times(1.8),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.padding(4.dp)
            )

            Column(
                modifier = Modifier
                    .padding(4.dp)
                    .wrapContentHeight()
                    .fillMaxWidth()
            ) {
                InputTextOption(
                    label = "Name:",
                    textFieldText = mName.value,
                    textFieldOnChange = { mName.value = it },
                    textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSecondaryContainer)
                )

                InputTextOption(
                    label = "Version:",
                    textFieldText = mVersion.value,
                    textFieldOnChange = { mVersion.value = it },
                    textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSecondaryContainer)
                )
            }
        }
    }
}