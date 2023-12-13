package components.utilitytab.buttonbartab.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
fun UtilityButtonBar1() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        UtilityIconButton(
            icon = painterResource("icons/folder.png")
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource("icons/file_copy.png")
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource("icons/download.png")
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource("icons/grid_view.png")
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource("icons/rule_folder.png")
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource("icons/save.png")
        ) {}
    }
}