package org.mr0btain.tunerstoolkit.components.screenconfig

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

/**
 * Window width buckets, the same breakpoints as Material 3's window size classes. Replaces
 * material3-window-size-class-multiplatform, which was built against an older Compose and crashes on 1.12
 */
enum class WindowWidthSizeClass {
    /** Under 600dp: phones */
    Compact,

    /** 600 to 840dp: tablets, small windows */
    Medium,

    /** 840dp and up: desktops, big tablets */
    Expanded;

    companion object {
        fun fromWidth(widthDp: Float): WindowWidthSizeClass = when {
            widthDp < 600f -> Compact
            widthDp < 840f -> Medium
            else -> Expanded
        }
    }
}

/** Window height buckets, Material 3's breakpoints */
enum class WindowHeightSizeClass {
    /** Under 480dp: phones held sideways */
    Compact,

    /** 480 to 900dp */
    Medium,

    /** 900dp and up */
    Expanded;

    companion object {
        fun fromHeight(heightDp: Float): WindowHeightSizeClass = when {
            heightDp < 480f -> Compact
            heightDp < 900f -> Medium
            else -> Expanded
        }
    }
}

data class WindowSizeClass(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass
)

/** The size class of the window the app is drawn in, updated as it's resized */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun calculateWindowSizeClass(): WindowSizeClass {
    val size = LocalWindowInfo.current.containerSize
    return with(LocalDensity.current) {
        WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.fromWidth(size.width.toDp().value),
            heightSizeClass = WindowHeightSizeClass.fromHeight(size.height.toDp().value)
        )
    }
}
