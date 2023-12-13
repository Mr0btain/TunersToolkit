package components.parsers.a2l.group

import kotlinx.serialization.Serializable

/**
 * Optional Parameters for A2L Group Data Class
 */
@Serializable
enum class GroupOpt {
    Annotation,
    FunctionList,
    IfData,
    RefCharacteristics,
    RefMeasurements,
    Root,
    SubGroup
}