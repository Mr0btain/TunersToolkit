package org.mr0btain.tunerstoolkit.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.mr0btain.tunerstoolkit.core.calibration.AxisSource
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationAxis
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationCodec
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationObject
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationValues
import org.mr0btain.tunerstoolkit.core.calibration.CellSelection
import org.mr0btain.tunerstoolkit.core.calibration.Conversion
import org.mr0btain.tunerstoolkit.core.calibration.DataType
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.calibration.RecordField
import org.mr0btain.tunerstoolkit.core.calibration.Storage
import org.mr0btain.tunerstoolkit.core.calibration.ValueOrder
import org.mr0btain.tunerstoolkit.core.calibration.writeAll
import org.mr0btain.tunerstoolkit.core.error.getOrNull
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import org.mr0btain.tunerstoolkit.ui.kit.map.MapTableMode
import org.mr0btain.tunerstoolkit.ui.kit.map.MapTableState
import org.mr0btain.tunerstoolkit.ui.kit.map.TmMapChart2D
import org.mr0btain.tunerstoolkit.ui.kit.map.TmMapChart3D
import org.mr0btain.tunerstoolkit.ui.kit.map.TmMapTable
import org.mr0btain.uikit.foundation.TmSurface
import org.mr0btain.uikit.foundation.TmSurfaceLevel
import org.mr0btain.uikit.gallery.GallerySection
import org.mr0btain.uikit.gallery.Specimen
import org.mr0btain.uikit.selection.TmSegmented
import org.mr0btain.uikit.theme.TmTheme

/** A made up 8 x 6 ignition map on fixed axes, and a copy with a few cells changed */
private class SampleMap(val image: MemoryImage, val obj: CalibrationObject) {
    fun read(from: MemoryImage = image): CalibrationValues? = CalibrationCodec.decode(from, obj).getOrNull()
}

private fun sampleMap(): SampleMap {
    val speeds = listOf(800.0, 1500.0, 2500.0, 3500.0, 4500.0, 5500.0, 6500.0, 7000.0)
    val loads = listOf(20.0, 40.0, 60.0, 80.0, 100.0, 120.0)
    val bytes = ByteArray(speeds.size * loads.size) { i ->
        val x = i % speeds.size
        val y = i / speeds.size
        (60 + x * 9 - y * 7).coerceIn(0, 255).toByte()
    }
    val image = MemoryImage.of(0x1000L to bytes)
    val obj = CalibrationObject(
        name = "IgnitionBase",
        description = "Base ignition advance",
        kind = ObjectKind.MAP,
        address = 0x1000,
        storage = Storage(listOf(RecordField.Values(DataType.UBYTE, ValueOrder.X_FIRST)), ByteOrder.LITTLE_ENDIAN),
        conversion = Conversion.Linear(0.375, -10.0),
        axes = listOf(
            CalibrationAxis(AxisSource.Fixed(speeds), Conversion.Identical, speeds.size, input = "EngineSpeed", unit = "rpm", format = "%4.0"),
            CalibrationAxis(AxisSource.Fixed(loads), Conversion.Identical, loads.size, input = "EngineLoad", unit = "%", format = "%3.0")
        ),
        unit = "deg",
        format = "%5.2",
        lowerLimit = -10.0,
        upperLimit = 50.0
    )
    return SampleMap(image, obj)
}

@Composable
internal fun MapTableSection() {
    val sample = remember { sampleMap() }
    val original = remember { sample.read() }
    var version by remember { mutableStateOf(0) }
    val changed = remember {
        sample.image.snapshot().also { copy -> sample.read(copy)?.let { CalibrationCodec.writeAll(copy, it, mapOf(10 to 30.0, 11 to 31.0, 19 to 33.0)) } }
    }
    val values = remember(version) { sample.read(changed) } ?: return
    val state = remember { MapTableState().apply { select(CellSelection(1..3, 1..2)) } }
    var mode by remember { mutableStateOf(MapTableMode.VALUES) }

    GallerySection(
        title = "Map table",
        description = "Values with their axes, tinted low to high; changed cells marked; a selection; and the difference from another image"
    ) {
        Specimen("TmMapTable") {
            Column(verticalArrangement = Arrangement.spacedBy(TmTheme.spacing.sm)) {
                TmSegmented(options = MapTableMode.entries, selected = mode, onSelect = { mode = it }, optionLabel = { it.label })
                TmSurface(modifier = Modifier.fillMaxWidth().height(280.dp), level = TmSurfaceLevel.SUNKEN) {
                    TmMapTable(
                        values = values,
                        state = state,
                        original = original,
                        compareWith = original,
                        mode = mode,
                        onChange = { changes ->
                            CalibrationCodec.writeAll(changed, values, changes)
                            version++
                        },
                        modifier = Modifier.fillMaxSize().padding(TmTheme.spacing.xs)
                    )
                }
            }
        }
        Specimen("TmMapTable, difference") {
            TmSurface(modifier = Modifier.fillMaxWidth().height(280.dp), level = TmSurfaceLevel.SUNKEN) {
                TmMapTable(values = values, state = remember { MapTableState() }, compareWith = original, mode = MapTableMode.DIFFERENCE, modifier = Modifier.fillMaxSize().padding(TmTheme.spacing.xs))
            }
        }
    }
}

@Composable
internal fun MapChartSection() {
    val sample = remember { sampleMap() }
    val original = remember { sample.read() } ?: return
    val changed = remember {
        sample.image.snapshot().also { copy -> CalibrationCodec.writeAll(copy, original, mapOf(10 to 30.0, 11 to 31.0, 12 to 33.0)) }
    }
    val values = remember { sample.read(changed) } ?: return
    GallerySection(title = "Map charts", description = "Rows of a map as lines (the original dashed behind the highlighted one), and the map as a surface to turn round") {
        Specimen("TmMapChart2D") {
            TmSurface(modifier = Modifier.fillMaxWidth().height(260.dp), level = TmSurfaceLevel.SUNKEN) {
                TmMapChart2D(values = values, original = original, highlightRow = 1, modifier = Modifier.fillMaxSize().padding(TmTheme.spacing.md))
            }
        }
        Specimen("TmMapChart3D") {
            TmSurface(modifier = Modifier.fillMaxWidth().height(300.dp), level = TmSurfaceLevel.SUNKEN) {
                TmMapChart3D(values = values, modifier = Modifier.fillMaxSize().padding(TmTheme.spacing.md))
            }
        }
    }
}
