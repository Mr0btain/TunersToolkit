package org.mr0btain.tunerstoolkit.ui.gallery

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.ui.kit.hex.HexViewState
import org.mr0btain.tunerstoolkit.ui.kit.hex.TmHexView
import org.mr0btain.tunerstoolkit.ui.settings.SettingsHandler
import org.mr0btain.uikit.foundation.TmSurface
import org.mr0btain.uikit.foundation.TmSurfaceLevel
import org.mr0btain.uikit.gallery.GallerySection
import org.mr0btain.uikit.gallery.KitGallery
import org.mr0btain.uikit.gallery.Specimen

/** The UI kit's gallery, followed by Tuners Toolkit's own components */
@Composable
fun GalleryScreen(settings: SettingsHandler, modifier: Modifier = Modifier) {
    KitGallery(
        palette = settings.palette,
        onPaletteChange = settings::setPalette,
        font = settings.font,
        onFontChange = settings::setFont,
        darkTheme = settings.data.darkMode,
        onDarkThemeChange = settings::setDarkMode,
        reduceTransparency = settings.data.reduceTransparency,
        onReduceTransparencyChange = settings::setReduceTransparency,
        modifier = modifier,
        extraSections = appGallerySections()
    )
}

/** Tuners Toolkit's own components, for the gallery */
fun appGallerySections(): List<@Composable () -> Unit> = listOf({ HexViewSection() }, { MapTableSection() }, { MapChartSection() })

/** Two segments with a gap, a changed byte, a selection and a search match */
private fun sampleImage(): MemoryImage = MemoryImage.Builder().apply {
    put(0x0080_0000L, ByteArray(96) { (it * 7 + 0x20).toByte() })
    put(0x0080_0100L, "TTDEMO1/0.1/2026-11-07".encodeToByteArray() + ByteArray(26) { 0xFF.toByte() })
}.build().image.also { it.write(0x0080_0012L, byteArrayOf(0x42)) }

@Composable
private fun HexViewSection() {
    val image = remember { sampleImage() }
    var version by remember { mutableLongStateOf(image.version) }
    val state = remember {
        HexViewState(16).apply {
            select(AddressRange(0x0080_0020L, 0x0080_0028L))
            highlight = AddressRange(0x0080_0100L, 0x0080_0107L)
        }
    }

    GallerySection(title = "Hex view", description = "Bytes and their text, changed bytes in the warning tone, gaps folded to a row") {
        Specimen("TmHexView") {
            TmSurface(modifier = Modifier.fillMaxWidth().height(240.dp), level = TmSurfaceLevel.SUNKEN) {
                TmHexView(
                    image = image,
                    version = version,
                    state = state,
                    onWrite = { address, bytes ->
                        image.write(address, bytes)
                        version = image.version
                    },
                    modifier = Modifier.fillMaxWidth().height(240.dp)
                )
            }
        }
    }
}
