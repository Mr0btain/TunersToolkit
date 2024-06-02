package components.mappopup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import getPlatform
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.clear
import tunerstoolkit.composeapp.generated.resources.tool_menu

@OptIn(ExperimentalResourceApi::class)
@Composable
fun GridWindowButtonBar(
    closeOnClick: () -> Unit,
    modifier: Modifier = Modifier
    ) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = modifier.padding(end = 2.dp)
    ) {

        // Menu Button
        IconButton(
            onClick = {  }
        ) { Icon(
            painterResource(Res.drawable.tool_menu),
            contentDescription = "Menu",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = if (getPlatform().name == "Java") Modifier.size(20.dp) else Modifier
        ) }

        // X button to close
        IconButton(
            onClick = closeOnClick
        ) { Icon(
            painterResource(Res.drawable.clear),
            contentDescription = "Close Window",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = if (getPlatform().name == "Java") Modifier.size(20.dp) else Modifier
        ) }
    }
}