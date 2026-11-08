package org.mr0btain.tunerstoolkit.core.checksum

import org.mr0btain.applogger.AppLogger
import org.mr0btain.tunerstoolkit.core.error.TkResult
import org.mr0btain.tunerstoolkit.core.project.ProjectStore

/**
 * Checksum modules of your own, kept in the app's store (`checksums/<id>.ttsum`) so every image can use them, not
 * just the project they were made in. Modules loaded from a file or made from your own checks go here.
 *
 * What a file says about its status isn't trusted: the caller decides how to treat what [loadAll] returns.
 * Storage failures are logged and leave the library as it was; a module that can't be read is skipped.
 */
class ModuleLibrary(private val store: ProjectStore) {
    /** Every module in the library, by name */
    suspend fun loadAll(): List<DefinedModule> {
        val names = runCatching { store.list(DIR) }.getOrElse {
            AppLogger.e("$TAG:LoadAll", "Couldn't list the checksum modules", it)
            return emptyList()
        }
        return names.filter { it.endsWith(EXTENSION) }.mapNotNull { name ->
            val bytes = runCatching { store.read("$DIR/$name") }.getOrNull() ?: return@mapNotNull null
            when (val result = TtSum.read(bytes, name)) {
                is TkResult.Success -> result.data.value
                is TkResult.Failure -> {
                    AppLogger.w("$TAG:LoadAll", "Skipped $name: ${result.error.message}")
                    null
                }
            }
        }.sortedBy { it.name.lowercase() }
    }

    /** Keeps [module], replacing one with the same id. @return false when it couldn't be written */
    suspend fun save(module: DefinedModule): Boolean = runCatching {
        store.write(pathOf(module.id), TtSum.write(module).encodeToByteArray())
        AppLogger.i("$TAG:Save", "Kept checksum module ${module.id}, ${module.checkList.size} checks")
    }.onFailure { AppLogger.e("$TAG:Save", "Couldn't keep checksum module ${module.id}", it) }.isSuccess

    /** Forgets the module with [id]. @return false when it couldn't be removed */
    suspend fun delete(id: String): Boolean = runCatching { store.delete(pathOf(id)) }
        .onFailure { AppLogger.e("$TAG:Delete", "Couldn't remove checksum module $id", it) }.isSuccess

    companion object {
        private const val TAG = "ModuleLibrary"
        const val DIR = "checksums"
        private const val EXTENSION = ".ttsum"

        /** An id made from [name]: lower case letters, digits and dashes */
        fun idFor(name: String): String =
            name.lowercase().map { if (it.isLetterOrDigit() && it.code < 128) it else '-' }.joinToString("")
                .split('-').filter { it.isNotEmpty() }.joinToString("-").ifEmpty { "module" }.take(MAX_ID)

        private const val MAX_ID = 64

        /** The file for [id], with anything that could leave the folder taken out */
        private fun pathOf(id: String): String = "$DIR/${idFor(id)}$EXTENSION"
    }
}
