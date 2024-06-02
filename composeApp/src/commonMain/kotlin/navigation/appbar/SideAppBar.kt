package navigation.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import moe.tlaster.precompose.navigation.NavOptions
import moe.tlaster.precompose.navigation.Navigator
import moe.tlaster.precompose.navigation.PopUpTo
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import MainViewModel
import tunerstoolkit.composeapp.generated.resources.*
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.calculate
import tunerstoolkit.composeapp.generated.resources.home
import tunerstoolkit.composeapp.generated.resources.menu

@OptIn(ExperimentalResourceApi::class)
@Composable
fun SideTopAppBar(
    modifier: Modifier = Modifier,
    menuButtonOnClick: () -> Unit,
    viewModel: MainViewModel,
    navigator: Navigator
) {
    val isLoading = viewModel.isLoading
    val currentDestination = navigator.currentEntry.collectAsState(initial = null)

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
                    if (currentDestination.value?.route?.route != "home") {
                        navigator.navigate(
                            route = "home",
                            NavOptions(launchSingleTop = true)
                        )
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
                    if (currentDestination.value?.route?.route != "editor") {
                        navigator.navigate(
                            route = "editor",
                            NavOptions(
                                launchSingleTop = true,
                                popUpTo = PopUpTo(
                                    route = "home",
                                    inclusive = true
                                )
                            )
                        )
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
                    if (currentDestination.value?.route?.route != "settings") {
                        navigator.navigate(
                            route = "settings",
                            NavOptions(launchSingleTop = true)
                        )
                    } },
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