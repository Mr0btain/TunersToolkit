package org.mr0btain.tunerstoolkit.core.error

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TkResultTest {
    private enum class TestError(override val message: String) : Error {
        BROKEN("It broke")
    }

    private val success: TkResult<Int, TestError> = TkResult.Success(2)
    private val failure: TkResult<Int, TestError> = TkResult.Failure(TestError.BROKEN)

    @Test
    fun mapOnlyChangesSuccess() {
        assertEquals(TkResult.Success(4), success.map { it * 2 })
        assertEquals(failure, failure.map { it * 2 })
    }

    @Test
    fun flatMapChainsAndStopsAtTheFirstFailure() {
        assertEquals(TkResult.Success("2"), success.flatMap { TkResult.Success(it.toString()) })
        assertEquals(failure, success.flatMap { failure })
        assertEquals(failure, failure.flatMap { TkResult.Success(it + 1) })
    }

    @Test
    fun callbacksRunOnlyForTheirSide() {
        var seen = ""
        success.onSuccess { seen += "s$it" }.onFailure { seen += "f" }
        failure.onSuccess { seen += "s" }.onFailure { seen += "f${it.message}" }
        assertEquals("s2fIt broke", seen)
    }

    @Test
    fun accessors() {
        assertEquals(2, success.getOrNull())
        assertNull(failure.getOrNull())
        assertNull(success.errorOrNull())
        assertEquals(TestError.BROKEN, failure.errorOrNull())
        assertEquals(-1, failure.valueOr { -1 })
        assertTrue(success.asEmptyResult() is TkResult.Success)
    }
}
