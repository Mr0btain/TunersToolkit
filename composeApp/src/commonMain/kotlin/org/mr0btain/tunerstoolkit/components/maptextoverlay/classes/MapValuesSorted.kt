package org.mr0btain.tunerstoolkit.components.maptextoverlay.classes
import org.mr0btain.tunerstoolkit.components.calibrationdata.classes.MapDataValues
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Encoder

@Serializable
data class MapValuesSorted(
    val segmentId: String = "",
    val mapsSortedByChunk: Map<Int, List<MapDataValues>> = mapOf(),
    )
