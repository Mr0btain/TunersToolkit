package org.mr0btain.tunerstoolkit.core.logging

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppLoggerTest {
    @BeforeTest
    fun reset() {
        AppLogger.clear()
        AppLogger.debug = false
        AppLogger.capacity = 2_000
    }

    @AfterTest
    fun cleanUp() = reset()

    @Test
    fun keepsLinesInOrderWithTheirLevel() {
        AppLogger.i("Test:Open", "Opened file")
        AppLogger.e("Test:Parse", "Failed", IllegalStateException("boom"))

        val entries = AppLogger.entries.value
        assertEquals(listOf(LogLevel.INFO, LogLevel.ERROR), entries.map { it.level })
        assertEquals("Test:Parse", entries[1].tag)
        assertNotNull(entries[1].throwable)
        assertTrue(entries[1].throwable!!.contains("boom"))
    }

    @Test
    fun debugOnlyWhenTurnedOn() {
        AppLogger.d("Test", "hidden")
        assertTrue(AppLogger.entries.value.isEmpty())

        AppLogger.debug = true
        AppLogger.d("Test", "shown")
        assertEquals(listOf("shown"), AppLogger.entries.value.map { it.message })
    }

    @Test
    fun dropsTheOldestPastCapacity() {
        AppLogger.capacity = 3
        repeat(5) { AppLogger.i("Test", "line $it") }
        assertEquals(listOf("line 2", "line 3", "line 4"), AppLogger.entries.value.map { it.message })
    }
}
