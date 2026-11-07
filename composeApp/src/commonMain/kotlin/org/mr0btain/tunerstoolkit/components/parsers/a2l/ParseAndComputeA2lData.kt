package org.mr0btain.tunerstoolkit.components.parsers.a2l
import org.mr0btain.tunerstoolkit.MainViewModel
import org.mr0btain.tunerstoolkit.components.calibrationdata.a2l.getCharacteristicData
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.MapDataValues
import org.mr0btain.tunerstoolkit.components.errorstuff.safeCall
import org.mr0btain.tunerstoolkit.components.helpers.alignChunksWithMaps
import org.mr0btain.tunerstoolkit.components.maptextoverlay.classes.MapValuesSorted
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.enums.MemorySegmentProgramType
import org.mr0btain.tunerstoolkit.components.parsers.a2l.functions.parseFunctionsToMapList
import org.mr0btain.tunerstoolkit.components.parsers.a2l.helpers.parseA2l
import org.mr0btain.tunerstoolkit.components.parsers.hex.HexSegmentData
import org.mr0btain.tunerstoolkit.components.parsers.hex.toAsciiString
import kotlin.time.measureTimedValue

fun parseAndComputeA2lData(
    input: ByteArray,
    viewModel: MainViewModel
) {
    // Set the loading to true
    viewModel.isLoading.value = true

    val hexData = viewModel.hexDataSegments
    var segmentId = ""
    var baseOffset = 0UL
    var startOffset = 0UL
    var endOffset = 0UL

    val data = input.toAsciiString().split("\n")
    val (a2lData, logData) = parseA2l(data)

    // Timed test
    testParseTime(data)

    val charResult = mutableListOf<MapDataValues>()

    if (logData.isEmpty()) {
        viewModel.logger.addMessage(
            message = "A2l file parsed succesfully!"
        )
    } else {
        println("Errors while parsing A2l!")
        logData.forEach {
            println(it)
        }
    }

    // Update the list of maps for our MapListTab
    viewModel.mapListItems.addAll(parseFunctionsToMapList(a2lData.functions))

    // Format the results
    a2lData.characteristics.forEach { (_, characteristic) ->
        val result = safeCall {
            getCharacteristicData(
                a2lData,
                viewModel.hexDataSegments,
                characteristic
            )
        }
        if (result.isSuccess) {
            result.getOrNull()?.let {
                charResult.add(it)
            }
        }
    }

    a2lData.modPar.memorySegments.forEach { memSeg ->
        if (memSeg.value.prgType == MemorySegmentProgramType.CODE) {
            hexData.forEach { (key, data) ->
                if (data.startOffset == memSeg.value.address) {
                    segmentId = key
                    baseOffset = data.baseOffset
                    startOffset = data.startOffset
                    endOffset = data.endOffset
                }
            }
        }
    }

    // Align segments with maps
    val finalData = safeCall {
        hexData[segmentId]?.let {
            alignChunksWithMaps(
                hexSegmentData = it,
                mapDataValues = charResult,
                lineCount = viewModel.digitsPerLine.value,
                minChunkSize = 4000
            )
        }
    }

    if (finalData.isSuccess) {
        finalData.getOrNull()?.let {
            viewModel.hexDataSegments[segmentId] = HexSegmentData(
                data = it.first,
                baseOffset = baseOffset,
                startOffset = startOffset,
                endOffset = endOffset
            )

            // Update the Sorted Map List in View Model
            viewModel.mapData.value = MapValuesSorted(
                segmentId = segmentId,
                mapsSortedByChunk = it.second
            )
        }
    }

    viewModel.logger.addAll(logData)
    viewModel.isLoading.value = false
}

private fun testParseTime(input: List<String>) {
    val original = measureTimedValue {
        parseA2l(input)
    }

    // Original
    println("Time to parse: ${original.duration}")
    println("Characteristic size: ${original.value.first.characteristics.size}")
    println("Errors: ${original.value.second.size}")
}