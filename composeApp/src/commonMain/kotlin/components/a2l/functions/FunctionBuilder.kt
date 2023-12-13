package components.a2l.functions

import components.a2l.classes.data.AnnotList
import components.a2l.ifdata.IfData
import components.a2l.ifdata.components.parseIfDataSection
import components.a2l.classes.data.Annotation
import components.a2l.classes.data.DefCharacs
import components.a2l.classes.data.FunctionVersion
import components.a2l.classes.data.IfDataList
import components.a2l.classes.data.InMeasurement
import components.a2l.classes.data.LocMeasurement
import components.a2l.classes.data.OutMeasurements
import components.a2l.classes.data.RefCharacs
import components.a2l.classes.data.SubFunction
import components.a2l.classes.data.parseAnnotation
import components.a2l.objects.A2lOptional

/**
 * Parses and builds a Function based on the parameters it contains if they exist
 */

class FunctionBuilder {
    fun build(input: List<String>): A2LFunction {
        return buildObject(input)
    }

    private fun buildObject(input: List<String>): A2LFunction {
        // Check if size it big enough else we get index out of bounds
        if (input.size >= 2) {
            val mAnnotation = mutableListOf<Annotation>()
            val mIfData = mutableListOf<IfData>()
            var mName = ""
            val mOptionalParams = mutableMapOf<FunctionOpt, A2lOptional>()

            // Wrap it in a try to give it a little crash protection
            try {
                input.forEachIndexed { index, line ->
                    when {
                        line.startsWith("/begin DEF_CHARACTERISTIC") -> {
                            val linesFound = mutableListOf<String>()

                            for (l in input.subList(index + 1, input.size)) {
                                when {
                                    l.contains("/end DEF_CHARACTERISTIC") -> {
                                        break
                                    }

                                    else -> {
                                        l.trim().split(" ").forEach {
                                            linesFound.add(it)
                                        }
                                    }
                                }
                            }

                            val mRes = mutableListOf<String>()

                            linesFound.forEach { found ->
                                found.split(" ").forEach { mRes.add(it) }
                            }

                            mOptionalParams[FunctionOpt.DefCharacteristics] = DefCharacs(
                                list = mRes
                            )
                        }

                        line.startsWith("/begin REF_CHARACTERISTIC") -> {
                            val linesFound = mutableListOf<String>()

                            for (l in input.subList(index + 1, input.size)) {
                                when {
                                    l.contains("/end REF_CHARACTERISTIC") -> {
                                        break
                                    }

                                    else -> {
                                        l.trim().split(" ").forEach {
                                            linesFound.add(it)
                                        }
                                    }
                                }
                            }

                            val mRefs = mutableListOf<String>()

                            linesFound.forEach { found ->
                                found.split(" ").forEach {
                                    if (it.isNotBlank()) { mRefs.add(it) }
                                }
                            }

                            mOptionalParams[FunctionOpt.RefCharacteristics] = RefCharacs(mRefs)
                        }

                        line.startsWith("/begin IN_MEASUREMENT") -> {
                            val linesFound = mutableListOf<String>()

                            for (l in input.subList(index + 1, input.size)) {
                                when {
                                    l.contains("/end IN_MEASUREMENT") -> {
                                        break
                                    }

                                    else -> {
                                        linesFound.add(l.trim())
                                    }
                                }
                            }

                            val mRes = mutableListOf<String>()

                            linesFound.forEach { found ->
                                found.split(" ").forEach {
                                    if (it.isNotBlank()) { mRes.add(it) }
                                }
                            }

                            mOptionalParams[FunctionOpt.InMeasurements] = InMeasurement(mRes)
                        }

                        line.startsWith("/begin OUT_MEASUREMENT") -> {
                            val linesFound = mutableListOf<String>()
                            for (l in input.subList(index + 1, input.size)) {
                                when {
                                    l.contains("/end OUT_MEASUREMENT") -> {
                                        break
                                    }

                                    else -> {
                                        linesFound.add(l.trim())
                                    }
                                }
                            }

                            val mRes = mutableListOf<String>()

                            linesFound.forEach { found ->
                                found.split(" ").forEach {
                                    if (it.isNotBlank()) { mRes.add(it) }
                                }
                            }

                            mOptionalParams[FunctionOpt.OutMeasurements] = OutMeasurements(mRes)
                        }

                        line.startsWith("/begin LOC_MEASUREMENT") -> {
                            val linesFound = mutableListOf<String>()
                            for (l in input.subList(index + 1, input.size)) {
                                when {
                                    l.contains("/end LOC_MEASUREMENT") -> {
                                        break
                                    }

                                    else -> {
                                        linesFound.add(l.trim())
                                    }
                                }
                            }

                            val mRes = mutableListOf<String>()

                            linesFound.forEach { found ->
                                found.split(" ").forEach {
                                    if (it.isNotBlank()) { mRes.add(it) }
                                }
                            }

                            mOptionalParams[FunctionOpt.LocMeasurement] = LocMeasurement(mRes)
                        }

                        line.startsWith("/begin SUB_FUNCTION") -> {
                            val linesFound = mutableListOf<String>()
                            for (l in input.subList(index + 1, input.size)) {
                                when {
                                    l.contains("/end SUB_FUNCTION") -> {
                                        break
                                    }

                                    else -> {
                                        linesFound.add(l.trim())
                                    }
                                }
                            }

                            val mRes = mutableListOf<String>()

                            linesFound.forEach { found ->
                                found.split(" ").forEach {
                                    if (it.isNotBlank()) { mRes.add(it) }
                                }
                            }

                            mOptionalParams[FunctionOpt.SubFunction] = SubFunction(mRes)
                        }

                        line.startsWith("FUNCTION_VERSION") -> {
                            mOptionalParams[FunctionOpt.FunctionVersion] = FunctionVersion(
                                line.replace("FUNCTION_VERSION", "")
                                    .replace("\"", "").trim()
                            )
                        }

                        line.startsWith("/begin ANNOTATION") -> {
                            // TODO: Test Annotation Parse
                            val subL = input.subList(index, input.size)
                            var annotList = listOf("")
                            var found = false
                            subL.forEachIndexed { ind, lin ->
                                when {
                                    !found && lin.contains("/end ANNOTATION") -> {
                                        annotList = subL.subList(0, ind)
                                        found = true
                                    }
                                }
                            }

                            mAnnotation.add(annotList.parseAnnotation())
                        }

                        line.startsWith("/begin IF_DATA") -> {
                            // TODO: Test IfData Parser
                            mIfData.add(input.subList(index, input.size).parseIfDataSection())
                        }

                        line.startsWith("/begin FUNCTION") -> {
                            val result = line.replace("/begin FUNCTION", "").trim()

                            if (result.isNotBlank()) {
                                mName = result
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            when {
                // Add Annotations
                mAnnotation.isNotEmpty() -> {
                    mOptionalParams[FunctionOpt.Annotation] = AnnotList(mAnnotation)
                }

                // Add IfData
                mIfData.isNotEmpty() -> {
                    mOptionalParams[FunctionOpt.IfData] = IfDataList(mIfData)
                }
            }

            return if (mName.isNotBlank()) {
                A2LFunction(
                    name = mName,
                    longIdentifier = input[1].replace("\"", "").trim(),
                    optionalParameters = mOptionalParams
                )
            } else {
                A2LFunction(
                    name = input[1].trim(),
                    longIdentifier = input[2].replace("\"", "").trim(),
                    optionalParameters = mOptionalParams
                )
            }
        } else return A2LFunction()
    }
}

