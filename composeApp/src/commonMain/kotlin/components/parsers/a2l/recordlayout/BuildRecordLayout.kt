package components.parsers.a2l.recordlayout

import components.parsers.a2l.classes.RecordLayoutAxisPoint
import components.parsers.a2l.classes.RecordLayoutAxisRescale
import components.parsers.a2l.classes.RecordLayoutIdentification
import components.parsers.a2l.classes.RecordLayoutOffset
import components.parsers.a2l.classes.RecordLayoutReserved
import components.parsers.a2l.classes.data.AlignmentByte
import components.parsers.a2l.classes.data.AlignmentFloat16
import components.parsers.a2l.classes.data.AlignmentFloat32
import components.parsers.a2l.classes.data.AlignmentFloat64
import components.parsers.a2l.classes.data.AlignmentInt
import components.parsers.a2l.classes.data.AlignmentLong
import components.parsers.a2l.classes.data.AlignmentWord
import components.parsers.a2l.classes.data.DistOp
import components.parsers.a2l.classes.data.ExtendedLimits
import components.parsers.a2l.classes.data.FixNoAxisPts
import components.parsers.a2l.classes.data.FncValues
import components.parsers.a2l.classes.data.NoAxisPts
import components.parsers.a2l.classes.data.NoRescale
import components.parsers.a2l.classes.data.RipAddress
import components.parsers.a2l.classes.data.ShiftOp
import components.parsers.a2l.classes.data.SrcAddress
import components.parsers.a2l.classes.enums.StaticRL
import components.parsers.a2l.classes.enums.GuardRails
import components.parsers.a2l.classes.enums.StaticAddressOffsets
import components.parsers.a2l.classes.enums.parseAddressType
import components.parsers.a2l.classes.enums.parseCalibrationAccess
import components.parsers.a2l.classes.enums.parseDataTypeEnum
import components.parsers.a2l.classes.enums.parseIndexIncrement
import components.parsers.a2l.classes.enums.parseIndexMode
import components.parsers.a2l.objects.A2lOptional

/**
 * Record Layout parser and builder
 *
 * Parses and sorts items in a list of strings in a Record Layout Block
 *
 * EG: /begin RECORD_LAYOUT .... /end RECORD_LAYOUT
 */

fun buildRecordLayout(recordLayoutContent: List<String>): RecordLayout {
    val mOptionalParams = mutableMapOf<RecordLOpt, A2lOptional>()
    var mIndexOrder = 0

    try {
        for (line in recordLayoutContent) {
            when {
                line.startsWith("ALIGNMENT_BYTE") -> {
                    mOptionalParams[RecordLOpt.AlignmentByte] = AlignmentByte(
                        line.replace("ALIGNMENT_BYTE", "").trim().toIntOrNull() ?: Int.MIN_VALUE
                    )
                }

                line.startsWith("ALIGNMENT_INT64") -> {
                    mOptionalParams[RecordLOpt.AlignmentInt64] = AlignmentInt(
                        line.replace("ALIGNMENT_INT64", "").trim().toIntOrNull() ?: Int.MIN_VALUE
                    )
                }

                line.startsWith("ALIGNMENT_LONG") -> {
                    mOptionalParams[RecordLOpt.AlignmentLong] = AlignmentLong(
                        line.replace("ALIGNMENT_LONG", "").trim().toIntOrNull() ?: Int.MIN_VALUE
                    )
                }

                line.startsWith("ALIGNMENT_WORD") -> {
                    mOptionalParams[RecordLOpt.AlignmentWord] = AlignmentWord(
                        line.replace("ALIGNMENT_WORD", "").trim().toIntOrNull() ?: Int.MIN_VALUE
                    )
                }

                line.startsWith("ALIGNMENT_FLOAT16_IEEE") -> {
                    mOptionalParams[RecordLOpt.AlignmentFloat16Ieee] = AlignmentFloat16(
                        line.replace("ALIGNMENT_FLOAT16_IEEE", "").trim().toIntOrNull() ?: Int.MIN_VALUE
                    )
                }

                line.startsWith("ALIGNMENT_FLOAT32_IEEE") -> {
                    mOptionalParams[RecordLOpt.AlignmentFloat32Ieee] = AlignmentFloat32(
                        line.replace("ALIGNMENT_FLOAT32_IEEE", "").trim().toIntOrNull() ?: Int.MIN_VALUE
                    )
                }

                line.startsWith("ALIGNMENT_FLOAT64_IEEE") -> {
                    mOptionalParams[RecordLOpt.AlignmentFloat64Ieee] = AlignmentFloat64(
                        line.replace("ALIGNMENT_FLOAT64_IEEE", "").trim().toIntOrNull() ?: Int.MIN_VALUE
                    )
                }

                line.startsWith("STATIC_RECORD_LAYOUT") -> {
                    mOptionalParams[RecordLOpt.StaticRecordLayout] = StaticRL.True
                }

                line.startsWith("STATIC_ADDRESS_OFFSET") -> {
                    mOptionalParams[RecordLOpt.StaticAddressOffsets] = StaticAddressOffsets.True
                }

                line.startsWith("GUARD_RAILS") -> {
                    mOptionalParams[RecordLOpt.GuardRails] = GuardRails.True
                }

                line.startsWith("EXTENDED_LIMITS") -> {
                    val data = line.replace("EXTENDED_LIMITS", "").trim().split(" ")
                    if (data.size >= 2) {
                        val lower = data[0].toDoubleOrNull()
                        val upper = data[1].toDoubleOrNull()
                        if (lower != null && upper != null) {
                            mOptionalParams[RecordLOpt.ExtendedLimits] = ExtendedLimits(
                                lowerLimit = lower,
                                upperLimit = upper
                            )
                        }
                    }
                }

                line.startsWith("IDENTIFICATION") -> {
                    val data = line.replace("IDENTIFICATION", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.Identification] = RecordLayoutIdentification(
                        data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("RESERVED") -> {
                    val data = line.replace("RESERVED", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.Reserved] = RecordLayoutReserved(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("CALIBRATION_ACCESS") -> {
                    val data = line.replace("CALIBRATION_ACCESS", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.CalibrationAccess] = data[1].parseCalibrationAccess()
                }

                line.startsWith("FNC_VALUES") -> {
                    val data = line.replace("FNC_VALUES", "")
                        .trim().split(" ")

                    mOptionalParams[RecordLOpt.FncValues] = FncValues(
                        position = data[0].trim().toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum(),
                        indexMode = data[2].parseIndexMode(),
                        addressType = data[3].parseAddressType()
                    )
                }

                line.startsWith("AXIS_PTS_X") -> {
                    val data = line.replace("AXIS_PTS_X", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.AXIS_PTS_X] = RecordLayoutAxisPoint(
                        position = data[0].trim().toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum(),
                        indexIncrement = data[2].parseIndexIncrement(),
                        addressType = data[3].parseAddressType(),
                        indexOrder = mIndexOrder
                    )
                    mIndexOrder++
                }

                line.startsWith("AXIS_PTS_Y") -> {
                    val data = line.replace("AXIS_PTS_Y", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.AXIS_PTS_Y] = RecordLayoutAxisPoint(
                        position = data[0].trim().toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum(),
                        indexIncrement = data[2].parseIndexIncrement(),
                        addressType = data[3].parseAddressType(),
                        indexOrder = mIndexOrder
                    )
                    mIndexOrder++
                }

                line.startsWith("AXIS_PTS_Z") -> {
                    val data = line.replace("AXIS_PTS_Z", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.AXIS_PTS_Z] = RecordLayoutAxisPoint(
                        position = data[0].trim().toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum(),
                        indexIncrement = data[2].parseIndexIncrement(),
                        addressType = data[3].parseAddressType(),
                        indexOrder = mIndexOrder
                    )
                    mIndexOrder++
                }

                line.startsWith("AXIS_PTS_4") -> {
                    val data = line.replace("AXIS_PTS_4", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.AXIS_PTS_4] = RecordLayoutAxisPoint(
                        position = data[0].trim().toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum(),
                        indexIncrement = data[2].parseIndexIncrement(),
                        addressType = data[3].parseAddressType(),
                        indexOrder = mIndexOrder
                    )
                    mIndexOrder++
                }

                line.startsWith("AXIS_PTS_5") -> {
                    val data = line.replace("AXIS_PTS_5", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.AXIS_PTS_5] = RecordLayoutAxisPoint(
                        position = data[0].trim().toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum(),
                        indexIncrement = data[2].parseIndexIncrement(),
                        addressType = data[3].parseAddressType(),
                        indexOrder = mIndexOrder
                    )
                    mIndexOrder++
                }

                line.startsWith("AXIS_RESCALE_X") -> {
                    val data = line.replace("AXIS_RESCALE_X", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.AXIS_RESCALE_X] = RecordLayoutAxisRescale(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum(),
                        maxNumberOfRescalePairs = data[2].toIntOrNull() ?: Int.MIN_VALUE,
                        indexOrder = mIndexOrder,
                        indexIncrement = data[3].parseIndexIncrement(),
                        addressType = data[4].parseAddressType()
                    )
                    mIndexOrder++
                }

                line.startsWith("AXIS_RESCALE_Y") -> {
                    val data = line.replace("AXIS_RESCALE_Y", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.AXIS_RESCALE_Y] = RecordLayoutAxisRescale(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum(),
                        maxNumberOfRescalePairs = data[2].toIntOrNull() ?: Int.MIN_VALUE,
                        indexOrder = mIndexOrder,
                        indexIncrement = data[3].parseIndexIncrement(),
                        addressType = data[4].parseAddressType()
                    )
                    mIndexOrder++
                }

                line.startsWith("AXIS_RESCALE_Z") -> {
                    val data = line.replace("AXIS_RESCALE_Z", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.AXIS_RESCALE_Z] = RecordLayoutAxisRescale(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum(),
                        maxNumberOfRescalePairs = data[2].toIntOrNull() ?: Int.MIN_VALUE,
                        indexOrder = mIndexOrder,
                        indexIncrement = data[3].parseIndexIncrement(),
                        addressType = data[4].parseAddressType()
                    )
                    mIndexOrder++
                }

                line.startsWith("AXIS_RESCALE_4") -> {
                    val data = line.replace("AXIS_RESCALE_4", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.AXIS_RESCALE_4] = RecordLayoutAxisRescale(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum(),
                        maxNumberOfRescalePairs = data[2].toIntOrNull() ?: Int.MIN_VALUE,
                        indexOrder = mIndexOrder,
                        indexIncrement = data[3].parseIndexIncrement(),
                        addressType = data[4].parseAddressType()
                    )
                    mIndexOrder++
                }

                line.startsWith("AXIS_RESCALE_5") -> {
                    val data = line.replace("AXIS_RESCALE_5", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.AXIS_RESCALE_5] = RecordLayoutAxisRescale(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum(),
                        maxNumberOfRescalePairs = data[2].toIntOrNull() ?: Int.MIN_VALUE,
                        indexOrder = mIndexOrder,
                        indexIncrement = data[3].parseIndexIncrement(),
                        addressType = data[4].parseAddressType()
                    )
                    mIndexOrder++
                }

                line.startsWith("NO_RESCALE_X") -> {
                    val data = line.replace("NO_RESCALE_X", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.NO_RESCALE_X] = NoRescale(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("NO_RESCALE_Y") -> {
                    val data = line.replace("NO_RESCALE_Y", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.NO_RESCALE_Y] = NoRescale(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("NO_RESCALE_Z") -> {
                    val data = line.replace("NO_RESCALE_Z", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.NO_RESCALE_Z] = NoRescale(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("NO_RESCALE_4") -> {
                    val data = line.replace("NO_RESCALE_4", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.NO_RESCALE_4] = NoRescale(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("NO_RESCALE_5") -> {
                    val data = line.replace("NO_RESCALE_5", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.NO_RESCALE_5] = NoRescale(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("DIST_OP_X") -> {
                    val data = line.replace("DIST_OP_X", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.DIST_OP_X] = DistOp(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("DIST_OP_Y") -> {
                    val data = line.replace("DIST_OP_Y", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.DIST_OP_Y] = DistOp(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("DIST_OP_Z") -> {
                    val data = line.replace("DIST_OP_Z", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.DIST_OP_Z] = DistOp(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("DIST_OP_4") -> {
                    val data = line.replace("DIST_OP_4", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.DIST_OP_4] = DistOp(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("DIST_OP_5") -> {
                    val data = line.replace("DIST_OP_5", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.DIST_OP_5] = DistOp(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("FIX_NO_AXIS_PTS_X") -> {
                    val data = line.replace("FIX_NO_AXIS_PTS_X", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.FIX_NO_AXIS_PTS_X] = FixNoAxisPts(
                        data[0].toIntOrNull() ?: Int.MIN_VALUE
                    )
                }

                line.startsWith("FIX_NO_AXIS_PTS_Y") -> {
                    val data = line.replace("FIX_NO_AXIS_PTS_Y", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.FIX_NO_AXIS_PTS_Y] = FixNoAxisPts(
                        data[0].toIntOrNull() ?: Int.MIN_VALUE
                    )
                }

                line.startsWith("FIX_NO_AXIS_PTS_Z") -> {
                    val data = line.replace("FIX_NO_AXIS_PTS_Z", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.FIX_NO_AXIS_PTS_Z] = FixNoAxisPts(
                        data[0].toIntOrNull() ?: Int.MIN_VALUE
                    )
                }

                line.startsWith("FIX_NO_AXIS_PTS_4") -> {
                    val data = line.replace("FIX_NO_AXIS_PTS_4", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.FIX_NO_AXIS_PTS_4] = FixNoAxisPts(
                        data[0].toIntOrNull() ?: Int.MIN_VALUE
                    )
                }

                line.startsWith("FIX_NO_AXIS_PTS_5") -> {
                    val data = line.replace("FIX_NO_AXIS_PTS_5", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.FIX_NO_AXIS_PTS_5] = FixNoAxisPts(
                        data[0].toIntOrNull() ?: Int.MIN_VALUE
                    )
                }

                line.startsWith("NO_AXIS_PTS_X") -> {
                    val data = line.replace("NO_AXIS_PTS_X", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.NO_AXIS_PTS_X] = NoAxisPts(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("NO_AXIS_PTS_Y") -> {
                    val data = line.replace("NO_AXIS_PTS_Y", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.NO_AXIS_PTS_Y] = NoAxisPts(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("NO_AXIS_PTS_Z") -> {
                    val data = line.replace("NO_AXIS_PTS_Z", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.NO_AXIS_PTS_Z] = NoAxisPts(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("NO_AXIS_PTS_4") -> {
                    val data = line.replace("NO_AXIS_PTS_4", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.NO_AXIS_PTS_4] = NoAxisPts(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("NO_AXIS_PTS_5") -> {
                    val data = line.replace("NO_AXIS_PTS_5", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.NO_AXIS_PTS_5] = NoAxisPts(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("OFFSET_X") -> {
                    val data = line.replace("OFFSET_X", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.OFFSET_X] = RecordLayoutOffset(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("OFFSET_Y") -> {
                    val data = line.replace("OFFSET_Y", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.OFFSET_Y] = RecordLayoutOffset(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("OFFSET_Z") -> {
                    val data = line.replace("OFFSET_Z", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.OFFSET_Z] = RecordLayoutOffset(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("OFFSET_4") -> {
                    val data = line.replace("OFFSET_4", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.OFFSET_4] = RecordLayoutOffset(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("OFFSET_5") -> {
                    val data = line.replace("OFFSET_5", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.OFFSET_5] = RecordLayoutOffset(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("RIP_ADDR_W") -> {
                    val data = line.replace("RIP_ADDR_W", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.RIP_ADDR_W] = RipAddress(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("RIP_ADDR_X") -> {
                    val data = line.replace("RIP_ADDR_X", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.RIP_ADDR_X] = RipAddress(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("RIP_ADDR_Y") -> {
                    val data = line.replace("RIP_ADDR_Y", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.RIP_ADDR_Y] = RipAddress(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("RIP_ADDR_Z") -> {
                    val data = line.replace("RIP_ADDR_Z", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.RIP_ADDR_Z] = RipAddress(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("RIP_ADDR_4") -> {
                    val data = line.replace("RIP_ADDR_4", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.RIP_ADDR_4] = RipAddress(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("RIP_ADDR_5") -> {
                    val data = line.replace("RIP_ADDR_5", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.RIP_ADDR_5] = RipAddress(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("SHIFT_OP_X") -> {
                    val data = line.replace("SHIFT_OP_X", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.SHIFT_OP_X] = ShiftOp(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("SHIFT_OP_Y") -> {
                    val data = line.replace("SHIFT_OP_Y", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.SHIFT_OP_Y] = ShiftOp(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("SHIFT_OP_Z") -> {
                    val data = line.replace("SHIFT_OP_Z", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.SHIFT_OP_Z] = ShiftOp(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("SHIFT_OP_4") -> {
                    val data = line.replace("SHIFT_OP_4", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.SHIFT_OP_4] = ShiftOp(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("SHIFT_OP_5") -> {
                    val data = line.replace("SHIFT_OP_5", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.SHIFT_OP_5] = ShiftOp(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("SRC_ADDR_X") -> {
                    val data = line.replace("SRC_ADDR_X", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.SRC_ADDR_X] = SrcAddress(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("SRC_ADDR_Y") -> {
                    val data = line.replace("SRC_ADDR_Y", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.SRC_ADDR_Y] = SrcAddress(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("SRC_ADDR_Z") -> {
                    val data = line.replace("SRC_ADDR_Z", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.SRC_ADDR_Z] = SrcAddress(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("SRC_ADDR_4") -> {
                    val data = line.replace("SRC_ADDR_4", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.SRC_ADDR_4] = SrcAddress(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }

                line.startsWith("SRC_ADDR_5") -> {
                    val data = line.replace("SRC_ADDR_5", "").trim().split(" ")
                    mOptionalParams[RecordLOpt.SRC_ADDR_5] = SrcAddress(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return RecordLayout(
        name = recordLayoutContent[0].replace("/begin RECORD_LAYOUT", "").trim(),
        nameSet = true,
        optionalParameters = mOptionalParams
    )
}