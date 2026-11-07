package org.mr0btain.tunerstoolkit.components.screenconfig
import androidx.compose.animation.AnimatedContent
import org.mr0btain.tunerstoolkit.components.screenconfig.WindowWidthSizeClass
import org.mr0btain.tunerstoolkit.components.screenconfig.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.mr0btain.tunerstoolkit.getPlatform

@Composable
fun AdaptiveScreen(
    modifier: Modifier = Modifier,
    compactContent: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit
) {
    when (getPlatform().name) {
        "Java" -> expandedContent()

        else -> {
            val windowSizeClass = calculateWindowSizeClass()

            AnimatedContent(
                targetState = windowSizeClass.widthSizeClass,
                modifier = modifier
            ) {
                when (it) {
                    WindowWidthSizeClass.Compact -> compactContent()
                    else -> expandedContent()
                }
            }
        }
    }
}