@file:Suppress("FunctionName")

package org.mr0btain.tunerstoolkit.navigation.drawer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@Composable
fun SideMenuDrawerItem(
    icon: Painter,
    label: String,
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition,
    exitTransition: ExitTransition,
    items: List<SideMenuDrawerItemChildren> = listOf(),
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column {
        NavigationDrawerItem(
            icon = { Icon(icon, contentDescription = "") },
            label = { Text(label) },
            selected = isSelected,
            onClick = onClick,
            modifier = modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        AnimatedVisibility(
            visible = isSelected,
            enter = enterTransition,
            exit = exitTransition,
            modifier = Modifier.padding(start = 20.dp)
        ) {
            Column {
                items.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        selected = false,
                        onClick = item.onClick,
                        modifier = modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    }
}