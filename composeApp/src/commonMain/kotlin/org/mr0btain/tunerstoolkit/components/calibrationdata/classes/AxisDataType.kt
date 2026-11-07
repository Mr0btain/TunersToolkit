package org.mr0btain.tunerstoolkit.components.calibrationdata.classes
import kotlinx.serialization.Serializable

@Serializable
enum class AxisDataType {
    W,
    X,
    Y,
    Z,
    Axis4,
    Axis5,
    NOT_SET
}
