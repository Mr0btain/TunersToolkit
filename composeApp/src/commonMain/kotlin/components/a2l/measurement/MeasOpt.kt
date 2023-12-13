package components.a2l.measurement

import kotlinx.serialization.Serializable

/**
 * Optional Parameters for A2L Measurement Data Class
 */
@Serializable
enum class MeasOpt {
    Annotation,
    ArraySize,
    BitMask,
    BitOperations,
    ByteOrder,
    Discrete,
    DisplayIdentifier,
    EcuAddress,
    EcuAddressExtension,
    ErrorMask,
    Format,
    FunctionList,
    IfData,
    Layout,
    MatrixDim,
    MaxRefresh,
    PhysUnit,
    ReadWrite,
    RefMemorySegment,
    SymbolLink,
    Virtual;
}