package components.calibrationdata.helpers

import components.a2l.recordlayout.RecordLayout
import components.a2l.recordlayout.RecordLOpt
import components.a2l.classes.data.A2lData
import components.a2l.classes.data.AlignmentByte
import components.a2l.classes.data.AlignmentFloat16
import components.a2l.classes.data.AlignmentFloat32
import components.a2l.classes.data.AlignmentFloat64
import components.a2l.classes.data.AlignmentInt
import components.a2l.classes.data.AlignmentLong
import components.a2l.classes.data.AlignmentWord
import components.a2l.classes.enums.DataType

/**
 * Checks an Address and makes sure it is aligned correctly as the A2l file specifies
 */
fun ULong.alignA2lAddress(
    recordLayout: RecordLayout,
    dataType: DataType,
    a2lData: A2lData
): ULong {
    when (dataType) {
        DataType.UBYTE -> {
            if (recordLayout.optionalParameters.containsKey(RecordLOpt.AlignmentByte)) {
                val alignmentByte = recordLayout.optionalParameters[RecordLOpt.AlignmentByte] as? AlignmentByte ?: AlignmentByte()

                if (alignmentByte.num != Int.MIN_VALUE) {
                    val result = this.toInt() % alignmentByte.num
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else if (a2lData.modCommon.alignmentByte != Int.MIN_VALUE) {
                    val alignment = a2lData.modCommon.alignmentByte
                    val result = this.toInt() % alignment
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else {
                    return this
                }
            } else {
                return this
            }
        }

        DataType.SBYTE -> {
            if (recordLayout.optionalParameters.containsKey(RecordLOpt.AlignmentByte)) {
                val alignmentByte = recordLayout.optionalParameters[RecordLOpt.AlignmentByte] as? AlignmentByte ?: AlignmentByte()

                if (alignmentByte.num != Int.MIN_VALUE) {
                    val result = this.toInt() % alignmentByte.num
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else if (a2lData.modCommon.alignmentByte != Int.MIN_VALUE) {
                    val alignment = a2lData.modCommon.alignmentByte
                    val result = this.toInt() % alignment
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else {
                    return this
                }
            } else {
                return this
            }
        }

        DataType.UWORD -> {
            if (recordLayout.optionalParameters.containsKey(RecordLOpt.AlignmentWord)) {
                val alignmentWord = recordLayout.optionalParameters[RecordLOpt.AlignmentWord] as? AlignmentWord ?: AlignmentWord()
                if (alignmentWord.num != Int.MIN_VALUE) {
                    val result = this.toInt() % alignmentWord.num
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else if (a2lData.modCommon.alignmentWord != Int.MIN_VALUE) {
                    val alignment = a2lData.modCommon.alignmentWord
                    val result = this.toInt() % alignment
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else {
                    val result = this.toInt() % 2
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                }
            } else return this
        }

        DataType.SWORD -> {
            if (recordLayout.optionalParameters.containsKey(RecordLOpt.AlignmentWord)) {
                val alignmentWord = recordLayout.optionalParameters[RecordLOpt.AlignmentWord] as? AlignmentWord ?: AlignmentWord()
                if (alignmentWord.num != Int.MIN_VALUE) {
                    val result = this.toInt() % alignmentWord.num
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else if (a2lData.modCommon.alignmentWord != Int.MIN_VALUE) {
                    val alignment = a2lData.modCommon.alignmentWord
                    val result = this.toInt() % alignment
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else {
                    val result = this.toInt() % 2
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                }
            } else return this
        }

        DataType.ULONG -> {
            if (recordLayout.optionalParameters.containsKey(RecordLOpt.AlignmentLong)) {
                val alignmentLong = recordLayout.optionalParameters[RecordLOpt.AlignmentLong] as? AlignmentLong ?: AlignmentLong()

                if (alignmentLong.num != Int.MIN_VALUE) {
                    val result = this.toInt() % alignmentLong.num
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else if (a2lData.modCommon.alignmentLong != Int.MIN_VALUE) {
                    val alignment = a2lData.modCommon.alignmentLong
                    val result = this.toInt() % alignment
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else {
                    val result = this.toInt() % 4
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                }
            } else return this
        }

        DataType.SLONG -> {
            if (recordLayout.optionalParameters.containsKey(RecordLOpt.AlignmentLong)) {
                val alignmentLong = recordLayout.optionalParameters[RecordLOpt.AlignmentLong] as? AlignmentLong ?: AlignmentLong()
                if (alignmentLong.num != Int.MIN_VALUE) {
                    val result = this.toInt() % alignmentLong.num
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else if (a2lData.modCommon.alignmentLong != Int.MIN_VALUE) {
                    val alignment = a2lData.modCommon.alignmentLong
                    val result = this.toInt() % alignment
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else {
                    val result = this.toInt() % 4
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                }
            } else return this
        }

        DataType.AUint64 -> {
            if (recordLayout.optionalParameters.containsKey(RecordLOpt.AlignmentInt64)) {
                val alignmentInt64 = recordLayout.optionalParameters[RecordLOpt.AlignmentInt64] as? AlignmentInt ?: AlignmentInt()

                if (alignmentInt64.num != Int.MIN_VALUE) {
                    val result = this.toInt() % alignmentInt64.num
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else if (a2lData.modCommon.alignmentInt64 != Int.MIN_VALUE) {
                    val alignment = a2lData.modCommon.alignmentInt64
                    val result = this.toInt() % alignment
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else {
                    val result = this.toInt() % 8
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                }
            } else return this
        }

        DataType.AInt64 -> {
            if (recordLayout.optionalParameters.containsKey(RecordLOpt.AlignmentInt64)) {
                val alignmentInt64 = recordLayout.optionalParameters[RecordLOpt.AlignmentInt64] as? AlignmentInt ?: AlignmentInt()

                if (alignmentInt64.num != Int.MIN_VALUE) {
                    val result = this.toInt() % alignmentInt64.num
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else if (a2lData.modCommon.alignmentInt64 != Int.MIN_VALUE) {
                    val alignment = a2lData.modCommon.alignmentInt64
                    val result = this.toInt() % alignment
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else {
                    val result = this.toInt() % 8
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                }
            } else return this
        }

        DataType.Float16Ieee -> {
            if (recordLayout.optionalParameters.containsKey(RecordLOpt.AlignmentFloat16Ieee)) {
                val alignmentFloat16Ieee = recordLayout.optionalParameters[RecordLOpt.AlignmentFloat16Ieee] as? AlignmentFloat16 ?: AlignmentFloat16()

                if (alignmentFloat16Ieee.num != Int.MIN_VALUE) {
                    val result = this.toInt() % alignmentFloat16Ieee.num
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else if (a2lData.modCommon.alignmentFloat16Ieee != Int.MIN_VALUE) {
                    val alignment = a2lData.modCommon.alignmentFloat16Ieee
                    val result = this.toInt() % alignment
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else {
                    val result = this.toInt() % 2
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                }
            } else return this
        }

        DataType.Float32Ieee -> {
            if (recordLayout.optionalParameters.containsKey(RecordLOpt.AlignmentFloat32Ieee)) {
                val alignmentFloat32Ieee = recordLayout.optionalParameters[RecordLOpt.AlignmentFloat32Ieee] as? AlignmentFloat32 ?: AlignmentFloat32()

                if (alignmentFloat32Ieee.num != Int.MIN_VALUE) {
                    val result = this.toInt() % alignmentFloat32Ieee.num
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else if (a2lData.modCommon.alignmentFloat32Ieee != Int.MIN_VALUE) {
                    val alignment = a2lData.modCommon.alignmentFloat32Ieee
                    val result = this.toInt() % alignment
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else {
                    val result = this.toInt() % 4
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                }
            } else return this
        }

        DataType.Float64Ieee -> {
            if (recordLayout.optionalParameters.containsKey(RecordLOpt.AlignmentFloat64Ieee)) {
                val alignmentFloat64Ieee = recordLayout.optionalParameters[RecordLOpt.AlignmentFloat64Ieee] as? AlignmentFloat64 ?: AlignmentFloat64()

                if (alignmentFloat64Ieee.num != Int.MIN_VALUE) {
                    val result = this.toInt() % alignmentFloat64Ieee.num
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else if (a2lData.modCommon.alignmentFloat64Ieee != Int.MIN_VALUE) {
                    val alignment = a2lData.modCommon.alignmentFloat64Ieee
                    val result = this.toInt() % alignment
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                } else {
                    val result = this.toInt() % 8
                    return if (result != 0) {
                        this + result.toULong()
                    } else {
                        this
                    }
                }
            } else return this
        }

        DataType.Default -> {
            return this
        }

        DataType.NOT_SET -> {
            return this
        }
    }
}