package components.utilitytab.projecttab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import components.utilitytab.projecttab.classes.ProjectInfo
import components.utilitytab.projecttab.components.EcuField
import components.utilitytab.projecttab.components.ProjectField
import components.utilitytab.projecttab.components.VehicleField
import MainViewModel

@Composable
fun ProjectInfoTab(
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    viewModel: MainViewModel
) {
    val mData = viewModel.projectInfo.value

    Box(modifier = modifier) {
        ProjectInfoView(
            modifier = modifier,
            data = mData,
            textStyle = textStyle,
            viewModel = viewModel
        )
    }
}

@Composable
fun ProjectInfoView(
    modifier: Modifier = Modifier,
    data: ProjectInfo = ProjectInfo(),
    textStyle: TextStyle = TextStyle.Default,
    viewModel: MainViewModel
) {
    val mScrollState = rememberScrollState()

    Box(modifier = modifier) {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(4.dp)
                .fillMaxWidth()
                .verticalScroll(state = mScrollState)
        ) {
            ProjectField(
                modifier = Modifier.padding(4.dp),
                name = data.name,
                version = data.projectVersion,
                textStyle = textStyle,
                viewModel = viewModel
            )

            VehicleField(
                modifier = Modifier.padding(4.dp),
                manufacturer = data.manufacturer,
                model = data.model,
                engine = data.engine,
                transmission = data.transmission,
                textStyle = textStyle,
                viewModel = viewModel
            )

            EcuField(
                modifier = Modifier.padding(4.dp),
                ecuBrand = data.ecuBrand,
                ecuModel = data.ecuModel,
                ecuVersion = data.ecuVersion,
                textStyle = textStyle,
                viewModel = viewModel
            )
        }
    }
}


