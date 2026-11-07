package org.mr0btain.tunerstoolkit.components.parsers.a2l.axis
import kotlinx.serialization.Serializable

/**
 * Optional Parameters for A2L AxisPts Data Class
 */
@Serializable
enum class AxisPtsOpt {
    Annotation,
    ByteOrder,
    CalibrationAccess,
    DisplayIdentifier,
    Deposit,
    EcuAddressExtension,
    ExtendedLimits,
    Format,
    FunctionList,
    GuardRails,
    IfData,
    Monotony,
    PhysUnit,
    ReadOnly,
    RefMemorySegment,
    StepSize,
    SymbolLink;
}