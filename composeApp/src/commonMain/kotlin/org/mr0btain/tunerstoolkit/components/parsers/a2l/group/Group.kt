package org.mr0btain.tunerstoolkit.components.parsers.a2l.group
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.RefCharacs
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

/**
 * These GROUP shall be used in the measurement and calibration system to provide
 * selection lists (groups) of adjustable objects and measuring channels.
 * For the structuring of projects involving a very large number of adjustable objects and
 * measuring channels, an unlimited number of grouping mechanisms, each constituted
 * from a root group containing subgroups (including nested references), can be defined.
 * Such root groups are used in the MCD system for initial display of the available groups,
 * as the root of a tree containing the referenced subgroups. Use cases are e.g. software
 * components which define the C file assignment, calibration components which describe
 * the calibration engineers viewpoint, editor selection lists which can define the
 * presentation of calibration objects and their related measurement quantities.
 *
 * @param name Unique identifier of the item
 * @param longIdentifier Description of the item
 */

@Serializable
data class Group(
    val name: String = "",
    val longIdentifier: String = "",
    val optionalParameters: Map<GroupOpt, A2lOptional> = mapOf()
)

fun Group.validate(): Boolean {
    return when {
        name.isBlank() -> false
        optionalParameters.isEmpty() -> false
        else -> true
    }
}

fun Group.getRefCharacteristics(): RefCharacs {
    return if (optionalParameters.containsKey(GroupOpt.RefCharacteristics)) {
        optionalParameters[GroupOpt.RefCharacteristics] as? RefCharacs ?: RefCharacs()
    } else RefCharacs()
}