package components.utilitytab.projecttab.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import MainViewModel

// TODO: Maybe make drop down?

@Composable
fun EcuField(
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    ecuBrand: String,
    ecuModel: String,
    ecuVersion: String,
    viewModel: MainViewModel
) {
    val mBrand = remember(viewModel.projectInfo.value.ecuBrand) { mutableStateOf(ecuBrand) }
    val mModel = remember(viewModel.projectInfo.value.ecuModel) { mutableStateOf(ecuModel) }
    val mVersion = remember(viewModel.projectInfo.value.ecuVersion) { mutableStateOf(ecuVersion) }
    val isBrandMatch = derivedStateOf { viewModel.projectInfo.value.ecuBrand != mBrand.value }
    val isModelMatch = derivedStateOf { viewModel.projectInfo.value.ecuModel != mModel.value }
    val isVersionMatch = derivedStateOf { viewModel.projectInfo.value.ecuVersion != mVersion.value }

    if (isBrandMatch.value) {
        viewModel.projectInfo.value = viewModel.projectInfo.value.copy(ecuBrand = mBrand.value)
    }

    if (isModelMatch.value) {
        viewModel.projectInfo.value = viewModel.projectInfo.value.copy(ecuModel = mModel.value)
    }

    if (isVersionMatch.value) {
        viewModel.projectInfo.value = viewModel.projectInfo.value.copy(ecuVersion = mVersion.value)
    }

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
                text = "Ecu",
                style = TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = textStyle.fontSize.times(1.8),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.padding(4.dp)
            )

            Row(
                modifier = Modifier
                    .padding(4.dp)
                    .wrapContentHeight()
                    .fillMaxWidth()
            ) {
                InputTextOption(
                    label = "Brand:",
                    textFieldText = mBrand.value,
                    textFieldOnChange = { mBrand.value = it },
                    textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSecondaryContainer)
                )

                InputTextOption(
                    label = "Model:",
                    textFieldText = mModel.value,
                    textFieldOnChange = { mModel.value = it },
                    textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSecondaryContainer)
                )
            }

            Row(
                modifier = Modifier
                    .padding(4.dp)
                    .wrapContentHeight()
                    .fillMaxWidth()
            ) {
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