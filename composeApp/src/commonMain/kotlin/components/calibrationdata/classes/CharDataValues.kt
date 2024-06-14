package components.calibrationdata.classes

import androidx.compose.ui.graphics.Path
import components.parsers.a2l.classes.enums.DataType
import kotlinx.serialization.Serializable

@Serializable
data class CharDataValues(
    val values: MutableList<FormattedValue> = mutableListOf(),
    val byteSegmentId: String = "",
    val baseOffset: ULong = ULong.MIN_VALUE,
    val actualStartPosition: Int = Int.MIN_VALUE,
    val actualEndPosition: Int = Int.MIN_VALUE,
    val dataType: DataType = DataType.NOT_SET,
    val format: String = ""
) {
    private fun getAxisXLength(): Int {
        var result = 0

        values.forEach {
            if (it.column > result) result = it.column
        }

        return result + 1
    }

    private fun getAxisYLength(): Int {
        var result = 0

        values.forEach {
            if (it.row > result) result = it.row
        }

        return result + 1
    }
}