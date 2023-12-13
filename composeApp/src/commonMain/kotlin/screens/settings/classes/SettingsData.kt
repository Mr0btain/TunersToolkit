package screens.settings.classes

import kotlinx.serialization.Serializable

@Serializable
data class SettingsData(
    val showTopUtilityTabBar: Boolean = true,
    val hidePagerOnKeyUp: Boolean = true,
    val alwaysDarkMode: Boolean = false,
    val digitsPerLine: Int = 16,
    val byteSize: Int = 8
)