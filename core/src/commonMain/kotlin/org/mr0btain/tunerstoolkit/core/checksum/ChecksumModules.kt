package org.mr0btain.tunerstoolkit.core.checksum

import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.image.MemoryImage

/** A module that recognised an image, and how sure it is */
class DetectedModule(val module: ChecksumModule, val detection: Detection)

/**
 * The checksum modules that come with the app. Each is a .ttsum text here, so built in modules use the same format
 * as the ones people add; a family whose blocks have to be found by reading the image can be a Kotlin
 * [ChecksumModule] added to [codeModules] instead. docs/checksums.md says how to add one, and lists the families
 * researched so far with what's known about them
 */
object ChecksumModules {
    /** TT-DEMO-1's checksums: one of each kind, so every path is exercised on a file anyone can have */
    const val DEMO = """
{
  "format": "ttsum", "formatVersion": 1,
  "id": "tt-demo-1", "name": "TT-DEMO-1", "family": "Tuners Toolkit demo ECU",
  "description": "The made up demo ECU: a CRC over the calibration, a word sum with its complement over the boot block, and a forced sum over the code the way Bosch MED17 blocks work",
  "status": "VERIFIED", "source": "core's DemoEcu writes these checksums",
  "detect": [ { "text": "TTDEMO1", "address": "0x800100" } ],
  "checks": [
    {
      "name": "Calibration CRC",
      "description": "CRC-32 of the calibration area, kept in its last four bytes",
      "algorithm": { "crc": "CRC-32/ISO-HDLC" },
      "ranges": [ "0x804000-0x807FFB" ],
      "store": { "address": "0x807FFC", "size": 4, "byteOrder": "LITTLE_ENDIAN", "form": "VALUE" }
    },
    {
      "name": "Boot sum",
      "description": "16-bit sum of the boot block's words, then its complement",
      "algorithm": { "sum": { "word": 2, "width": 16, "byteOrder": "LITTLE_ENDIAN" } },
      "ranges": [ "0x0-0xFB" ],
      "store": { "address": "0xFC", "size": 2, "byteOrder": "LITTLE_ENDIAN", "form": "VALUE_AND_COMPLEMENT" }
    },
    {
      "name": "Code block",
      "description": "32-bit sum of the code's words from 0xFADECAFE, made to come to 0xCAFEAFFE by the block's last word",
      "algorithm": { "sum": { "word": 4, "width": 32, "byteOrder": "LITTLE_ENDIAN", "init": "0xFADECAFE" } },
      "ranges": [ "0x800000-0x803FFF" ],
      "store": { "address": "0x803FFC", "size": 4, "byteOrder": "LITTLE_ENDIAN", "form": "FORCE", "target": "0xCAFEAFFE" }
    }
  ]
}
"""

    /** Families written in .ttsum, one text each */
    private val definedModules: List<String> = listOf(DEMO)

    /** Families that need code to find their blocks; none yet */
    private val codeModules: List<ChecksumModule> = emptyList()

    /** Every built in module */
    val builtIn: List<ChecksumModule> by lazy {
        definedModules.mapNotNull { text -> (TtSum.read(text.encodeToByteArray(), "built in") as? TkResult.Success)?.data?.value } + codeModules
    }

    val demo: DefinedModule get() = builtIn.first { it.id == "tt-demo-1" } as DefinedModule

    /** The modules among [modules] that recognise [image], surest first */
    fun detect(image: MemoryImage, modules: List<ChecksumModule> = builtIn): List<DetectedModule> =
        modules.mapNotNull { module -> module.detect(image)?.let { DetectedModule(module, it) } }.sortedByDescending { it.detection.confidence }
}
