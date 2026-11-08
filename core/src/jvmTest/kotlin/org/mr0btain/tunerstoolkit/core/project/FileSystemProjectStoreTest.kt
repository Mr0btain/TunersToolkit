package org.mr0btain.tunerstoolkit.core.project

import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class FileSystemProjectStoreTest : ProjectStoreContract() {
    private val roots = mutableListOf<java.io.File>()

    override fun newStore(): ProjectStore {
        val root = Files.createTempDirectory("tt-store").toFile()
        roots += root
        return FileSystemProjectStore(root.absolutePath)
    }

    @AfterTest
    fun cleanUp() {
        roots.forEach { it.deleteRecursively() }
    }

    @Test
    fun pathsCantLeaveTheRoot() = runTest {
        val store = newStore()
        assertFailsWith<IllegalArgumentException> { store.write("../escape.bin", byteArrayOf(1)) }
    }
}
