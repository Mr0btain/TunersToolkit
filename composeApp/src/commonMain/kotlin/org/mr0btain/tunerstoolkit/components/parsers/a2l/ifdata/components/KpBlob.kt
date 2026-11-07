package org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.components
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lBlob

data class KpBlob(
    val startAddress: ULong,
    val addressLocation: String,
    val size: UInt,
    val raster: Raster? = null
): A2lBlob()