package org.mr0btain.tunerstoolkit.components.utilitytab.buttonbartab.components
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
import tunerstoolkit.composeapp.generated.resources.*
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.copy
import tunerstoolkit.composeapp.generated.resources.download
import tunerstoolkit.composeapp.generated.resources.folder

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
            icon = painterResource(Res.drawable.folder)
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource(Res.drawable.copy)
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource(Res.drawable.download)
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource(Res.drawable.grid_view)
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource(Res.drawable.rule_folder)
        ) {}

        Spacer(modifier = Modifier.padding(4.dp))

        UtilityIconButton(
            icon = painterResource(Res.drawable.save)
        ) {}
    }
}