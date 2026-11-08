package org.mr0btain.tunerstoolkit.core.calibration

/** One number of an object: a value, or a point of one of its axes */
sealed interface CellRef {
    data class Value(val index: Int) : CellRef
    data class AxisPoint(val axis: Int, val index: Int) : CellRef
}

/**
 * The value or axis point stored at [address], if it's one of this object's own (a shared axis's points belong to the
 * shared object)
 */
fun CalibrationValues.cellAt(address: Long): CellRef? {
    slots?.indexAt(address)?.let { return CellRef.Value(it) }
    axes.forEachIndexed { axis, values ->
        if (values.sharedWith == null) values.slots?.indexAt(address)?.let { return CellRef.AxisPoint(axis, it) }
    }
    return null
}

private fun Slots.indexAt(address: Long): Int? {
    val size = type.size
    for (i in addresses.indices) {
        if (address >= addresses[i] && address < addresses[i] + size) return i
    }
    return null
}

/** Where value [index] is along each dimension, x first: the reverse of [CalibrationValues.index] */
fun CalibrationValues.position(index: Int): IntArray {
    var rest = index
    return IntArray(dimensions.size) { d ->
        (rest % dimensions[d]).also { rest /= dimensions[d] }
    }
}
