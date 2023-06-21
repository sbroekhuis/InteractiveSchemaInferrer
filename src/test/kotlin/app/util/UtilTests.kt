package app.util

import app.interactiveschemainferrer.util.frequencies
import kotlin.test.Test
import kotlin.test.assertEquals


class UtilTests {

    @Test
    fun `test if frequencies works correctly`() {
        val input = listOf(1, 2, 3, 4, 1, 1, 1, 3)
        val expected = buildMap {
            this[1] = 4
            this[2] = 1
            this[3] = 2
            this[4] = 1
        }
        assertEquals(expected, input.frequencies())
    }


}
