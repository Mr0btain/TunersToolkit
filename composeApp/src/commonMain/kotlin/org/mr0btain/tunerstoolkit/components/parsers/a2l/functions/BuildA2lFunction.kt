package org.mr0btain.tunerstoolkit.components.parsers.a2l.functions
import org.mr0btain.tunerstoolkit.components.errorstuff.LogDataEntry
import org.mr0btain.tunerstoolkit.components.errorstuff.safeCall
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
import org.mr0btain.tunerstoolkit.components.parsers.a2l.classes.data.parseAnnotation
import org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.components.parseIfDataSection
import org.mr0btain.tunerstoolkit.components.parsers.a2l.objects.A2lOptional

/**
 * Parses and builds a Function based on the parameters it contains if they exist
 */

fun buildA2lFunction(input: List<String>): Pair<A2LFunction, List<LogDataEntry>> {
    val process = "BuildFunction"
    val errors = mutableListOf<LogDataEntry>()
    val annotations = mutableListOf<Annotation>()
    val ifData = mutableListOf<org.mr0btain.tunerstoolkit.components.parsers.a2l.ifdata.IfData>()
    var name = ""
    val optionalParams = mutableMapOf<FunctionOpt, A2lOptional>()

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

                val res = mutableListOf<String>()

                linesFound.forEach { found ->
                    found.split(" ").forEach { res.add(it) }
                }

                optionalParams[FunctionOpt.DefCharacteristics] = DefCharacs(
                    list = res
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

                val refs = mutableListOf<String>()

                linesFound.forEach { found ->
                    found.split(" ").forEach {
                        if (it.isNotBlank()) { refs.add(it) }
                    }
                }

                optionalParams[FunctionOpt.RefCharacteristics] = RefCharacs(refs)
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

                val res = mutableListOf<String>()

                linesFound.forEach { found ->
                    found.split(" ").forEach {
                        if (it.isNotBlank()) { res.add(it) }
                    }
                }

                optionalParams[FunctionOpt.InMeasurements] = InMeasurement(res)
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

                val res = mutableListOf<String>()

                linesFound.forEach { found ->
                    found.split(" ").forEach {
                        if (it.isNotBlank()) { res.add(it) }
                    }
                }

                optionalParams[FunctionOpt.OutMeasurements] = OutMeasurements(res)
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

                val res = mutableListOf<String>()

                linesFound.forEach { found ->
                    found.split(" ").forEach {
                        if (it.isNotBlank()) { res.add(it) }
                    }
                }

                optionalParams[FunctionOpt.LocMeasurement] = LocMeasurement(res)
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

                val res = mutableListOf<String>()

                linesFound.forEach { found ->
                    found.split(" ").forEach {
                        if (it.isNotBlank()) { res.add(it) }
                    }
                }

                optionalParams[FunctionOpt.SubFunction] = SubFunction(res)
            }

            line.startsWith("FUNCTION_VERSION") -> {
                optionalParams[FunctionOpt.FunctionVersion] = FunctionVersion(
                    line.replace("FUNCTION_VERSION", "")
                        .replace("\"", "").trim()
                )
            }

            line.startsWith("/begin ANNOTATION") -> {
                // TODO: Test Annotation Parse
                val subList = input.subList(index, input.size)

                safeCall {
                    annotations.add(parseAnnotation(subList))
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to parse Annotations!; ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("/begin IF_DATA") -> {
                safeCall {
                    ifData.add(input.subList(index, input.size).parseIfDataSection())
                }.onFailure {
                    errors.add(
                        LogDataEntry(
                            process = process,
                            message = "Failure to parse IfData!; ${it.message}",
                            cause = it.cause.toString(),
                            error = true
                        )
                    )
                }
            }

            line.startsWith("/begin FUNCTION") -> {
                val result = line.replace("/begin FUNCTION", "").trim()

                if (result.isNotBlank()) {
                    name = result
                }
            }
        }
    }

    when {
        // Add Annotations
        annotations.isNotEmpty() -> {
            optionalParams[FunctionOpt.Annotation] = AnnotList(annotations)
        }

        // Add IfData
        ifData.isNotEmpty() -> {
            optionalParams[FunctionOpt.IfData] = IfDataList(ifData)
        }
    }

    safeCall {
        return if (name.isNotBlank()) {
            Pair(
                A2LFunction(
                    name = name,
                    longIdentifier = input[1].replace("\"", "").trim(),
                    optionalParameters = optionalParams
                ),
                errors
            )
        } else {
            Pair(
                A2LFunction(
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
                message = "Failure to Build A2l Function!: ${it.message}",
                cause = it.cause.toString(),
                error = true
            )
        )
    }

    return Pair(A2LFunction(), errors)
}

