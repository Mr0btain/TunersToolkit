package components.calibrationdata.classes

import androidx.compose.ui.graphics.Path
import components.a2l.classes.enums.DataType
import kotlinx.serialization.Serializable

@Serializable
data class CharDataValues(
    val values: MutableList<FormattedValue> = mutableListOf(),
    val byteSegmentId: String = "",
    val baseOffset: ULong = ULong.MIN_VALUE,
    val actualStartPosition: Int = Int.MIN_VALUE,
    val actualEndPosition: Int = Int.MIN_VALUE,
    val dataType: DataType = DataType.NOT_SET,
    val format: String = "",
    val overlayPath: Path = Path()
) {
    private fun getAxisXLength(): Int {
        var mResult = 0

        values.forEach {
            if (it.column > mResult) mResult = it.column
        }

        return mResult + 1
    }

    private fun getAxisYLength(): Int {
        var mResult = 0

        values.forEach {
            if (it.row > mResult) mResult = it.row
        }

        return mResult + 1
    }
}