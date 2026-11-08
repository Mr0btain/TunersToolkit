package org.mr0btain.tunerstoolkit.core.checksum

import kotlinx.coroutines.test.runTest
import org.mr0btain.tunerstoolkit.core.project.MemoryProjectStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModuleLibraryTest {
    @Test
    fun modulesAreKeptReplacedAndForgotten() = runTest {
        val store = MemoryProjectStore()
        val library = ModuleLibrary(store)
        assertTrue(library.loadAll().isEmpty())

        val mine = ChecksumModules.demo.copy(id = "mine", name = "Mine", status = ModuleStatus.USER)
        assertTrue(library.save(mine))
        assertTrue(library.save(ChecksumModules.demo.copy(id = "another", name = "Another")))
        assertEquals(listOf("Another", "Mine"), library.loadAll().map { it.name })

        // The same id replaces it
        library.save(mine.copy(description = "Changed"))
        assertEquals("Changed", library.loadAll().single { it.id == "mine" }.description)
        assertEquals(mine.checkList, library.loadAll().single { it.id == "mine" }.checkList)

        assertTrue(library.delete("mine"))
        assertEquals(listOf("another"), library.loadAll().map { it.id })
    }

    @Test
    fun aBrokenFileIsSkipped() = runTest {
        val store = MemoryProjectStore()
        store.write("checksums/broken.ttsum", "not json".encodeToByteArray())
        store.write("checksums/notes.txt", "ignored".encodeToByteArray())
        val library = ModuleLibrary(store)
        library.save(ChecksumModules.demo.copy(id = "fine"))
        assertEquals(listOf("fine"), library.loadAll().map { it.id })
    }

    @Test
    fun idsStayInsideTheFolder() {
        assertEquals("my-ecu-stage-1", ModuleLibrary.idFor("My ECU: stage 1!"))
        assertEquals("etc-passwd", ModuleLibrary.idFor("../../etc/passwd"))
        assertEquals("module", ModuleLibrary.idFor("///"))
    }
}
