package components.parsers.a2l.ifdata.components

import components.parsers.a2l.objects.A2lBlob

data class DpBlob(
    val startAddress: ULong? = null,
    val baseAddress: ULong? = null,
    val size: ULong? = null
): A2lBlob()