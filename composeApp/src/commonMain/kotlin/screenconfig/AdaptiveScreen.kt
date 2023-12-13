package screenconfig

import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import getPlatform


@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
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