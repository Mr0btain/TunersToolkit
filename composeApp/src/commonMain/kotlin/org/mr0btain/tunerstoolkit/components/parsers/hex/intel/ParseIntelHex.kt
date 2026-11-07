@file:Suppress("LocalVariableName")

package org.mr0btain.tunerstoolkit.components.parsers.hex.intel
import org.mr0btain.tunerstoolkit.components.parsers.hex.HexSegmentData
import org.mr0btain.tunerstoolkit.components.parsers.hex.chunkHexData

/**
 * Parses a list of Ascii data and gets the hex data, offsets, and any other data
 */
fun List<String>.parseIntelHex(): Map<String, HexSegmentData> {
    var currentGlobalOffset = ULong.MIN_VALUE
    var currentAddress = ULong.MIN_VALUE
    var duplicateIdNumber = 0
    data class IntelHexSegment(val byteCount: ULong, val globalOffset: ULong, val address: ULong, val data: String)
    val result = mutableMapOf<String, HexSegmentData>()
    val segmentList = arrayListOf<IntelHexSegment>()

    // Process each line
    this.forEach { line ->
        // Check line length if invalid return
        if (line.length < 9) return@forEach

        // Sort using record type
        when (line.parseIntelHexRecordType()) {
            // Data record type
            IntelHexRecordType.RECORD_TYPE_DATA -> {
                val byteCount = readByteCount(line)
                val address = readAddress(line) + currentGlobalOffset
                val data = readData(line)
                val checksum = readChecksum(line)
                val calcChecksum = calculateChecksum(line)

                // TODO: Add some error handling here
                if (checksum != calcChecksum) println("Checksum Error!")

                segmentList.add(
                    IntelHexSegment(
                        byteCount = byteCount,
                        globalOffset = currentGlobalOffset,
                        address = address,
                        data = data
                    )
                )
            }

            // End of File
            IntelHexRecordType.RECORD_TYPE_END_OF_FILE -> {
                val sorted = segmentList.sortedWith(compareBy { it.address })
                var startingOffset = ULong.MIN_VALUE
                var newSegment = true
                val data = StringBuilder()
                var base = ULong.MIN_VALUE

                // Sort sections by address
                sorted.forEachIndexed { index, item ->
                    when {
                        // End of file check
                        (index == sorted.size - 1) -> {
                            val mId = startingOffset.toString(16)  // Integer.toHexString(startingOffset.toInt()) ?: "0"
                            // Check if map contains this ID already, if so append the duplicate number behind it
                            if (result.containsKey(mId)) {
                                // Add with appended duplicate number
                                result["$mId:$duplicateIdNumber"] = HexSegmentData(
                                    data = data.toString().chunkHexData(),
                                    startOffset = startingOffset,
                                    endOffset = currentAddress,
                                    baseOffset = base,
                                    totalLength = data.length
                                )

                                // Advance the duplicate number for the next one
                                duplicateIdNumber += 1
                            } else {
                                // If its ID is not already in the list, just add the segment
                                result[mId] = HexSegmentData(
                                    data = data.toString().chunkHexData(),
                                    startOffset = startingOffset,
                                    endOffset = currentAddress,
                                    baseOffset = base,
                                    totalLength = data.length
                                )
                            }
                        }

                        // Address matches, either the start of a file at 0000 or next item in segment
                        item.address == currentAddress -> {
                            if (newSegment) {
                                // Append data to the segment
                                data.append(item.data)
                                // Set beginning offset
                                startingOffset = if (item.address < item.byteCount) { item.address } else {
                                    item.address - item.byteCount
                                }
                                // Advance address for check on next item
                                currentAddress = item.address + (item.byteCount)
                                // Set new segment counter to false
                                newSegment = false
                                // Set the base offset
                                base = item.globalOffset
                            } else {
                                // Add the data to the segment
                                data.append(item.data)
                                // Advance the address by this items Byte count
                                currentAddress += (item.byteCount)
                            }
                        }

                        // End of last segment, start of next one
                        else -> {
                            val mId = startingOffset.toString(16)
                            // Check if map contains this ID already, if so append the duplicate number behind it
                            if (result.containsKey(mId)) {
                                // Add with appended duplicate number
                                result["$mId:$duplicateIdNumber"] = HexSegmentData(
                                    data = data.toString().chunkHexData(),
                                    startOffset = startingOffset,
                                    endOffset = currentAddress,
                                    baseOffset = item.globalOffset,
                                    totalLength = data.length
                                )

                                // Advance the duplicate number for the next one
                                duplicateIdNumber += 1
                            } else {
                                // If its ID is not already in the list, just add the segment
                                result[mId] = HexSegmentData(
                                    data = data.toString().chunkHexData(),
                                    startOffset = startingOffset,
                                    endOffset = currentAddress,
                                    baseOffset = item.globalOffset,
                                    totalLength = data.length
                                )
                            }

                            // Clear StringBuilder and start a new segment
                            data.clear()
                            data.append(item.data)
                            startingOffset = if (item.address < item.byteCount) { item.address } else {
                                item.address - item.byteCount
                            }
                            currentAddress = item.address + (item.byteCount)
                            newSegment = true
                        }
                    }
                }
            }

            IntelHexRecordType.RECORD_TYPE_EXTENDED_SEGMENT_ADDRESS -> {
                val dat0 = line.substring(9, line.length - 4)
                val dat1 = line.substring(11, line.length - 2)
                currentGlobalOffset = ((((dat0.toULong(16) and 255u) shl 8) or (dat1.toULong(16) and 255u)) shl 4)
            }

            IntelHexRecordType.RECORD_TYPE_START_SEGMENT_ADDRESS -> {
                println("Start Segment Address")
                println(line)
            }

            IntelHexRecordType.RECORD_TYPE_EXTENDED_LINEAR_ADDRESS -> {
                val dat0 = line.substring(9, line.length - 4)
                val dat1 = line.substring(11, line.length - 2)
                currentGlobalOffset = ((((dat0.toULong(16) and 255u) shl 8) or (dat1.toULong(16) and 255u)) shl 16)
            }

            IntelHexRecordType.RECORD_TYPE_START_LINEAR_ADDRESS -> {
                println("Start Linear Address")
                println(line)
            }

            IntelHexRecordType.NOT_FOUND -> { }
        }
    }
    return result
}

private fun readByteCount(line: String): ULong {
    return line.substring(1,3).toULong(radix = 16)
}

private fun readData(line: String): String {
    return line.substring(9, line.length - 2)
}

private fun hasComma(x: Any) = when(x) {
    is String -> x.startsWith(":")
    else -> false
}

private fun readChecksum(line: String): Int {
    return line.substring(line.length-2, line.length).toInt(radix = 16)
}

private fun calculateChecksum(line: String): Int {
    val len = line.length-3
    var checksum = 0
    for (i in 1..len step 2) {
        val num = line.substring(i, i+2).toInt(radix = 16)
        checksum += num
    }
    checksum = ((0xFF-(checksum and 0xFF))+1) and 0xFF
    return checksum
}

private fun readAddress(line: String): ULong {
    return line.substring(3,7).toULong(radix = 16)
}