package org.mr0btain.tunerstoolkit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import org.mr0btain.tunerstoolkit.components.screenconfig.WindowWidthSizeClass
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.mr0btain.tunerstoolkit.components.datastorage.components.getSettings
import org.mr0btain.tunerstoolkit.components.datastorage.components.saveSettings
import org.mr0btain.tunerstoolkit.components.errorstuff.Logger
import org.mr0btain.tunerstoolkit.components.hextexteditor.components.*
import org.mr0btain.tunerstoolkit.components.maptextoverlay.classes.MapValuesSorted
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.A2lData
import org.mr0btain.tunerstoolkit.components.parsers.hex.HexSegmentData
import org.mr0btain.tunerstoolkit.components.parsers.hex.asciiToHex
import org.mr0btain.tunerstoolkit.components.parsers.hex.hexToAscii
import org.mr0btain.tunerstoolkit.components.utilitytab.maplisttab.classes.MapDataContainer
import org.mr0btain.tunerstoolkit.components.utilitytab.projecttab.classes.ProjectInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mr0btain.tunerstoolkit.screens.settings.classes.SettingsData
import org.mr0btain.tunerstoolkit.ui.theme.md_theme_light_error
import org.mr0btain.tunerstoolkit.ui.theme.md_theme_light_inverseSurface
import org.mr0btain.tunerstoolkit.ui.theme.md_theme_light_primary

@OptIn(ExperimentalFoundationApi::class)
@Suppress("PropertyName", "MemberVisibilityCanBePrivate")
class MainViewModel() : ViewModel() {
    val appBarTitle = mutableStateOf("Tuners Toolkit")
    val a2lData = mutableStateOf(A2lData())
    val hexEditorData = TextFieldState()
    val asciiActualEditorData = mutableStateOf("")
    val asciiEditorData = TextFieldState()
    val hexDataSegments = mutableStateMapOf<String, HexSegmentData>()
    val mapData = mutableStateOf(MapValuesSorted())
    val selectedHexSegment = mutableStateOf("")
    val isLoading = mutableStateOf(false)
    val projectInfo = mutableStateOf(ProjectInfo())
    val bitEncoding = mutableStateOf(BitEncoding.EIGHT)
    val digitsPerLine = mutableIntStateOf(8)
    val textSize = mutableStateOf(18.sp)
    val screenSize = mutableStateOf(IntSize(0,0))
    val mapListItems = mutableStateListOf<MapDataContainer>()
    val JetBrainsMono = FontFamily.Monospace
    val firstVisibleItemIdx = mutableIntStateOf(0)
    val firstVisibleItemOffset = mutableIntStateOf(0)
    val utilitySelection = mutableIntStateOf(0)
    val settings = mutableStateOf(getSettings())
    val showNewProjectDialog = mutableStateOf(false)
    val showNewVersionDialog = mutableStateOf(false)
    val showOpenVersionDialog = mutableStateOf(false)
    val showOpenProjectDialog = mutableStateOf(false)
    val showDeleteProjectDialog = mutableStateOf(false)
    val showDeleteVersionDialog = mutableStateOf(false)
    val showImportDialog = mutableStateOf(false)
    val mapFillColor = mutableStateOf(md_theme_light_inverseSurface)
    val mapOutlineColor = mutableStateOf(md_theme_light_primary)
    val mapSelectedOutlineColor = mutableStateOf(md_theme_light_error)
    val windowSizeClass = mutableStateOf(WindowWidthSizeClass.Compact)
    val logger = Logger()

    // TODO: Remove these test options
    val show07kTest = mutableStateOf(false)

    fun setAsciiInitialText(text: String) {
        // Set the visual representation that the user sees
        asciiEditorData.setTextAndPlaceCursorAtEnd(
            text.convertHexToAscii().formatForAsciiEditor(digitsPerLine.intValue)
        )

        // Set the actual representation that we need to work with
        asciiActualEditorData.value = text.hexToAscii()
    }

    fun setHexEditorInitialText(text: String) {
        // Set the Hex data
        hexEditorData.setTextAndPlaceCursorAtEnd(
            text.formatHexStringForEditor(bitEncoding.value, digitsPerLine.intValue)
        )
    }

    /**
     * Way to handle updating Hex editor from the Ascii editor, or anywhere we may need to
     */

    fun updateHexEditorText(
        text: String,
        range: IntRange,
        asciiRange: Boolean
    ) {
        // Check if its from the Ascii editor, as every digit takes up 2 digits in hex
        if (asciiRange) {
            val start = asciiPositionToHexPosition(range.first)
            val hex = text.asciiToHex()

            hexEditorData.edit {
                replace(start, start + hex.length, hex)
            }
        } else {
            hexEditorData.edit { replace(range.first, range.first + text.length, text) }
        }
    }

    /**
     * Way to handle updating Ascii editor from the Hex editor, or anywhere we may need to
     */

    fun updateAsciiEditorText(
        text: String,
        range: IntRange,
        hexRange: Boolean
    ) {
        if (hexRange) {
            val (actualStart, editorStart) = hexPositionToAsciiPosition(range.first)
            val ascii = text.hexToAscii()

            asciiEditorData.edit { replace(editorStart, editorStart + 1, getAsciiEditorCharacters(ascii)) }
            asciiActualEditorData.value.replaceRange(actualStart, actualStart + 1, ascii)
        } else {
            val newlineCharCount = asciiEditorData.text.substring(0, range.first).count { it == '\n' }

            asciiEditorData.edit { replace(range.first, range.last, getAsciiEditorCharacters(text)) }
            asciiActualEditorData.value.replaceRange(range.first - newlineCharCount, range.last - newlineCharCount, text)
        }
    }

    fun updateSettings(input: SettingsData) {
        settings.value = input
        viewModelScope.launch(Dispatchers.Default) { saveSettings(input) }
    }

    fun toolbarNameReset() { appBarTitle.value = "Tuners Toolkit" }

    private fun asciiPositionToHexPosition(position: Int): Int {
        val positionWithoutNewLines = asciiEditorData.text.substring(0, position).replace("\n", "").length * 2
        val newLinesProceeding = asciiEditorData.text.substring(0, position).count { it == '\n' }
        val spacesProceeding = positionWithoutNewLines / bitEncoding.value.number() - newLinesProceeding

        return positionWithoutNewLines + spacesProceeding + newLinesProceeding
    }

    private fun hexPositionToAsciiPosition(position: Int): Pair<Int, Int> {
        val newLineChars = hexEditorData.text.substring(0, position).count { it == '\n' }
        val actualPos = hexEditorData.text.substring(0, position)
            .replace(" ", "")
            .replace("\n", "").length / bitEncoding.value.number()

        return Pair(actualPos, actualPos + newLineChars)
    }

    private fun getAsciiEditorCharacters(input: String): String {
        val builder = StringBuilder()

        for (char in input) {
            if (char.isLetterOrDigit() && char.code < 128) {
                builder.append(char)
            } else builder.append('.')
        }

        return builder.toString()
    }
}