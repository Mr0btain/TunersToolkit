import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import components.datastorage.components.getSettings
import components.datastorage.components.saveSettings
import components.errorstuff.Logger
import components.hextexteditor.components.*
import components.maptextoverlay.classes.MapValuesSorted
import components.parsers.a2l.classes.data.A2lData
import components.parsers.hex.HexSegmentData
import components.parsers.hex.asciiToHex
import components.parsers.hex.hexToAscii
import components.utilitytab.maplisttab.classes.MapDataContainer
import components.utilitytab.projecttab.classes.ProjectInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
import screens.settings.classes.SettingsData
import ui.theme.md_theme_light_error
import ui.theme.md_theme_light_inverseSurface
import ui.theme.md_theme_light_primary

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
            println("Updating Hex data from Ascii")
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
            val start = range.first / 2
            val ascii = text.hexToAscii()

            asciiEditorData.text.replaceRange(
                startIndex = start,
                endIndex = start + ascii.length,
                replacement = ascii
            )
        } else {
            asciiEditorData.text.replaceRange(range, text)
        }
    }

    fun updateSettings(input: SettingsData) {
        settings.value = input
        viewModelScope.launch(Dispatchers.IO) { saveSettings(input) }
    }

    fun toolbarNameReset() { appBarTitle.value = "Tuners Toolkit" }

    private fun asciiPositionToHexPosition(position: Int): Int {
        val positionWithoutNewLines = asciiEditorData.text.substring(0, position).replace("\n", "").length * 2
        val spacesProceeding = positionWithoutNewLines / bitEncoding.value.number()
        val newLinesProceeding = asciiEditorData.text.subSequence(0, position).count { it == '\n' }

        println("PositionWithoutLines: $positionWithoutNewLines")
        println("Spaces: $spacesProceeding")
        println("newLines: $newLinesProceeding")

        return positionWithoutNewLines + spacesProceeding + newLinesProceeding
    }
}