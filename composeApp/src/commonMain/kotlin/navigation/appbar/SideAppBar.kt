@file:Suppress("FunctionName")

package navigation.appbar

import MainViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import org.jetbrains.compose.resources.painterResource
import tunerstoolkit.composeapp.generated.resources.*


@Composable
fun SideTopAppBar(
    modifier: Modifier = Modifier,
    menuButtonOnClick: () -> Unit,
    viewModel: MainViewModel,
    navigator: NavHostController
) {
    val isLoading = viewModel.isLoading
    val currentDestination = navigator.currentDestination?.route ?: ""
    val oldDestination =  navigator.currentDestination?.route ?: ""

    Column(
        modifier = modifier
            .fillMaxHeight()
            .wrapContentWidth()
            .zIndex(1f)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Top
    ) {
        Box(modifier = Modifier.padding(4.dp)) {
            if (isLoading.value) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).padding(4.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                IconButton(
                    onClick = menuButtonOnClick
                ) {
                    Icon(
                        painterResource(Res.drawable.menu),
                        contentDescription = "Open Nav Drawer",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(Modifier.padding(top = 4.dp))

        Column(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = {
                    if (currentDestination != "MAIN") {
                        navigator.navigate(route = "MAIN")
                    }
                          },
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    painterResource(Res.drawable.home),
                    "Home",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = {
                    if (currentDestination != "EDITOR") {
                        navigator.navigate(route = "EDITOR")
                    } },
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    painterResource(Res.drawable.calculate),
                    "Editor",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = {
                    if ((navigator.currentDestination?.route ?: "") != "SETTINGS") {
                        navigator.navigate(route = "SETTINGS")
                    } else {
                        navigator.navigate(oldDestination)
                    }
                          },
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    painterResource(Res.drawable.settings),
                    "Settings",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}