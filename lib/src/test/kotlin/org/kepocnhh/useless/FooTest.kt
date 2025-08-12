package org.kepocnhh.useless

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FooTest {
    @Test
    fun getNumberTest() {
        val expected = 1
        val actual = Foo.getNumber()
        assertEquals(expected, actual)
    }
}
