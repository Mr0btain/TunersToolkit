package org.mr0btain.tunerstoolkit.core.project

import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

/**
 * A [ProjectStore] in a folder on disk, for desktop, Android and iOS (browsers have no file system kotlinx-io can
 * reach). Writes go to a temporary file first and are moved into place, so a crash mid-save never leaves half a file
 */
class FileSystemProjectStore(
    private val root: Path,
    private val fileSystem: FileSystem = SystemFileSystem
) : ProjectStore {
    constructor(root: String) : this(Path(root))

    override suspend fun read(path: String): ByteArray? {
        val file = resolve(path)
        val metadata = fileSystem.metadataOrNull(file) ?: return null
        if (!metadata.isRegularFile) return null
        return fileSystem.source(file).buffered().use { it.readByteArray() }
    }

    override suspend fun write(path: String, bytes: ByteArray) {
        val file = resolve(path)
        file.parent?.let { fileSystem.createDirectories(it) }
        val temporary = Path(file.parent ?: root, ".${file.name}.tmp")
        fileSystem.sink(temporary).buffered().use { it.write(bytes) }
        fileSystem.atomicMove(temporary, file)
    }

    override suspend fun list(directory: String): List<String> {
        val folder = resolve(directory)
        if (fileSystem.metadataOrNull(folder)?.isDirectory != true) return emptyList()
        return fileSystem.list(folder).map { it.name }.filterNot { it.startsWith(".") && it.endsWith(".tmp") }.sorted()
    }

    override suspend fun delete(path: String) {
        deleteTree(resolve(path))
    }

    private fun deleteTree(path: Path) {
        val metadata = fileSystem.metadataOrNull(path) ?: return
        if (metadata.isDirectory) fileSystem.list(path).forEach(::deleteTree)
        fileSystem.delete(path, mustExist = false)
    }

    private fun resolve(path: String): Path {
        val parts = normalize(path).split('/').filter { it.isNotEmpty() }
        require(parts.none { it == "." || it == ".." }) { "A project path can't leave the store: $path" }
        return if (parts.isEmpty()) root else Path(root, *parts.toTypedArray())
    }
}
