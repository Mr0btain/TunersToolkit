package components.parsers.a2l

import MainViewModel
import components.calibrationdata.a2l.getCharacteristicData
import components.calibrationdata.classes.MapDataValues
import components.errorstuff.safeCall
import components.helpers.alignChunksWithMaps
import components.maptextoverlay.classes.MapValuesSorted
import components.parsers.a2l.classes.enums.MemorySegmentProgramType
import components.parsers.a2l.functions.parseFunctionsToMapList
import components.parsers.a2l.helpers.parseA2l
import components.parsers.hex.HexSegmentData
import components.parsers.hex.toAsciiString

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

    safeCall {
        val (a2lData, errors) = input.toAsciiString().split("\n").parseA2l()

        val charResult = mutableListOf<MapDataValues>()

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

        if (errors.isNotEmpty()) {
            viewModel.logger.addAll(errors)
            println("Errors occurred parsing A2l!")
            errors.forEach { println(it) }
        }
    }

    viewModel.isLoading.value = false
}