@file:Suppress("FunctionName")

package components.utilitytab.maplisttab.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import components.utilitytab.maplisttab.classes.MapDataContainer

@Composable
fun MapListMainItemView(
    container: MapDataContainer,
    modifier: Modifier,
    textStyle: TextStyle
) {
    val isExpanded = remember { mutableStateOf(false) }

    Box(
        modifier = modifier,
    ) {

        // Opening Animation
        val expandTransition = remember {
            expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(300)
            ) + fadeIn(
                animationSpec = tween(300)
            )
        }

        // Closing Animation
        val collapseTransition = remember {
            shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(300)
            ) + fadeOut(
                animationSpec = tween(300)
            )
        }

        Column {
            HeaderView(
                title = container.title,
                textStyle = textStyle,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(textStyle.background)
                    .clickable(
                        indication = null, // Removes the ripple effect on tap
                        interactionSource = remember { MutableInteractionSource() }, // Removes the ripple effect on tap
                        onClick = {
                            isExpanded.value = isExpanded.value != true
                        }
                    )
                    .padding(4.dp)
            )

            Spacer(modifier = Modifier.padding(all = 2.dp))

            AnimatedVisibility(
                visible = isExpanded.value,
                enter = expandTransition,
                exit = collapseTransition
            ) {
                Box(
                    modifier = Modifier.wrapContentHeight()
                ) {
                    Column {
                        container.children.forEach { item ->
                            MapListSubItemView(
                                buttonText = item,
                                textStyle = textStyle,
                                onClick = {  } // TODO: Add the logic for this
                            )
                        }
                    }
                }
            }
        }
    }
}