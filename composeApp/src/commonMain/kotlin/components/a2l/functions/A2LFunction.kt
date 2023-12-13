package components.a2l.functions

import components.a2l.classes.data.AnnotList
import components.a2l.ifdata.IfData
import components.a2l.classes.data.Annotation
import components.a2l.classes.data.DefCharacs
import components.a2l.classes.data.FunctionVersion
import components.a2l.classes.data.IfDataList
import components.a2l.classes.data.InMeasurement
import components.a2l.classes.data.LocMeasurement
import components.a2l.classes.data.OutMeasurements
import components.a2l.classes.data.RefCharacs
import components.a2l.classes.data.SubFunction
import components.a2l.objects.A2lOptional
import components.helpers.checkItemsAre
import kotlinx.serialization.Contextual
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
) {
    fun isNotEmpty(): Boolean {
        return when {
            name.isBlank() -> false
            optionalParameters.isEmpty() -> false
            else -> true
        }
    }

    /**
     * Checks and Returns a list of Annotations if set
     */
    fun getAnnotations(): List<Annotation> {
        return if (this.optionalParameters.containsKey(FunctionOpt.Annotation)) {
            val result = this.optionalParameters[FunctionOpt.Annotation] as? AnnotList ?: AnnotList()
            result.annot
        } else listOf()
    }

    /**
     * Checks and Returns a List of Def Characteristics
     */
    fun getDefCharacteristics(): List<String> {
        return if (this.optionalParameters.containsKey(FunctionOpt.DefCharacteristics)) {
            val result = this.optionalParameters[FunctionOpt.DefCharacteristics] as? DefCharacs ?: DefCharacs()
            result.list
        } else listOf()
    }

    /**
     * Checks and Returns the function version
     */
    fun getFunctionVersion(): String {
        return if (this.optionalParameters.containsKey(FunctionOpt.FunctionVersion)) {
            val res = this.optionalParameters[FunctionOpt.FunctionVersion] as? FunctionVersion ?: FunctionVersion()
            res.version
        } else ""
    }

    /**
     * Checks and Returns any IfData if set
     */
    fun getIfData(): List<IfData> {
        return if (this.optionalParameters.containsKey(FunctionOpt.IfData)) {
            val result = this.optionalParameters[FunctionOpt.IfData] as? IfDataList ?: IfDataList()
            // Check if items are correct type
            result.ifData
        } else listOf()
    }

    /**
     * Checks and Returns a list of In Measurements
     */
    fun getInMeasurements(): List<String> {
        return if (this.optionalParameters.containsKey(FunctionOpt.InMeasurements)) {
            val result = this.optionalParameters[FunctionOpt.InMeasurements] as? InMeasurement ?: InMeasurement()
            result.list
        } else listOf()
    }

    /**
     * Checks and Returns a list of Loc Measurements
     */
    fun getLocMeasurement(): List<String> {
        return if (this.optionalParameters.containsKey(FunctionOpt.LocMeasurement)) {
            val result = this.optionalParameters[FunctionOpt.LocMeasurement] as? LocMeasurement ?: LocMeasurement()
            result.list
        } else listOf()
    }

    /**
     * Checks and Returns a list of Out Measurements
     */
    fun getOutMeasurements(): List<String> {
        return if (this.optionalParameters.containsKey(FunctionOpt.OutMeasurements)) {
            val result = this.optionalParameters[FunctionOpt.OutMeasurements] as? OutMeasurements ?: OutMeasurements()
            result.list
        } else listOf()
    }

    /**
     * Checks and Returns a list of Ref Characteristics
     */
    fun getRefCharacteristics(): List<String> {
        return if (this.optionalParameters.containsKey(FunctionOpt.RefCharacteristics)) {
            val result = this.optionalParameters[FunctionOpt.RefCharacteristics] as? RefCharacs ?: RefCharacs()
            result.refs
        } else listOf()
    }

    /**
     * Checks and Returns a list of Sub Functions
     */
    fun getSubFunction(): List<String> {
        return if (this.optionalParameters.containsKey(FunctionOpt.SubFunction)) {
            val result = this.optionalParameters[FunctionOpt.SubFunction] as? SubFunction ?: SubFunction()
            result.list
        } else listOf()
    }
}