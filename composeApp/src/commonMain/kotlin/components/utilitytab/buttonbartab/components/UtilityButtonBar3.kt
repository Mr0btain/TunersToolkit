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
fun UtilityButtonBar3() {
    Row(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        UtilityIconButton(
            icon = painterResource("icons/file_present.png")
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource("icons/folder.png")
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource("icons/bar_chart/.png")
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource("icons/add_comment.png")
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource("icons/find_in_page.png")
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource("icons/folder_special.png")
        ) {}
    }
}