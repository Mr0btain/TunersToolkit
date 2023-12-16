package components.parsers.a2l.group

import components.parsers.a2l.classes.data.AnnotList
import components.parsers.a2l.classes.data.Annotation
import components.parsers.a2l.classes.data.FunctionList
import components.parsers.a2l.classes.data.IfDataList
import components.parsers.a2l.classes.data.RefCharacs
import components.parsers.a2l.classes.data.RefMeasurements
import components.parsers.a2l.classes.data.SubGroup
import components.parsers.a2l.classes.data.parseAnnotation
import components.parsers.a2l.classes.enums.Root
import components.parsers.a2l.ifdata.components.parseIfDataSection
import components.parsers.a2l.objects.A2lOptional

/**
 * Parses and returns a Group from a list of strings with parameters in the data
 */

fun buildGroup(input: List<String>): Group {
    // Check if size it big enough else we get index out of bounds
    if (input.size >= 2) {
        var mName = ""
        val mOptionalParams = mutableMapOf<GroupOpt, A2lOptional>()
        val mAnnotation = mutableListOf<Annotation>()
        val mIfData = mutableListOf<components.parsers.a2l.ifdata.IfData>()

        try {
            input.forEachIndexed { index, line ->
                when {
                    line.startsWith("/begin ANNOTATION") -> {
                        val subList = input.subList(index, input.size)

                        mAnnotation.add(parseAnnotation(subList))
                    }

                    line.startsWith("/begin REF_CHARACTERISTIC") -> {
                        val mRefChars = mutableListOf<String>()

                        input.subList(index + 1, input.size).forEach { section ->
                            when {
                                section.startsWith("/end REF_CHARACTERISTIC") -> {
                                    mOptionalParams[GroupOpt.RefCharacteristics] = RefCharacs(mRefChars)
                                    return@forEach
                                }
                                else -> section.trim().split (" ").forEach() {mRefChars.add(it)}
                            }
                        }

                    }

                    line.startsWith("ROOT") -> {
                        mOptionalParams[GroupOpt.Root] = Root.True
                    }

                    line.startsWith("/begin REF_MEASUREMENT") -> {
                        val mRefMeas = mutableListOf<String>()

                        input.subList(index + 1, input.size).forEach { section ->
                            when {
                                section.startsWith("/end REF_MEASUREMENT") -> {
                                    mOptionalParams[GroupOpt.RefMeasurements] = RefMeasurements(mRefMeas)
                                    return@forEach
                                }
                                else -> section.trim().split (" ").forEach() {mRefMeas.add(it)}
                            }
                        }
                    }

                    line.startsWith("/begin FUNCTION_LIST") -> {
                        val mFunctionList = mutableListOf<String>()

                        input.subList(index + 1, input.size).forEach { section ->
                            when {
                                section.startsWith("/end FUNCTION_LIST") -> {
                                    mOptionalParams[GroupOpt.FunctionList] = FunctionList(mFunctionList)
                                    return@forEach
                                }
                                else -> section.trim().split (" ").forEach() {mFunctionList.add(it)}
                            }
                        }
                    }

                    line.startsWith("/begin SUB_GROUP") -> {
                        val mSubGroup = mutableListOf<String>()

                        input.subList(index + 1, input.size).forEach { section ->
                            when {
                                section.startsWith("/end SUB_GROUP") -> {
                                    mOptionalParams[GroupOpt.SubGroup] = SubGroup(mSubGroup)
                                    return@forEach
                                }
                                else -> section.trim().split (" ").forEach() {mSubGroup.add(it)}
                            }
                        }
                    }

                    line.startsWith("/begin IF_DATA") -> {
                        mIfData.add(input.subList(index, input.size).parseIfDataSection())
                    }

                    line.startsWith("/begin GROUP") -> {
                        val result = line.replace("/begin GROUP", "").trim()
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
                mOptionalParams[GroupOpt.Annotation] = AnnotList(mAnnotation)
            }

            // Add IfData
            mIfData.isNotEmpty() -> {
                mOptionalParams[GroupOpt.IfData] = IfDataList(mIfData)
            }
        }

        return if (mName.isNotBlank()) {
            Group(
                name = mName,
                longIdentifier = input[1].replace("\"", "").trim(),
                optionalParameters = mOptionalParams
            )
        } else {
            Group(
                name = input[1].trim(),
                longIdentifier = input[2].replace("\"", "").trim(),
                optionalParameters = mOptionalParams
            )
        }
    } else return Group()
}
