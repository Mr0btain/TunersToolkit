package components.parsers.a2l

import components.parsers.a2l.classes.data.A2lData
import components.parsers.a2l.classes.enums.MemorySegmentProgramType
import components.parsers.a2l.helpers.parseA2l
import components.parsers.a2l.functions.parseFunctionsToMapList
import components.calibrationdata.classes.MapDataValues
import components.calibrationdata.a2l.getCharacteristicData
import components.errorstuff.safeCall
import components.helpers.alignChunksWithMaps
import components.parsers.hex.HexSegmentData
import components.parsers.hex.toAsciiString
import components.maptextoverlay.classes.MapValuesSorted
import screens.editor.components.MainViewModel

fun parseAndComputeA2lData(
    input: ByteArray,
    viewModel: MainViewModel
) {
    // Set the loading to true
    viewModel.isLoading.value = true

    viewModel.appBarTitle.value = "Pulling data from A2L File.."

    val mHexData = viewModel.hexDataSegments
    var mSegmentId = ""
    var mBaseOffset = 0UL
    var mStartOffset = 0UL
    var mEndOffset = 0UL
    val mA2lData = safeCall { input.toAsciiString().split("\n").parseA2l() }

    if (mA2lData.isSuccess) {
        val mCharResult = mutableListOf<MapDataValues>()
        mA2lData.getOrNull()?.let { a2lData ->
            // Update the list of maps for our MapListTab
            viewModel.mapListItems.addAll(parseFunctionsToMapList(a2lData.functions))

            // Format the results
            a2lData.characteristics.forEach { (_, characteristic) ->
                val result = safeCall {
                    getCharacteristicData(
                        mA2lData.getOrDefault(A2lData()),
                        viewModel.hexDataSegments,
                        characteristic
                    )
                }
                if (result.isSuccess) {
                    result.getOrNull()?.let {
                        mCharResult.add(it)
                    }
                }
            }

            a2lData.modPar.memorySegments.forEach { memSeg ->
                if (memSeg.value.prgType == MemorySegmentProgramType.CODE) {
                    mHexData.forEach { (key, data) ->
                        if (data.startOffset == memSeg.value.address) {
                            mSegmentId = key
                            mBaseOffset = data.baseOffset
                            mStartOffset = data.startOffset
                            mEndOffset = data.endOffset
                        }
                    }
                }
            }
        }

        // Align segments with maps
        val mFinalData = safeCall {
            mHexData[mSegmentId]?.let {
                alignChunksWithMaps(
                    hexSegmentData = it,
                    mapDataValues = mCharResult,
                    lineCount = viewModel.digitsPerLine.value,
                    minChunkSize = 4000
                )
            }
        }

        if (mFinalData.isSuccess) {
            mFinalData.getOrNull()?.let {
                viewModel.hexDataSegments[mSegmentId] = HexSegmentData(
                        data = it.first,
                        baseOffset = mBaseOffset,
                        startOffset = mStartOffset,
                        endOffset = mEndOffset
                )

                // Update the Sorted Map List in View Model
                viewModel.mapData.value = MapValuesSorted(
                    segmentId = mSegmentId,
                    mapsSortedByChunk = it.second
                )
            }
        }
    }

    viewModel.isLoading.value = false
    viewModel.appBarTitle.value = "Tuners Toolkit"
}