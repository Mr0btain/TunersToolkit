package components.calibrationdata.classes

import components.a2l.classes.enums.DataType
import components.maptextoverlay.classes.PathData
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
    val overlayPath: PathData = PathData()
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