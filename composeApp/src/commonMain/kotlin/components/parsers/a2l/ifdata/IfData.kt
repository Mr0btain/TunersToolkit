package components.parsers.a2l.ifdata

import components.parsers.a2l.ifdata.components.Raster
import components.parsers.a2l.ifdata.components.enums.BlobTypes
import components.parsers.a2l.ifdata.components.enums.IfDataTypes
import components.parsers.a2l.ifdata.components.enums.InternalLocation
import kotlinx.serialization.Serializable

@Serializable
data class IfData(
    val name: String = "",
    val type: IfDataTypes = IfDataTypes.NOT_SET,
    val blobType: BlobTypes,
    val addrExtension: ULong = ULong.MIN_VALUE,
    val baseAddress: ULong = ULong.MIN_VALUE,
    val mappedAddress: ULong = ULong.MIN_VALUE,
    val length: ULong = ULong.MIN_VALUE,
    val internExtern: InternalLocation = InternalLocation.NOT_SET,
    val raster: Raster = Raster()
)

fun components.parsers.a2l.ifdata.IfData.isNotEmpty(): Boolean {
    return when {
        name.isBlank() -> false
        type == IfDataTypes.NOT_SET -> false
        else -> true
    }
}