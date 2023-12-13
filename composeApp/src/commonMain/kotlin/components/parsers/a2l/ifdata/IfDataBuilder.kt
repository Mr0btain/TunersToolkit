package components.parsers.a2l.ifdata

import components.parsers.a2l.ifdata.components.Raster
import components.parsers.a2l.ifdata.components.enums.BlobTypes
import components.parsers.a2l.ifdata.components.enums.IfDataTypes
import components.parsers.a2l.ifdata.components.enums.InternalLocation
import components.parsers.a2l.ifdata.components.parseAndFormatRasters

fun buildIfData(input: String): components.parsers.a2l.ifdata.IfData {
    // Multiple checks in line with functions to check size to make sure
    // we don't throw out of bounds
    val data = input.replace("/begin IF_DATA", "")
        .replace("/end IF_DATA", "").trim().split("\\s+".toRegex())

    var mType: IfDataTypes = IfDataTypes.NOT_SET
    var mBlobType: BlobTypes = BlobTypes.NOT_SET
    var mAddrExtension = ULong.MIN_VALUE  /* Address extension of the calibration data (only Low Byte significant) */
    var mBaseAddress = ULong.MIN_VALUE
    var mMappedAddress = ULong.MIN_VALUE
    var mLength = ULong.MIN_VALUE  /* Number of Bytes belonging to the calibration data  */
    var mInternExtern: InternalLocation = InternalLocation.NOT_SET
    var mRaster = Raster()

    try {
        data.forEachIndexed { index, line ->
            when {
                line.startsWith("/*orig_adr:*/") -> {
                    val result = line.replace("/*orig_adr:*/", "")
                        .trim().replace("0x", "").toULongOrNull(16)
                    if (result != null) {
                        mBaseAddress = result
                    }


                }

                line.startsWith("/*mapping_adr:*/") -> {
                    val result = line.replace("/*mapping_adr:*/", "")
                        .trim().replace("0x", "").toULongOrNull(16)
                    if (result != null) {
                        mMappedAddress = result
                    }
                }

                line.startsWith("/*length:*/") -> {
                    val result = line.replace("/*length:*/", "")
                        .trim().replace("0x", "").toULongOrNull(16)
                    if (result != null) {
                        mLength = result
                    }
                }

                line.startsWith("DP_BLOB") -> {
                    mBlobType = BlobTypes.DP_BLOB
                    if (data.size >= index + 1) {
                        when (data[index + 1].trim()) {
                            "INTERN" -> {
                                mInternExtern = InternalLocation.INTERN
                                mAddrExtension = ULong.MIN_VALUE
                            }
                            "EXTERN" -> {
                                mInternExtern = InternalLocation.EXTERN
                                mAddrExtension = ULong.MIN_VALUE
                            }
                            else ->  {
                                val result = data[index + 1].trim().replace("0x", "").toULongOrNull(16)
                                if (result != null) {
                                    mAddrExtension = result
                                }
                            }
                        }
                    }

                    if (data.size >= index + 2) {
                        when (data[index + 2].trim()) {
                            "INTERN" -> {
                                mInternExtern = InternalLocation.INTERN
                                mBaseAddress = ULong.MIN_VALUE
                            }
                            "EXTERN" -> {
                                mInternExtern = InternalLocation.EXTERN
                                mBaseAddress = ULong.MIN_VALUE
                            }
                            else -> {
                                val result = data[index + 2].trim().replace("0x", "").toULongOrNull(16)
                                if (result != null) {
                                    mBaseAddress = result
                                }
                            }
                        }
                    }

                    if (data.size >= index + 3) {
                        val result = data[index + 3].trim().replace("0x", "").toULongOrNull(16)
                        if (result != null) {
                            mLength = result
                        }
                    }
                }

                line.startsWith("TP_BLOB") -> {
                    mBlobType = BlobTypes.TP_BLOB
                    if (data.size >= index + 1) {
                        when (data[index + 1].trim()) {
                            "INTERN" -> {
                                mInternExtern = InternalLocation.INTERN
                                mAddrExtension = ULong.MIN_VALUE
                            }
                            "EXTERN" -> {
                                mInternExtern = InternalLocation.EXTERN
                                mAddrExtension = ULong.MIN_VALUE
                            }
                            else ->  {
                                val result = data[index + 1].trim().replace("0x", "").toULongOrNull(16)
                                if (result != null) {
                                    mAddrExtension = result
                                }
                            }
                        }
                    }

                    if (data.size >= index + 2) {
                        when (data[index + 2].trim()) {
                            "INTERN" -> {
                                mInternExtern = InternalLocation.INTERN
                                mBaseAddress = ULong.MIN_VALUE
                            }
                            "EXTERN" -> {
                                mInternExtern = InternalLocation.EXTERN
                                mBaseAddress = ULong.MIN_VALUE
                            }
                            else -> {
                                val result = data[index + 2].trim().replace("0x", "").toULongOrNull(16)
                                if (result != null) {
                                    mBaseAddress = result
                                }
                            }
                        }
                    }

                    if (data.size >= index + 3) {
                        val result = data[index + 3].trim().replace("0x", "").toULongOrNull(16)
                        if (result != null) {
                            mLength = result
                        }
                    }
                }

                line.startsWith("KP_BLOB") -> {
                    mBlobType = BlobTypes.KP_BLOB
                    if (data.size >= index + 1) {
                        when (data[index + 1].trim()) {
                            "INTERN" -> {
                                mInternExtern = InternalLocation.INTERN
                                mAddrExtension = ULong.MIN_VALUE
                            }
                            "EXTERN" -> {
                                mInternExtern = InternalLocation.EXTERN
                                mAddrExtension = ULong.MIN_VALUE
                            }
                            else ->  {
                                val result = data[index + 1].trim().replace("0x", "").toULongOrNull(16)
                                if (result != null) {
                                    mAddrExtension = result
                                }
                            }
                        }
                    }

                    if (data.size >= index + 2) {
                        when (data[index + 2].trim()) {
                            "INTERN" -> {
                                mInternExtern = InternalLocation.INTERN
                                mBaseAddress = ULong.MIN_VALUE
                            }
                            "EXTERN" -> {
                                mInternExtern = InternalLocation.EXTERN
                                mBaseAddress = ULong.MIN_VALUE
                            }
                            else -> {
                                val result = data[index + 2].trim().replace("0x", "").toULongOrNull(16)
                                if (result != null) {
                                    mBaseAddress = result
                                }
                            }
                        }
                    }

                    if (data.size >= index + 3) {
                        val result = data[index + 3].trim().replace("0x", "").toULongOrNull(16)
                        if (result != null) {
                            mLength = result
                        }
                    }
                }

                line.startsWith("ASAP1B_CCP") -> {
                    mType = IfDataTypes.ASAP1B_CCP
                }

                line.startsWith("ASAP1B_KWP2000") -> {
                    mType = IfDataTypes.ASAP1B_KWP2000
                }

                line.startsWith("ETK") -> {
                    mType = IfDataTypes.ETK
                }

                line.startsWith("/begin RASTER") -> {
                    mRaster = parseAndFormatRasters(
                        data.subList(index, data.size).joinToString(" ")
                    )
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return components.parsers.a2l.ifdata.IfData(
        name = data[0],
        type = mType,
        blobType = mBlobType,
        addrExtension = mAddrExtension,
        baseAddress = mBaseAddress,
        mappedAddress = mMappedAddress,
        length = mLength,
        internExtern = mInternExtern,
        raster = mRaster
    )
}