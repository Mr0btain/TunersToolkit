package components.parsers.a2l.recordlayout

import components.errorstuff.LogDataEntry
import components.errorstuff.safeCall
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
import components.parsers.a2l.classes.data.parseExtendedLimits
import components.parsers.a2l.classes.enums.CalibrationAccess
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

fun buildRecordLayout(recordLayoutContent: List<String>): Pair<RecordLayout, List<LogDataEntry>> {
    val process = "BuildRecordLayout"
    val errors = mutableListOf<LogDataEntry>()
    val optionalParams = mutableMapOf<RecordLOpt, A2lOptional>()
    var indexOrder = 0

    recordLayoutContent.forEach { line ->
        when {
            line.startsWith("ALIGNMENT_BYTE") -> {
                safeCall {
                    optionalParams[RecordLOpt.AlignmentByte] = AlignmentByte(
                        line.replace("ALIGNMENT_BYTE", "").trim().toInt()
                    )
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add AlignmentByte!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("ALIGNMENT_INT64") -> {
                safeCall {
                    optionalParams[RecordLOpt.AlignmentInt64] = AlignmentByte(
                        line.replace("ALIGNMENT_INT64", "").trim().toInt()
                    )
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add AlignmentInt64!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("ALIGNMENT_LONG") -> {
                safeCall {
                    optionalParams[RecordLOpt.AlignmentLong] = AlignmentByte(
                        line.replace("ALIGNMENT_LONG", "").trim().toInt()
                    )
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add AlignmentLong!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("ALIGNMENT_WORD") -> {
                safeCall {
                    optionalParams[RecordLOpt.AlignmentWord] = AlignmentByte(
                        line.replace("ALIGNMENT_WORD", "").trim().toInt()
                    )
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add AlignmentWord!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("ALIGNMENT_FLOAT16_IEEE") -> {
                safeCall {
                    optionalParams[RecordLOpt.AlignmentFloat16Ieee] = AlignmentByte(
                        line.replace("ALIGNMENT_FLOAT16_IEEE", "").trim().toInt()
                    )
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add AlignmentFloat16!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("ALIGNMENT_FLOAT32_IEEE") -> {
                safeCall {
                    optionalParams[RecordLOpt.AlignmentFloat32Ieee] = AlignmentByte(
                        line.replace("ALIGNMENT_FLOAT32_IEEE", "").trim().toInt()
                    )
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add AlignmentFloat32!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("ALIGNMENT_FLOAT64_IEEE") -> {
                safeCall {
                    optionalParams[RecordLOpt.AlignmentFloat64Ieee] = AlignmentByte(
                        line.replace("ALIGNMENT_FLOAT64_IEEE", "").trim().toInt()
                    )
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add AlignmentFloat64!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("STATIC_RECORD_LAYOUT") -> {
                optionalParams[RecordLOpt.StaticRecordLayout] = StaticRL.True
            }

            line.startsWith("STATIC_ADDRESS_OFFSET") -> {
                optionalParams[RecordLOpt.StaticAddressOffsets] = StaticAddressOffsets.True
            }

            line.startsWith("GUARD_RAILS") -> {
                optionalParams[RecordLOpt.GuardRails] = GuardRails.True
            }

            line.startsWith("EXTENDED_LIMITS") -> {
                safeCall {
                    optionalParams[RecordLOpt.ExtendedLimits] = line.parseExtendedLimits()
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to parse ExtendedLimits!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("IDENTIFICATION") -> {
                safeCall {
                    val data = line.replace("IDENTIFICATION", "").trim().split(" ")
                    optionalParams[RecordLOpt.Identification] = RecordLayoutIdentification(
                        data[0].toInt(),
                        data[1].parseDataTypeEnum()
                    )
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add Identification!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("RESERVED") -> {
                safeCall {
                    val data = line.replace("RESERVED", "").trim().split(" ")
                    optionalParams[RecordLOpt.Reserved] = RecordLayoutReserved(
                        position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                        dataType = data[1].parseDataTypeEnum()
                    )
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add Reserved!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("CALIBRATION_ACCESS") -> {
                val data = line.replace("CALIBRATION_ACCESS", "").trim().split(" ")
                val result = data[1].parseCalibrationAccess()

                if (result != CalibrationAccess.NOT_SET) {
                    optionalParams[RecordLOpt.CalibrationAccess] = result
                } else {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add CalibrationAccess!",
                            error = true
                        )
                    )
                }
            }

            line.startsWith("FNC_VALUES") -> {
                safeCall {
                    val data = line.replace("FNC_VALUES", "")
                        .trim().split(" ")

                    optionalParams[RecordLOpt.FncValues] = FncValues(
                        position = data[0].trim().toInt(),
                        dataType = data[1].parseDataTypeEnum(),
                        indexMode = data[2].parseIndexMode(),
                        addressType = data[3].parseAddressType()
                    )
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add FncValues!: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("AXIS_PTS_X") -> {
                line.parseRecordLayoutItem(RecordLOpt.AXIS_PTS_X, errors, optionalParams)
                indexOrder++
            }

            line.startsWith("AXIS_PTS_Y") -> {
                line.parseRecordLayoutItem(RecordLOpt.AXIS_PTS_Y, errors, optionalParams)
                indexOrder++
            }

            line.startsWith("AXIS_PTS_Z") -> {
                line.parseRecordLayoutItem(RecordLOpt.AXIS_PTS_Z, errors, optionalParams)
                indexOrder++
            }

            line.startsWith("AXIS_PTS_4") -> {
                line.parseRecordLayoutItem(RecordLOpt.AXIS_PTS_4, errors, optionalParams)
                indexOrder++
            }

            line.startsWith("AXIS_PTS_5") -> {
                line.parseRecordLayoutItem(RecordLOpt.AXIS_PTS_5, errors, optionalParams)
                indexOrder++
            }

            line.startsWith("AXIS_RESCALE_X") -> {
                line.parseRecordLayoutItem(RecordLOpt.AXIS_RESCALE_X, errors, optionalParams)
                indexOrder++
            }

            line.startsWith("AXIS_RESCALE_Y") -> {
                line.parseRecordLayoutItem(RecordLOpt.AXIS_RESCALE_Y, errors, optionalParams)
                indexOrder++
            }

            line.startsWith("AXIS_RESCALE_Z") -> {
                line.parseRecordLayoutItem(RecordLOpt.AXIS_RESCALE_Z, errors, optionalParams)
                indexOrder++
            }

            line.startsWith("AXIS_RESCALE_4") -> {
                line.parseRecordLayoutItem(RecordLOpt.AXIS_RESCALE_4, errors, optionalParams)
                indexOrder++
            }

            line.startsWith("AXIS_RESCALE_5") -> {
                line.parseRecordLayoutItem(RecordLOpt.AXIS_RESCALE_5, errors, optionalParams)
                indexOrder++
            }

            line.startsWith("NO_RESCALE_X") -> {
                line.parseRecordLayoutItem(RecordLOpt.NO_RESCALE_X, errors, optionalParams)
            }

            line.startsWith("NO_RESCALE_Y") -> {
                line.parseRecordLayoutItem(RecordLOpt.NO_RESCALE_Y, errors, optionalParams)
            }

            line.startsWith("NO_RESCALE_Z") -> {
                line.parseRecordLayoutItem(RecordLOpt.NO_RESCALE_Z, errors, optionalParams)
            }

            line.startsWith("NO_RESCALE_4") -> {
                line.parseRecordLayoutItem(RecordLOpt.NO_RESCALE_4, errors, optionalParams)
            }

            line.startsWith("NO_RESCALE_5") -> {
                line.parseRecordLayoutItem(RecordLOpt.NO_RESCALE_5, errors, optionalParams)
            }

            line.startsWith("DIST_OP_X") -> {
                line.parseRecordLayoutItem(RecordLOpt.DIST_OP_X, errors, optionalParams)
            }

            line.startsWith("DIST_OP_Y") -> {
                line.parseRecordLayoutItem(RecordLOpt.DIST_OP_Y, errors, optionalParams)
            }

            line.startsWith("DIST_OP_Z") -> {
                line.parseRecordLayoutItem(RecordLOpt.DIST_OP_Z, errors, optionalParams)
            }

            line.startsWith("DIST_OP_4") -> {
                line.parseRecordLayoutItem(RecordLOpt.DIST_OP_4, errors, optionalParams)
            }

            line.startsWith("DIST_OP_5") -> {
                line.parseRecordLayoutItem(RecordLOpt.DIST_OP_5, errors, optionalParams)
            }

            line.startsWith("FIX_NO_AXIS_PTS_X") -> {
                line.parseRecordLayoutItem(RecordLOpt.FIX_NO_AXIS_PTS_X, errors, optionalParams)
            }

            line.startsWith("FIX_NO_AXIS_PTS_Y") -> {
                line.parseRecordLayoutItem(RecordLOpt.FIX_NO_AXIS_PTS_Y, errors, optionalParams)
            }

            line.startsWith("FIX_NO_AXIS_PTS_Z") -> {
                line.parseRecordLayoutItem(RecordLOpt.FIX_NO_AXIS_PTS_Z, errors, optionalParams)
            }

            line.startsWith("FIX_NO_AXIS_PTS_4") -> {
                line.parseRecordLayoutItem(RecordLOpt.FIX_NO_AXIS_PTS_4, errors, optionalParams)
            }

            line.startsWith("FIX_NO_AXIS_PTS_5") -> {
                line.parseRecordLayoutItem(RecordLOpt.FIX_NO_AXIS_PTS_5, errors, optionalParams)
            }

            line.startsWith("NO_AXIS_PTS_X") -> {
                line.parseRecordLayoutItem(RecordLOpt.NO_AXIS_PTS_X, errors, optionalParams)
            }

            line.startsWith("NO_AXIS_PTS_Y") -> {
                line.parseRecordLayoutItem(RecordLOpt.NO_AXIS_PTS_Y, errors, optionalParams)
            }

            line.startsWith("NO_AXIS_PTS_Z") -> {
                line.parseRecordLayoutItem(RecordLOpt.NO_AXIS_PTS_Z, errors, optionalParams)
            }

            line.startsWith("NO_AXIS_PTS_4") -> {
                line.parseRecordLayoutItem(RecordLOpt.NO_AXIS_PTS_4, errors, optionalParams)
            }

            line.startsWith("NO_AXIS_PTS_5") -> {
                line.parseRecordLayoutItem(RecordLOpt.NO_AXIS_PTS_5, errors, optionalParams)
            }

            line.startsWith("OFFSET_X") -> {
                line.parseRecordLayoutItem(RecordLOpt.OFFSET_X, errors, optionalParams)
            }

            line.startsWith("OFFSET_Y") -> {
                line.parseRecordLayoutItem(RecordLOpt.OFFSET_Y, errors, optionalParams)
            }

            line.startsWith("OFFSET_Z") -> {
                line.parseRecordLayoutItem(RecordLOpt.OFFSET_Z, errors, optionalParams)
            }

            line.startsWith("OFFSET_4") -> {
                line.parseRecordLayoutItem(RecordLOpt.OFFSET_4, errors, optionalParams)
            }

            line.startsWith("OFFSET_5") -> {
                line.parseRecordLayoutItem(RecordLOpt.OFFSET_5, errors, optionalParams)
            }

            line.startsWith("RIP_ADDR_W") -> {
                line.parseRecordLayoutItem(RecordLOpt.RIP_ADDR_W, errors, optionalParams)
            }

            line.startsWith("RIP_ADDR_X") -> {
                line.parseRecordLayoutItem(RecordLOpt.RIP_ADDR_X, errors, optionalParams)
            }

            line.startsWith("RIP_ADDR_Y") -> {
                line.parseRecordLayoutItem(RecordLOpt.RIP_ADDR_Y, errors, optionalParams)
            }

            line.startsWith("RIP_ADDR_Z") -> {
                line.parseRecordLayoutItem(RecordLOpt.RIP_ADDR_Z, errors, optionalParams)
            }

            line.startsWith("RIP_ADDR_4") -> {
                line.parseRecordLayoutItem(RecordLOpt.RIP_ADDR_4, errors, optionalParams)
            }

            line.startsWith("RIP_ADDR_5") -> {
                line.parseRecordLayoutItem(RecordLOpt.RIP_ADDR_5, errors, optionalParams)
            }

            line.startsWith("SHIFT_OP_X") -> {
                line.parseRecordLayoutItem(RecordLOpt.SHIFT_OP_X, errors, optionalParams)
            }

            line.startsWith("SHIFT_OP_Y") -> {
                line.parseRecordLayoutItem(RecordLOpt.SHIFT_OP_Y, errors, optionalParams)
            }

            line.startsWith("SHIFT_OP_Z") -> {
                line.parseRecordLayoutItem(RecordLOpt.SHIFT_OP_Z, errors, optionalParams)
            }

            line.startsWith("SHIFT_OP_4") -> {
                line.parseRecordLayoutItem(RecordLOpt.SHIFT_OP_4, errors, optionalParams)
            }

            line.startsWith("SHIFT_OP_5") -> {
                line.parseRecordLayoutItem(RecordLOpt.SHIFT_OP_5, errors, optionalParams)
            }

            line.startsWith("SRC_ADDR_X") -> {
                line.parseRecordLayoutItem(RecordLOpt.SRC_ADDR_X, errors, optionalParams)
            }

            line.startsWith("SRC_ADDR_Y") -> {
                line.parseRecordLayoutItem(RecordLOpt.SRC_ADDR_Y, errors, optionalParams)
            }

            line.startsWith("SRC_ADDR_Z") -> {
                line.parseRecordLayoutItem(RecordLOpt.SRC_ADDR_Z, errors, optionalParams)
            }

            line.startsWith("SRC_ADDR_4") -> {
                line.parseRecordLayoutItem(RecordLOpt.SRC_ADDR_4, errors, optionalParams)
            }

            line.startsWith("SRC_ADDR_5") -> {
                line.parseRecordLayoutItem(RecordLOpt.SRC_ADDR_5, errors, optionalParams)
            }
        }
    }

    return Pair(
        RecordLayout(
            name = recordLayoutContent[0].replace("/begin RECORD_LAYOUT", "").trim(),
            nameSet = true,
            optionalParameters = optionalParams
        ),
        errors
    )
}

private fun String.parseRecordLayoutItem(
    item: RecordLOpt,
    error: MutableList<LogDataEntry>,
    optionalParams: MutableMap<RecordLOpt, A2lOptional>,
    indexOrder: Int = 0
) {
    safeCall {
        when {
            item.name.startsWith("FIX_NO_AXIS_PTS") -> {
                val data = this.replace(item.name, "").trim().split(" ")
                optionalParams[item] = FixNoAxisPts(
                    data[0].toInt()
                )
            }

            item.name.startsWith("NO_RESCALE") -> {
                val data = this.replace(item.name, "").trim().split(" ")

                optionalParams[item] = NoRescale(
                    position = data[0].toInt(),
                    dataType = data[1].parseDataTypeEnum()
                )
            }

            item.name.startsWith("NO_AXIS_PTS") -> {
                val data = this.replace(item.name, "").trim().split(" ")
                optionalParams[item] = NoAxisPts(
                    position = data[0].toInt(),
                    dataType = data[1].parseDataTypeEnum()
                )
            }

            item.name.startsWith("AXIS_RESCALE") -> {
                val data = this.replace(item.name, "").trim().split(" ")

                optionalParams[item] = RecordLayoutAxisRescale(
                    position = data[0].toInt(),
                    dataType = data[1].parseDataTypeEnum(),
                    maxNumberOfRescalePairs = data[2].toInt(),
                    indexOrder = indexOrder,
                    indexIncrement = data[3].parseIndexIncrement(),
                    addressType = data[4].parseAddressType()
                )
            }

            item.name.startsWith("AXIS_PTS") -> {
                val data = this.replace(item.name, "").trim().split(" ")

                optionalParams[item] = RecordLayoutAxisPoint(
                    position = data[0].trim().toInt(),
                    dataType = data[1].parseDataTypeEnum(),
                    indexIncrement = data[2].parseIndexIncrement(),
                    addressType = data[3].parseAddressType(),
                    indexOrder = indexOrder
                )
            }

            item.name.startsWith("DIST_OP") -> {
                val data = this.replace(item.name, "").trim().split(" ")

                optionalParams[item] = DistOp(
                    position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                    dataType = data[1].parseDataTypeEnum()
                )
            }

            item.name.startsWith("SHIFT_OP") -> {
                val data = this.replace(item.name, "").trim().split(" ")

                optionalParams[item] = ShiftOp(
                    position = data[0].toIntOrNull() ?: Int.MIN_VALUE,
                    dataType = data[1].parseDataTypeEnum()
                )
            }

            item.name.startsWith("OFFSET_") -> {
                val data = this.replace(item.name, "").trim().split(" ")
                optionalParams[item] = RecordLayoutOffset(
                    position = data[0].toInt(),
                    dataType = data[1].parseDataTypeEnum()
                )
            }

            item.name.startsWith("RIP_ADDR") -> {
                val data = this.replace(item.name, "").trim().split(" ")
                optionalParams[item] = RipAddress(
                    position = data[0].toInt(),
                    dataType = data[1].parseDataTypeEnum()
                )
            }

            item.name.startsWith("SRC_ADDR_" ) -> {
                val data = this.replace(item.name, "").trim().split(" ")
                optionalParams[item] = SrcAddress(
                    position = data[0].toInt(),
                    dataType = data[1].parseDataTypeEnum()
                )
            }
        }
    }.onFailure {
        error.add(
            LogDataEntry(
                process = "BuildRecordLayout",
                message = "Failure to add ${item.name}!: ${it.message}",
                cause = it.cause.toString(),
                error = true
            )
        )
    }
}