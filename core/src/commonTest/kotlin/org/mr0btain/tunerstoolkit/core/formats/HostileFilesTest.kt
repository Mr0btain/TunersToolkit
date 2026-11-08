package org.mr0btain.tunerstoolkit.core.formats

import org.mr0btain.tunerstoolkit.core.calibration.CalibrationCodec
import org.mr0btain.tunerstoolkit.core.calibration.CalibrationObject
import org.mr0btain.tunerstoolkit.core.calibration.Conversion
import org.mr0btain.tunerstoolkit.core.calibration.DataType
import org.mr0btain.tunerstoolkit.core.calibration.ObjectKind
import org.mr0btain.tunerstoolkit.core.calibration.RecordField
import org.mr0btain.tunerstoolkit.core.calibration.Storage
import org.mr0btain.tunerstoolkit.core.calibration.ValueOrder
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.formats.ttdef.TtDef
import org.mr0btain.tunerstoolkit.core.formats.values.Cdfx
import org.mr0btain.tunerstoolkit.core.formats.values.Csv
import org.mr0btain.tunerstoolkit.core.image.ByteOrder
import org.mr0btain.tunerstoolkit.core.image.MemoryImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Files made to break a reader get an error, not a crash */
class HostileFilesTest {
    @Test
    fun deeplyNestedTtdef() {
        val deep = "{\"format\": \"ttdef\", \"formatVersion\": 1, \"title\": \"x\", \"folders\": " + "[{\"name\": \"a\", \"folders\": ".repeat(50_000) + "[]" + "}]".repeat(50_000) + "}"
        assertEquals(TkResult.Failure(ImportError.UNKNOWN_FORMAT), TtDef.read(deep.encodeToByteArray()))
        assertEquals(3, TtDef.nesting("{\"a\": [\"[[[[\", {}]}"), "brackets in strings don't count")
    }

    @Test
    fun cdfxSizesThatOverflow() {
        val text = """<MSRSW><SW-INSTANCE><SHORT-NAME>Huge</SHORT-NAME><CATEGORY>VAL_BLK</CATEGORY><SW-VALUE-CONT>
            <SW-ARRAYSIZE><V>65536</V><V>65536</V><V>65537</V></SW-ARRAYSIZE><SW-VALUES-PHYS><V>1</V></SW-VALUES-PHYS></SW-VALUE-CONT></SW-INSTANCE>
            <SW-INSTANCE><SHORT-NAME>Negative</SHORT-NAME><CATEGORY>VAL_BLK</CATEGORY><SW-VALUE-CONT>
            <SW-ARRAYSIZE><V>-1</V><V>-1</V></SW-ARRAYSIZE><SW-VALUES-PHYS><V>1</V></SW-VALUES-PHYS></SW-VALUE-CONT></SW-INSTANCE></MSRSW>"""
        val read = (Cdfx.read(text.encodeToByteArray()) as TkResult.Success).data
        assertTrue(read.value.isEmpty())
        assertEquals(listOf("Huge", "Negative"), read.diagnostics.map { it.objectName })
    }

    @Test
    fun csvTextCantRunAsAFormula() {
        val obj = CalibrationObject(
            "=HYPERLINK(\"x\")", "", ObjectKind.VALUE, 0, Storage(listOf(RecordField.Values(DataType.UBYTE, ValueOrder.X_FIRST)), ByteOrder.BIG_ENDIAN),
            Conversion.Verbal(listOf(5.0 to "-bad"))
        )
        val values = (CalibrationCodec.decode(MemoryImage.of(0L to byteArrayOf(5)), obj) as TkResult.Success).data
        val text = Csv.writeMap(values)
        assertTrue(text.startsWith("\"'=HYPERLINK"), text)
        assertTrue(";'-bad" in text, text)
        assertEquals(listOf("-bad"), (Csv.readMap(text, values) as TkResult.Success).data.texts, "the ' comes off again")
    }
}
