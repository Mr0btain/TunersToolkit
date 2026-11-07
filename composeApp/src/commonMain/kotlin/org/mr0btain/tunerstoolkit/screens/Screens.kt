package org.mr0btain.tunerstoolkit.screens
enum class Screen {
    MAIN,
    EDITOR,
    SETTINGS
}

sealed class NavigationItem(val route: String) {
    object Main : NavigationItem(Screen.MAIN.name)
    object Editor : NavigationItem(Screen.EDITOR.name)
    object Settings : NavigationItem(Screen.SETTINGS.name)
}