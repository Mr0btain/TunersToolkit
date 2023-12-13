package components.a2l.characteristic

import kotlinx.serialization.Serializable

/**
 * Optional Parameters for A2L Characteristic Data Class
 */
@Serializable
enum class CharOpt {
    Annotations,
    AXIS_DESC_X,
    AXIS_DESC_Y,
    AXIS_DESC_Z,
    AXIS_DESC_4,
    AXIS_DESC_5,
    BitMask,
    ByteOrder,
    CalibrationAccess,
    ComparisonQuantity,
    ConversionType,
    DependantCharacteristic,
    Deposit,
    Discrete,
    DisplayIdentifier,
    EcuAddressExtension,
    ExtendedLimits,
    Format,
    FunctionList,
    GuardRails,
    IfData,
    MapList,
    MatrixDim,
    MaxRefresh,
    Number,
    PhysUnit,
    ReadOnly,
    RefMemorySegment,
    StepSize,
    SymbolLink,
    VirtualCharacteristic
}

fun Int.getCharAxisDescType(): CharOpt {
    return when (this) {
        0 -> CharOpt.AXIS_DESC_X
        1 -> CharOpt.AXIS_DESC_Y
        2 -> CharOpt.AXIS_DESC_Z
        3 -> CharOpt.AXIS_DESC_4
        4 -> CharOpt.AXIS_DESC_5
        else -> CharOpt.AXIS_DESC_X
    }
}