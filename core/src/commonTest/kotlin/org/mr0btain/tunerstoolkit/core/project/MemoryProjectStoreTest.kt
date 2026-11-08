package org.mr0btain.tunerstoolkit.core.project

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The behaviour every [ProjectStore] has to have; FileSystemProjectStoreTest runs the same on disk */
abstract class ProjectStoreContract {
    abstract fun newStore(): ProjectStore

    @Test
    fun readWriteListDelete() = runTest {
        val store = newStore()
        assertNull(store.read("a/b.json"))
        assertEquals(emptyList(), store.list("a"))

        store.write("a/b.json", byteArrayOf(1, 2, 3))
        store.write("a/c/d.bin", byteArrayOf(4))
        store.write("/a//e.json/", byteArrayOf(5))
        assertContentEquals(byteArrayOf(1, 2, 3), store.read("a/b.json"))
        assertContentEquals(byteArrayOf(5), store.read("a/e.json"))
        assertEquals(listOf("b.json", "c", "e.json"), store.list("a"))
        assertEquals(listOf("a"), store.list(""))
        assertNull(store.read("a/c"))

        store.write("a/b.json", byteArrayOf(9))
        assertContentEquals(byteArrayOf(9), store.read("a/b.json"))

        store.delete("a/c")
        assertEquals(listOf("b.json", "e.json"), store.list("a"))
        store.delete("a")
        store.delete("not/there")
        assertEquals(emptyList(), store.list(""))
    }
}

class MemoryProjectStoreTest : ProjectStoreContract() {
    override fun newStore(): ProjectStore = MemoryProjectStore()
}
