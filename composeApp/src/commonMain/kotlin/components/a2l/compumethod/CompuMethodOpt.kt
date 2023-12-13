package components.a2l.compumethod

import kotlinx.serialization.Serializable

/**
 * Optional Parameters for A2L CompuMethod Data Class
 */
@Serializable
enum class CompuMethodOpt {
    CompuTabRef,
    Coeffs,
    CoeffsLinear,
    Formula,
    RefUnit,
    StatusStringRef,
    CompuVTabRef,
    CompuVTabRange;
}