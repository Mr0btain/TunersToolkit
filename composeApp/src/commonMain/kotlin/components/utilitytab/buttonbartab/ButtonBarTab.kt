package components.utilitytab.buttonbartab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.datastorage.components.Keyboard
import components.datastorage.components.keyboardAsState
import components.utilitytab.buttonbartab.components.UtilityButtonBar1
import components.utilitytab.buttonbartab.components.UtilityButtonBar2
import components.utilitytab.components.UtilityActionAnimated
import components.utilitytab.components.UtilityDialog
import kotlinx.coroutines.CoroutineScope
import screens.editor.components.MainViewModel
import ui.theme.TunerToolkitTheme

@Composable
fun ButtonBarTab(
    mainViewModel: MainViewModel,
    scrollState: ScrollState,
    scope: CoroutineScope,
    focusRequester: FocusRequester
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    // Variables
    val inputTextStyle = remember {
        mutableStateOf(
            TextStyle(
                fontFamily = mainViewModel.JetBrainsMono,
                fontSize = 20.sp,
                color = backgroundColor
            )
        )
    }
    val useDialog = remember { mutableStateOf(false) }
    val inputLabel = remember { mutableStateOf("") }
    val input = remember { mutableStateOf(0.0) }
    val openInput = remember { mutableStateOf(false) }
    val isKeyboardOpen = keyboardAsState() // Keyboard.Opened or Keyboard.Closed
    val isExpanded = remember { mutableStateOf(true) }
    if (!openInput.value) { isExpanded.value = isKeyboardOpen.value != Keyboard.Opened }

    // Opening Animation for hidden bars
    val expandTransition = remember {
        expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(300)
        ) + fadeIn(
            animationSpec = tween(300)
        )
    }

    // Closing Animation for hidden bars
    val collapseTransition = remember {
        shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(100)
        ) + fadeOut(
            animationSpec = tween(100)
        )
    }

    TunerToolkitTheme {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .wrapContentHeight()
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(4.dp)
        ) {
            // Animate the visibility of other button bars when keyboard is open
            AnimatedVisibility(
                visible = isExpanded.value,
                enter = expandTransition,
                exit = collapseTransition
            ) {
                Column(
                    modifier = Modifier.wrapContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // First of the expandable utility bars
                    UtilityButtonBar1()

                    // Second of the expandable utility bars
                    UtilityButtonBar2(mainViewModel, scope, focusRequester)

                    // Not sure if this will be included
                    //UtilityButtonBar3()
                }
            }
        }

        // Check if we want to use Dialog popup or Row for input
        if (useDialog.value) {
            // Add button Dialog
            if (openInput.value) {
                // TODO: Make dialog box have a nice surrounding box
                UtilityDialog(
                    dismissState = { openInput.value = false },
                    label = inputLabel.value,
                    onClick = {
                        input.value = it
                        openInput.value = false
                    },
                    cancelClick = { openInput.value = false },
                    textStyle = inputTextStyle.value,
                )
            }
        } else {
            // Use Row that can be always shown or hidden
            // TODO: Add setting for Utility input bar and test
            val alwaysShowInputRow = remember { mutableStateOf(false) }
            UtilityActionAnimated(
                label = inputLabel.value,
                textStyle = inputTextStyle.value,
                onClick = {
                    input.value = it
                    openInput.value = false
                },
                alwaysShow = alwaysShowInputRow.value,
                isOpen = openInput.value
            )
        }
    }
}