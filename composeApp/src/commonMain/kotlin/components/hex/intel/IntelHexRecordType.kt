package components.hex.intel

enum class IntelHexRecordType(id: Int) {
    RECORD_TYPE_DATA(0),
    RECORD_TYPE_END_OF_FILE(1),
    RECORD_TYPE_EXTENDED_SEGMENT_ADDRESS(2),
    RECORD_TYPE_START_SEGMENT_ADDRESS(3),
    RECORD_TYPE_EXTENDED_LINEAR_ADDRESS(4),
    RECORD_TYPE_START_LINEAR_ADDRESS(5),
    NOT_FOUND(100);
}

fun String.parseIntelHexRecordType(): IntelHexRecordType {
    val id =  this.substring(7,9).toInt(radix = 16)
    return when (id) {
        IntelHexRecordType.RECORD_TYPE_DATA.ordinal -> IntelHexRecordType.RECORD_TYPE_DATA
        IntelHexRecordType.RECORD_TYPE_END_OF_FILE.ordinal -> IntelHexRecordType.RECORD_TYPE_END_OF_FILE
        IntelHexRecordType.RECORD_TYPE_START_LINEAR_ADDRESS.ordinal -> IntelHexRecordType.RECORD_TYPE_START_LINEAR_ADDRESS
        IntelHexRecordType.RECORD_TYPE_EXTENDED_LINEAR_ADDRESS.ordinal -> IntelHexRecordType.RECORD_TYPE_EXTENDED_LINEAR_ADDRESS
        IntelHexRecordType.RECORD_TYPE_START_SEGMENT_ADDRESS.ordinal -> IntelHexRecordType.RECORD_TYPE_START_SEGMENT_ADDRESS
        IntelHexRecordType.RECORD_TYPE_EXTENDED_SEGMENT_ADDRESS.ordinal -> IntelHexRecordType.RECORD_TYPE_EXTENDED_SEGMENT_ADDRESS
        else -> IntelHexRecordType.NOT_FOUND
    }
}