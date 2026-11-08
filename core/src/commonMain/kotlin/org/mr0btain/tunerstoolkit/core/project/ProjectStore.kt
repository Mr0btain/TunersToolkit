package org.mr0btain.tunerstoolkit.core.project

/**
 * Where projects are kept: a small tree of files under one root, addressed by relative paths with `/` between the
 * parts. Each platform has one (files on desktop, Android and iOS, the Origin Private File System on the web), and
 * [ProjectRepository] is the only thing that knows what goes in it.
 *
 * Any call can throw when the storage itself fails; the repository turns that into a [ProjectError].
 */
interface ProjectStore {
    /** What's at [path], or null when there's no file there */
    suspend fun read(path: String): ByteArray?

    /** Puts [bytes] at [path], making the folders it's in and replacing what was there */
    suspend fun write(path: String, bytes: ByteArray)

    /** The names (not paths) of what's directly inside [directory]; empty when it doesn't exist */
    suspend fun list(directory: String): List<String>

    /** Removes the file or folder at [path] and everything in it. Nothing being there is fine */
    suspend fun delete(path: String)

    /** Whether the projects outlive the app: false when there's nowhere to keep them (a fallback in memory) */
    val persistent: Boolean get() = true
}

/** Keeps everything in memory, for tests and as the fallback when a platform has nowhere to keep files */
class MemoryProjectStore : ProjectStore {
    private val files = mutableMapOf<String, ByteArray>()

    override val persistent: Boolean get() = false

    override suspend fun read(path: String): ByteArray? = files[normalize(path)]?.copyOf()

    override suspend fun write(path: String, bytes: ByteArray) {
        files[normalize(path)] = bytes.copyOf()
    }

    override suspend fun list(directory: String): List<String> {
        val prefix = normalize(directory).let { if (it.isEmpty()) "" else "$it/" }
        return files.keys.filter { it.startsWith(prefix) }.map { it.removePrefix(prefix).substringBefore('/') }.distinct().sorted()
    }

    override suspend fun delete(path: String) {
        val target = normalize(path)
        files.keys.removeAll { it == target || it.startsWith("$target/") }
    }
}

/** [path] without leading, trailing or doubled slashes */
internal fun normalize(path: String): String = path.split('/').filter { it.isNotEmpty() }.joinToString("/")
