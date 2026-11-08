package org.mr0btain.tunerstoolkit.ui.settings

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mr0btain.applogger.AppLogger
import org.mr0btain.uikit.theme.TmFont
import org.mr0btain.uikit.theme.TmFonts
import org.mr0btain.uikit.theme.TmPalette
import org.mr0btain.uikit.theme.TmPalettes

/** The saved settings, saved again on each change */
@Stable
class SettingsHandler(private val scope: CoroutineScope, initial: SettingsData = loadSettings()) {
    var data by mutableStateOf(initial)
        private set

    val palette: TmPalette get() = TmPalettes.byId(data.palette)
    val font: TmFont get() = TmFonts.byId(data.font)

    fun update(change: (SettingsData) -> SettingsData) {
        val updated = change(data)
        if (updated == data) return
        data = updated
        scope.launch(Dispatchers.Default) {
            try {
                saveSettingsData(updated)
            } catch (e: Exception) {
                AppLogger.e("$TAG:Save", "Couldn't save the settings", e)
            }
        }
    }

    fun setPalette(palette: TmPalette) = update { it.copy(palette = palette.id) }
    fun setFont(font: TmFont) = update { it.copy(font = font.id) }
    fun setDarkMode(dark: Boolean) = update { it.copy(darkMode = dark) }
    fun setReduceTransparency(reduce: Boolean) = update { it.copy(reduceTransparency = reduce) }
    fun setHexBytesPerRow(bytes: Int) = update { it.copy(hexBytesPerRow = bytes) }

    private companion object {
        const val TAG = "SettingsHandler"

        fun loadSettings(): SettingsData = try {
            loadSettingsData()
        } catch (e: Exception) {
            AppLogger.w("$TAG:Load", "Saved settings couldn't be read, using the defaults", e)
            SettingsData()
        }
    }
}
