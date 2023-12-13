package components.parsers.a2l.axis

import kotlinx.serialization.Serializable

/**
 * Optional Parameters for A2L AxisDescr Data Class
 */
@Serializable
enum class AxisDescrOpt {
    Format,
    ReferenceAxisPoint,
    ReadOnly,
    MaxGradient,
    Monotony,
    ByteOrder,
    ExtendedLimits,
    ReferenceCurveAxis,
    Deposit,
    PhysUnit,
    StepSize,
    Annotations,
    FixAxisPar,
    FixAxisParDist,
    FixAxisParList;
}