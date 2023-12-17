package components.parsers.a2l.ifdata

import components.errorstuff.LogDataEntry
import components.errorstuff.safeCall
import components.parsers.a2l.ifdata.components.Raster
import components.parsers.a2l.ifdata.components.enums.BlobTypes
import components.parsers.a2l.ifdata.components.enums.IfDataTypes
import components.parsers.a2l.ifdata.components.enums.InternalLocation
import components.parsers.a2l.ifdata.components.parseAndFormatRasters

fun buildIfData(input: String): Pair<IfData, List<LogDataEntry>> {
    val process = "BuildIfData"
    val errors = mutableListOf<LogDataEntry>()
    val data = input.replace("/begin IF_DATA", "")
        .replace("/end IF_DATA", "").trim().split("\\s+".toRegex())
    var type: IfDataTypes = IfDataTypes.NOT_SET
    var blobType: BlobTypes = BlobTypes.NOT_SET
    var addrExtension = ULong.MIN_VALUE  /* Address extension of the calibration data (only Low Byte significant) */
    var baseAddress = ULong.MIN_VALUE
    var mappedAddress = ULong.MIN_VALUE
    var length = ULong.MIN_VALUE  /* Number of Bytes belonging to the calibration data  */
    var internExtern: InternalLocation = InternalLocation.NOT_SET
    var raster = Raster()

    data.forEachIndexed { index, line ->
        when {
            line.startsWith("/*orig_adr:*/") -> {
                safeCall {
                    baseAddress = line.replace("/*orig_adr:*/", "")
                        .trim().replace("0x", "").toULong(16)
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add Orig_Addr: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }

            }

            line.startsWith("/*mapping_adr:*/") -> {
                safeCall {
                    mappedAddress = line.replace("/*mapping_adr:*/", "")
                        .trim().replace("0x", "").toULong(16)
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add Mapping_Addr: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("/*length:*/") -> {
                safeCall {
                    length = line.replace("/*length:*/", "")
                        .trim().replace("0x", "").toULong(16)
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to add length: ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("DP_BLOB") -> {
                blobType = BlobTypes.DP_BLOB
                if (data.size >= index + 1) {
                    when (data[index + 1].trim()) {
                        "INTERN" -> {
                            internExtern = InternalLocation.INTERN
                            addrExtension = ULong.MIN_VALUE
                        }
                        "EXTERN" -> {
                            internExtern = InternalLocation.EXTERN
                            addrExtension = ULong.MIN_VALUE
                        }
                        else ->  {
                            safeCall {
                                addrExtension = data[index + 1].trim().replace("0x", "").toULong(16)
                            }.onFailure {
                                errors.add(
                                    LogDataEntry(
                                        process = process,
                                        message = "Failure to add Address Extension: ${it.message}",
                                        cause = it.cause.toString(),
                                        error = true
                                    )
                                )
                            }
                        }
                    }
                }

                if (data.size >= index + 2) {
                    when (data[index + 2].trim()) {
                        "INTERN" -> {
                            internExtern = InternalLocation.INTERN
                            baseAddress = ULong.MIN_VALUE
                        }
                        "EXTERN" -> {
                            internExtern = InternalLocation.EXTERN
                            baseAddress = ULong.MIN_VALUE
                        }
                        else -> {
                            safeCall {
                                baseAddress = data[index + 2].trim().replace("0x", "").toULong(16)
                            }.onFailure {
                                errors.add(
                                    LogDataEntry(
                                        process = process,
                                        message = "Failure to add base_Addr: ${it.message}",
                                        cause = it.cause.toString(),
                                        error = true
                                    )
                                )
                            }
                        }
                    }
                }

                if (data.size >= index + 3) {
                    safeCall {
                        length = data[index + 3].trim().replace("0x", "").toULong(16)
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failure to add length ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }
            }

            line.startsWith("TP_BLOB") -> {
                blobType = BlobTypes.TP_BLOB
                if (data.size > index + 1) {
                    when (data[index + 1].trim()) {
                        "INTERN" -> {
                            internExtern = InternalLocation.INTERN
                            addrExtension = ULong.MIN_VALUE
                        }
                        "EXTERN" -> {
                            internExtern = InternalLocation.EXTERN
                            addrExtension = ULong.MIN_VALUE
                        }
                        else ->  {
                            safeCall {
                                addrExtension = data[index + 1].trim().replace("0x", "").toULong(16)
                            }.onFailure {
                                errors.add(
                                    LogDataEntry(
                                        process = process,
                                        message = "Failure to add Addr_Extension: ${it.message}",
                                        cause = it.cause.toString(),
                                        error = true
                                    )
                                )
                            }
                        }
                    }
                }

                if (data.size >= index + 2) {
                    when (data[index + 2].trim()) {
                        "INTERN" -> {
                            internExtern = InternalLocation.INTERN
                            baseAddress = ULong.MIN_VALUE
                        }
                        "EXTERN" -> {
                            internExtern = InternalLocation.EXTERN
                            baseAddress = ULong.MIN_VALUE
                        }
                        else -> {
                            safeCall {
                                baseAddress = data[index + 2].trim().replace("0x", "").toULong(16)
                            }.onFailure {
                                errors.add(
                                    LogDataEntry(
                                        process = process,
                                        message = "Failure to add base_Addr: ${it.message}",
                                        cause = it.cause.toString(),
                                        error = true
                                    )
                                )
                            }
                        }
                    }
                }

                if (data.size >= index + 3) {
                    safeCall {
                        length = data[index + 3].trim().replace("0x", "").toULong(16)
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failure to add length: ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }
            }

            line.startsWith("KP_BLOB") -> {
                blobType = BlobTypes.KP_BLOB
                if (data.size >= index + 1) {
                    when (data[index + 1].trim()) {
                        "INTERN" -> {
                            internExtern = InternalLocation.INTERN
                            addrExtension = ULong.MIN_VALUE
                        }
                        "EXTERN" -> {
                            internExtern = InternalLocation.EXTERN
                            addrExtension = ULong.MIN_VALUE
                        }
                        else ->  {
                            safeCall {
                                addrExtension = data[index + 1].trim().replace("0x", "").toULong(16)
                            }.onFailure {
                                errors.add(
                                    LogDataEntry(
                                        process = process,
                                        message = "Failure to add Addr_extension: ${it.message}",
                                        cause = it.cause.toString(),
                                        error = true
                                    )
                                )
                            }
                        }
                    }
                }

                if (data.size >= index + 2) {
                    when (data[index + 2].trim()) {
                        "INTERN" -> {
                            internExtern = InternalLocation.INTERN
                            baseAddress = ULong.MIN_VALUE
                        }
                        "EXTERN" -> {
                            internExtern = InternalLocation.EXTERN
                            baseAddress = ULong.MIN_VALUE
                        }
                        else -> {
                            safeCall {
                                baseAddress = data[index + 2].trim().replace("0x", "").toULong(16)
                            }.onFailure {
                                errors.add(
                                    LogDataEntry(
                                        process = process,
                                        message = "Failure to add base_Addr: ${it.message}",
                                        cause = it.cause.toString(),
                                        error = true
                                    )
                                )
                            }
                        }
                    }
                }

                if (data.size >= index + 3) {
                    safeCall {
                        length = data[index + 3].trim().replace("0x", "").toULong(16)
                    }.onFailure {
                        errors.add(
                            LogDataEntry(
                                process = process,
                                message = "Failure to add Length: ${it.message}",
                                cause = it.cause.toString(),
                                error = true
                            )
                        )
                    }
                }
            }

            line.startsWith("ASAP1B_CCP") -> {
                type = IfDataTypes.ASAP1B_CCP
            }

            line.startsWith("ASAP1B_KWP2000") -> {
                type = IfDataTypes.ASAP1B_KWP2000
            }

            line.startsWith("ETK") -> {
                type = IfDataTypes.ETK
            }

            line.startsWith("/begin RASTER") -> {
                raster = parseAndFormatRasters(
                    data.subList(index, data.size).joinToString(" ")
                )
            }
        }
    }

    return Pair(
        IfData(
            name = data[0],
            type = type,
            blobType = blobType,
            addrExtension = addrExtension,
            baseAddress = baseAddress,
            mappedAddress = mappedAddress,
            length = length,
            internExtern = internExtern,
            raster = raster
        ),
        errors
    )
}