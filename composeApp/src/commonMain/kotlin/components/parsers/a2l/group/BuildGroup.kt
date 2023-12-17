package components.parsers.a2l.group

import components.errorstuff.LogDataEntry
import components.errorstuff.safeCall
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

fun buildGroup(input: List<String>): Pair<Group, List<LogDataEntry>> {
    val process = "BuildGroup"
    val errors = mutableListOf<LogDataEntry>()
    var name = ""
    val optionalParams = mutableMapOf<GroupOpt, A2lOptional>()
    val annotations = mutableListOf<Annotation>()
    val ifData = mutableListOf<components.parsers.a2l.ifdata.IfData>()

    input.forEachIndexed { index, line ->
        when {
            line.startsWith("/begin ANNOTATION") -> {
                val subList = input.subList(index, input.size)

                safeCall {
                    annotations.add(parseAnnotation(subList))
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to parse Annotation! ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("/begin REF_CHARACTERISTIC") -> {
                val refCharacteristics = mutableListOf<String>()

                input.subList(index + 1, input.size).forEach { section ->
                    when {
                        section.startsWith("/end REF_CHARACTERISTIC") -> {
                            optionalParams[GroupOpt.RefCharacteristics] = RefCharacs(refCharacteristics)
                        }
                        else -> refCharacteristics.addAll(section.trim().split(" "))
                    }
                }
            }

            line.startsWith("ROOT") -> {
                optionalParams[GroupOpt.Root] = Root.True
            }

            line.startsWith("/begin REF_MEASUREMENT") -> {
                val refMeasurements = mutableListOf<String>()

                input.subList(index + 1, input.size).forEach { section ->
                    when {
                        section.startsWith("/end REF_MEASUREMENT") -> {
                            optionalParams[GroupOpt.RefMeasurements] = RefMeasurements(refMeasurements)
                        }
                        else -> refMeasurements.addAll(section.trim().split(" "))
                    }
                }
            }

            line.startsWith("/begin FUNCTION_LIST") -> {
                val functionList = mutableListOf<String>()

                input.subList(index + 1, input.size).forEach { section ->
                    when {
                        section.startsWith("/end FUNCTION_LIST") -> {
                            optionalParams[GroupOpt.FunctionList] = FunctionList(functionList)
                        }
                        else -> functionList.addAll(section.trim().split(" "))
                    }
                }
            }

            line.startsWith("/begin SUB_GROUP") -> {
                val subGroup = mutableListOf<String>()

                input.subList(index + 1, input.size).forEach { section ->
                    when {
                        section.startsWith("/end SUB_GROUP") -> {
                            optionalParams[GroupOpt.SubGroup] = SubGroup(subGroup)
                        }
                        else -> subGroup.addAll(section.trim().split(" "))
                    }
                }
            }

            line.startsWith("/begin IF_DATA") -> {
                safeCall {
                    ifData.add(input.subList(index, input.size).parseIfDataSection())
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to parse IfData! ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("/begin GROUP") -> {
                val result = line.replace("/begin GROUP", "").trim()
                if (result.isNotBlank()) {
                    name = result
                }
            }
        }
    }

    when {
        // Add Annotations
        annotations.isNotEmpty() -> {
            optionalParams[GroupOpt.Annotation] = AnnotList(annotations)
        }

        // Add IfData
        ifData.isNotEmpty() -> {
            optionalParams[GroupOpt.IfData] = IfDataList(ifData)
        }
    }

    safeCall {
        return if (name.isNotBlank()) {
            Pair(
                Group(
                    name = name,
                    longIdentifier = input[1].replace("\"", "").trim(),
                    optionalParameters = optionalParams
                ),
                errors
            )
        } else {
            Pair(
                Group(
                    name = input[1].trim(),
                    longIdentifier = input[2].replace("\"", "").trim(),
                    optionalParameters = optionalParams
                ),
                errors
            )
        }
    }.onFailure {
        errors.add(
            LogDataEntry(
                process = process,
                message = "Failure to Build Group!: ${it.message}",
                cause = it.cause.toString(),
                error = true
            )
        )
    }

    return Pair(Group(), errors)
}
