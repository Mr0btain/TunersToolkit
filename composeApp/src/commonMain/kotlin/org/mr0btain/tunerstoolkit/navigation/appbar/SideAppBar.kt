@file:Suppress("FunctionName")

package org.mr0btain.tunerstoolkit.navigation.appbar
import org.mr0btain.tunerstoolkit.MainViewModel
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
    navHostController: NavHostController
) {
    val isLoading = viewModel.isLoading
    val oldDestination =  navHostController.currentDestination?.route ?: ""

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
                    if ((navHostController.currentDestination?.route ?: "") != "MAIN") {
                        navHostController.navigate(route = "MAIN")
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
                    if ((navHostController.currentDestination?.route ?: "") != "EDITOR") {
                        navHostController.navigate(route = "EDITOR")
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
                    if ((navHostController.currentDestination?.route ?: "") != "SETTINGS") {
                        navHostController.navigate(route = "SETTINGS")
                    } else {
                        navHostController.navigate(oldDestination)
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