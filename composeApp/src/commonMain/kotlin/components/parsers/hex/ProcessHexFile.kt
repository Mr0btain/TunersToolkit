package components.parsers.hex

import components.errorstuff.safeCall
import components.parsers.hex.intel.checkIfIntelHex
import components.parsers.hex.intel.parseIntelHex
import MainViewModel

fun processHexFile(
    input: ByteArray,
    viewModel: MainViewModel
) {
    viewModel.isLoading.value = true

    // Only convert a small section to check if its Intel Hex, if so convert whole thing else
    // don't waste computations
    val mAsciiData = safeCall {
        input.copyOfRange(0, 64).toAsciiString().split(":".toRegex()).map { ":${it.trim()}" }
    }
    var mIntelCheck = false

    // Check if intel hex
    if (mAsciiData.isSuccess) {
        mAsciiData.getOrNull()?.let {
            mIntelCheck = checkIfIntelHex(it[1])
        }
    }

    // TODO: Add motorola hex s19
    when {
        // Run Intel Hex check
        (mIntelCheck) -> {
            // Read all lines of the File
            val mResult = safeCall {
                input.toAsciiString()
                    .split(":".toRegex())
                    .map { ":${it.trim()}" }
                    .parseIntelHex()
            }

            if (mResult.isSuccess) {
                mResult.getOrNull()?.let { result ->
                    // Loop through segments
                    result.forEach { segment ->
                        // Check if segment starts at an offset indicating it's the data segment
                        if (segment.value.startOffset >= 8000000u) {
                            // Update selected item in drop down
                            viewModel.selectedHexSegment.value = segment.key
                        }
                    }

                    // Update the stored Value in the View Model
                    viewModel.hexDataSegments.clear()
                    result.forEach { viewModel.hexDataSegments[it.key] = it.value }

                    // Set text with first entry if one does not fall in a normal offset
                    if (viewModel.selectedHexSegment.value.isBlank()) {
                        // Update selected item in drop down
                        viewModel.selectedHexSegment.value = result.keys.first()
                    }
                }
            }
        }

        // If not known type that needs processed then set the data as normal hexadecimal
        else -> {
            val mResult = safeCall { input.toHexMap() }

            if (mResult.isSuccess) {
                mResult.getOrNull()?.let { result ->
                    // Update the View Model data
                    viewModel.hexDataSegments.clear()
                    viewModel.hexDataSegments["0"] = HexSegmentData(
                        baseOffset = 0u,
                        data = result,
                        startOffset = 0u,
                        endOffset = input.size.toULong(),
                        totalLength = input.size * 2
                    )
                    viewModel.selectedHexSegment.value = "0"
                }
            }
        }
    }
    viewModel.isLoading.value = false
}