package org.mr0btain.tunerstoolkit.screens.settings.classes
import kotlinx.serialization.Serializable

/**
 * The app's saved settings. New fields need defaults, so settings saved by an older version still load
 * @param palette A TmPalette id from the UI kit
 * @param font A TmFont id from the UI kit
 * @param hexBytesPerRow How many bytes each hex view row shows: 8, 16 or 32
 */
@Serializable
data class SettingsData(
    val showTopUtilityTabBar: Boolean = true,
    val hidePagerOnKeyUp: Boolean = true,
    val alwaysDarkMode: Boolean = false,
    val digitsPerLine: Int = 16,
    val byteSize: Int = 8,
    val palette: String = "bench",
    val font: String = "barlow",
    val darkMode: Boolean = true,
    val reduceTransparency: Boolean = false,
    val hexBytesPerRow: Int = 16
)