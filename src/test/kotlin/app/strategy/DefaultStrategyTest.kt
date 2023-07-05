package app.strategy

import app.interactiveschemainferrer.strategy.DefaultStrategy
import kotlin.test.Test


class DefaultStrategyTest {

    @Test
    fun `simple outlier test`() {
        val input: List<Int> = listOf(7, 7, 7, 1, 3, 7, 7, 3, 4, 5, 5, 9, 7, 7)
        val outliers = DefaultStrategy().outliers(input)
        val expected = 7
        kotlin.test.assertEquals(mapOf(expected to expected), outliers)

    }
}
