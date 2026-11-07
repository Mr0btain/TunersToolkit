package org.mr0btain.tunerstoolkit.components.utilitytab.buttonbartab.components
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
import org.mr0btain.tunerstoolkit.MainViewModel
import tunerstoolkit.composeapp.generated.resources.*
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.addchart
import tunerstoolkit.composeapp.generated.resources.assessment
import tunerstoolkit.composeapp.generated.resources.functions

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
            icon = painterResource(Res.drawable.assessment)
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource(Res.drawable.addchart)
        ) {  }

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource(Res.drawable.functions)
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource(Res.drawable.horizontal_split)
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource(Res.drawable.history)
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource(Res.drawable.outlet)
        ) {}
    }
}