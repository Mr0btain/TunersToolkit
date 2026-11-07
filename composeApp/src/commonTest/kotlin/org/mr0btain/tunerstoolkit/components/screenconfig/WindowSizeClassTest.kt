package org.mr0btain.tunerstoolkit.components.screenconfig

import kotlin.test.Test
import kotlin.test.assertEquals

class WindowSizeClassTest {
    @Test
    fun widthBreakpoints() {
        assertEquals(WindowWidthSizeClass.Compact, WindowWidthSizeClass.fromWidth(0f))
        assertEquals(WindowWidthSizeClass.Compact, WindowWidthSizeClass.fromWidth(599.9f))
        assertEquals(WindowWidthSizeClass.Medium, WindowWidthSizeClass.fromWidth(600f))
        assertEquals(WindowWidthSizeClass.Medium, WindowWidthSizeClass.fromWidth(839.9f))
        assertEquals(WindowWidthSizeClass.Expanded, WindowWidthSizeClass.fromWidth(840f))
    }

    @Test
    fun heightBreakpoints() {
        assertEquals(WindowHeightSizeClass.Compact, WindowHeightSizeClass.fromHeight(479f))
        assertEquals(WindowHeightSizeClass.Medium, WindowHeightSizeClass.fromHeight(480f))
        assertEquals(WindowHeightSizeClass.Medium, WindowHeightSizeClass.fromHeight(899f))
        assertEquals(WindowHeightSizeClass.Expanded, WindowHeightSizeClass.fromHeight(900f))
    }
}
