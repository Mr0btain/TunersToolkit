package components.utilitytab.buttonbartab.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import MainViewModel

@OptIn(ExperimentalResourceApi::class)
@Composable
fun UtilityButtonBar2(
    viewModel: MainViewModel,
    scope: CoroutineScope,
    focusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        UtilityIconButton(
            icon = painterResource("icons/assignment.png")
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource("icons/addchart.png")
        ) {  }

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource("icons/functions.png")
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource("icons/horizontal_split.png")
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource("icons/history.png")
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource("icons/outlet.png")
        ) {}
    }
}