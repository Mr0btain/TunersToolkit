package org.mr0btain.tunerstoolkit.core.calibration

/**
 * A folder of objects, as a definition groups them (A2L FUNCTIONs or GROUPs). [objects] are names; ones the set
 * doesn't have are skipped when shown
 */
class ObjectFolder(
    val name: String,
    val description: String = "",
    val folders: List<ObjectFolder> = emptyList(),
    val objects: List<String> = emptyList()
)

/** One line of the map list */
sealed interface TreeRow {
    val depth: Int

    /** [path] names it for keeping it open: its names from the top, joined with / */
    data class Folder(val path: String, val folder: ObjectFolder, override val depth: Int, val count: Int, val expanded: Boolean) : TreeRow

    data class Object(val obj: CalibrationObject, override val depth: Int, val path: String) : TreeRow
}

/**
 * The map list as rows: folders with their objects under them when open. A search shows only objects whose name,
 * description or display name has it in (any case) with every folder on the way to them open
 */
object ObjectTree {
    /** Deep enough for any real tree; stops a FUNCTION that's its own sub-function from going round forever */
    const val MAX_DEPTH = 32

    fun rows(roots: List<ObjectFolder>, set: CalibrationSet, expanded: Set<String>, query: String = ""): List<TreeRow> {
        val search = query.trim().lowercase()
        val rows = mutableListOf<TreeRow>()

        fun matches(obj: CalibrationObject) = search.isEmpty() ||
            search in obj.name.lowercase() || search in obj.description.lowercase() || obj.displayName?.lowercase()?.contains(search) == true

        /** How many matching objects are in [folder] and under it */
        fun count(folder: ObjectFolder, depth: Int): Int {
            if (depth > MAX_DEPTH) return 0
            return folder.objects.count { name -> set[name]?.let(::matches) == true } + folder.folders.sumOf { count(it, depth + 1) }
        }

        fun add(folder: ObjectFolder, parent: String, depth: Int) {
            if (depth > MAX_DEPTH) return
            val path = if (parent.isEmpty()) folder.name else "$parent/${folder.name}"
            val total = count(folder, depth)
            if (total == 0 && search.isNotEmpty()) return
            val open = search.isNotEmpty() || path in expanded
            rows += TreeRow.Folder(path, folder, depth, total, open)
            if (!open) return
            folder.folders.forEach { add(it, path, depth + 1) }
            folder.objects.forEach { name ->
                val obj = set[name] ?: return@forEach
                if (matches(obj)) rows += TreeRow.Object(obj, depth + 1, path)
            }
        }

        roots.forEach { add(it, "", 0) }
        return rows
    }

    /** Every object of [set] in one flat folder, by name */
    fun all(set: CalibrationSet, name: String = "All objects"): ObjectFolder =
        ObjectFolder(name, objects = set.objects.map { it.name }.sortedBy { it.lowercase() })
}
