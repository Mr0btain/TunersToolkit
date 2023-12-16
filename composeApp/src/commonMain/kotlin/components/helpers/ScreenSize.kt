package components.helpers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import getPlatform
import MainViewModel

/**
 * Updates the view model with the size of the screen we have to work with as well as
 * calculating the text size for our offset, editor, and ascii text boxes
 */
@Composable
fun ScreenSize(viewModel: MainViewModel, widthSizeClass: WindowWidthSizeClass) {
    var size by remember { mutableStateOf(IntSize(0, 0)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { size = it.size }
    ) {
        val firstTextSizeSet = remember { mutableStateOf(false) }
        val screenS = viewModel.screenSize.value
        val textSizeCalculated = if (getPlatform().name == "Java") {
            if (widthSizeClass == WindowWidthSizeClass.Compact) size.width / 90 else if (size.width / 56 >= 20) 20 else size.width / 56
        } else { size.width / 90 }

        if (size.width != screenS.width) {
            viewModel.textSize.value = textSizeCalculated.sp
        } else if (!firstTextSizeSet.value) {
            viewModel.textSize.value = textSizeCalculated.sp
            firstTextSizeSet.value = true
        }

        viewModel.screenSize.value = IntSize(size.width, size.height)
    }
}
