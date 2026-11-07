package org.mr0btain.tunerstoolkit.components.parsers.a2l.functions
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.AnnotList
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.Annotation
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.DefCharacs
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.FunctionVersion
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.IfDataList
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.InMeasurement
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.LocMeasurement
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.OutMeasurements
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.RefCharacs
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.SubFunction
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional
import kotlinx.serialization.Serializable

/**
 * For the structuring of projects involving a very large number of adjustable objects and
 * measuring channels, functions can be defined. These functions shall be used in the
 * measurement and calibration system to allow the selection lists for the selection of
 * adjustable objects and measuring channels to be represented in a structured,
 * hierarchical manner following the order of input on the basis of functional viewpoints
 * (function orientation).
 * Note: Since ASAP2 version 1.2.0 the references between functions and
 * measurement objects resp. adjustable objects can be described either with
 * keyword CHARACTERISTIC, AXIS_PTS and MEASUREMENT (see
 * FUNCTION_LIST) or with keyword FUNCTION.
 *
 * @param name unique Identifier in the program, referencing is based
 * on this 'name'
 * @param longIdentifier Description of the item
 */

@Serializable
data class A2LFunction(
    val name: String = "",
    val longIdentifier: String = "",
    val optionalParameters: Map<FunctionOpt, A2lOptional> = mapOf()
)

fun A2LFunction.validate(): Boolean {
    return when {
        name.isBlank() -> false
        optionalParameters.isEmpty() -> false
        else -> true
    }
}

/**
 * Checks and Returns a list of Annotations if set
 */
fun A2LFunction.getAnnotations(): List<Annotation> {
    return if (optionalParameters.containsKey(FunctionOpt.Annotation)) {
        val result = optionalParameters[FunctionOpt.Annotation] as? AnnotList ?: AnnotList()
        result.annot
    } else listOf()
}

/**
 * Checks and Returns a List of Def Characteristics
 */
fun A2LFunction.getDefCharacteristics(): List<String> {
    return if (optionalParameters.containsKey(FunctionOpt.DefCharacteristics)) {
        val result = optionalParameters[FunctionOpt.DefCharacteristics] as? DefCharacs ?: DefCharacs()
        result.list
    } else listOf()
}

/**
 * Checks and Returns the function version
 */
fun A2LFunction.getFunctionVersion(): String {
    return if (optionalParameters.containsKey(FunctionOpt.FunctionVersion)) {
        val res = optionalParameters[FunctionOpt.FunctionVersion] as? FunctionVersion ?: FunctionVersion()
        res.version
    } else ""
}

/**
 * Checks and Returns any IfData if set
 */
fun A2LFunction.getIfData(): List<org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.IfData> {
    return if (optionalParameters.containsKey(FunctionOpt.IfData)) {
        val result = optionalParameters[FunctionOpt.IfData] as? IfDataList ?: IfDataList()
        // Check if items are correct type
        result.ifData
    } else listOf()
}

/**
 * Checks and Returns a list of In Measurements
 */
fun A2LFunction.getInMeasurements(): List<String> {
    return if (optionalParameters.containsKey(FunctionOpt.InMeasurements)) {
        val result = optionalParameters[FunctionOpt.InMeasurements] as? InMeasurement ?: InMeasurement()
        result.list
    } else listOf()
}

/**
 * Checks and Returns a list of Loc Measurements
 */
fun A2LFunction.getLocMeasurement(): List<String> {
    return if (optionalParameters.containsKey(FunctionOpt.LocMeasurement)) {
        val result = optionalParameters[FunctionOpt.LocMeasurement] as? LocMeasurement ?: LocMeasurement()
        result.list
    } else listOf()
}

/**
 * Checks and Returns a list of Out Measurements
 */
fun A2LFunction.getOutMeasurements(): List<String> {
    return if (optionalParameters.containsKey(FunctionOpt.OutMeasurements)) {
        val result = optionalParameters[FunctionOpt.OutMeasurements] as? OutMeasurements ?: OutMeasurements()
        result.list
    } else listOf()
}

/**
 * Checks and Returns a list of Ref Characteristics
 */
fun A2LFunction.getRefCharacteristics(): List<String> {
    return if (optionalParameters.containsKey(FunctionOpt.RefCharacteristics)) {
        val result = optionalParameters[FunctionOpt.RefCharacteristics] as? RefCharacs ?: RefCharacs()
        result.refs
    } else listOf()
}

/**
 * Checks and Returns a list of Sub Functions
 */
fun A2LFunction.getSubFunction(): List<String> {
    return if (optionalParameters.containsKey(FunctionOpt.SubFunction)) {
        val result = optionalParameters[FunctionOpt.SubFunction] as? SubFunction ?: SubFunction()
        result.list
    } else listOf()
}