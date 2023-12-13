package components.parsers.a2l.functions

import kotlinx.serialization.Serializable

/**
 * Optional Parameters for A2L Function Data Class
 */
@Serializable
enum class FunctionOpt {
    Annotation,
    DefCharacteristics,
    FunctionVersion,
    IfData,
    InMeasurements,
    LocMeasurement,
    OutMeasurements,
    RefCharacteristics,
    SubFunction
}