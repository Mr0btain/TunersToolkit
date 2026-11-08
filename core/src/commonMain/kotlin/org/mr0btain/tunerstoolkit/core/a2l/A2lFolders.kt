package org.mr0btain.tunerstoolkit.core.a2l

import org.mr0btain.tunerstoolkit.core.calibration.CalibrationSet
import org.mr0btain.tunerstoolkit.core.calibration.ObjectFolder

/**
 * An A2L module's FUNCTIONs and GROUPs as folders of objects, for the map list. Objects in none of them go in a last
 * folder of their own, so every object can be found
 */
object A2lFolders {
    /**
     * FUNCTIONs as folders: the ones that aren't another's SUB_FUNCTION at the top, each holding its DEF_ and
     * REF_CHARACTERISTICs and the objects whose FUNCTION_LIST names it
     */
    fun byFunction(module: A2lModule, set: CalibrationSet): List<ObjectFolder> {
        val byName = module.functions.associateBy { it.name }
        val listed = mutableMapOf<String, MutableList<String>>()
        (module.characteristics.map { it.name to it.options.functionList } + module.axisPts.map { it.name to it.options.functionList })
            .forEach { (name, functions) -> functions.forEach { listed.getOrPut(it) { mutableListOf() } += name } }
        val subs = module.functions.flatMap { it.subFunctions }.toSet()

        fun folder(function: A2lFunction, seen: Set<String>): ObjectFolder = ObjectFolder(
            name = function.name,
            description = function.longId,
            folders = function.subFunctions.filter { it !in seen }.mapNotNull { byName[it] }.map { folder(it, seen + it.name) },
            objects = (function.defCharacteristics + function.refCharacteristics + listed[function.name].orEmpty()).distinct().filter { set[it] != null }
        )

        val roots = module.functions.filter { it.name !in subs }.map { folder(it, setOf(it.name)) }
        return roots + rest(roots, set, "Not in a function")
    }

    /** GROUPs as folders: ROOT groups at the top (or those in no other group, when none are marked ROOT) */
    fun byGroup(module: A2lModule, set: CalibrationSet): List<ObjectFolder> {
        val byName = module.groups.associateBy { it.name }
        val subs = module.groups.flatMap { it.subGroups }.toSet()

        fun folder(group: A2lGroup, seen: Set<String>): ObjectFolder = ObjectFolder(
            name = group.name,
            description = group.longId,
            folders = group.subGroups.filter { it !in seen }.mapNotNull { byName[it] }.map { folder(it, seen + it.name) },
            objects = group.refCharacteristics.distinct().filter { set[it] != null }
        )

        val tops = module.groups.filter { it.root }.ifEmpty { module.groups.filter { it.name !in subs } }
        val roots = tops.map { folder(it, setOf(it.name)) }
        return roots + rest(roots, set, "Not in a group")
    }

    /** A folder of the objects in none of [roots], if there are any */
    private fun rest(roots: List<ObjectFolder>, set: CalibrationSet, name: String): List<ObjectFolder> {
        val placed = mutableSetOf<String>()
        fun collect(folder: ObjectFolder, depth: Int) {
            if (depth > 32) return
            placed += folder.objects
            folder.folders.forEach { collect(it, depth + 1) }
        }
        roots.forEach { collect(it, 0) }
        val others = set.objects.map { it.name }.filter { it !in placed }.sortedBy { it.lowercase() }
        return if (others.isEmpty()) emptyList() else listOf(ObjectFolder(name, objects = others))
    }
}
