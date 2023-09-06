package com.github.sbroekhuis.interactiveschemainferrer.strategy

import kotlin.jvm.optionals.getOrElse
import kotlin.test.Test
import kotlin.test.fail


class DefaultStrategyTest {

    @Test
    fun `simple outlier test`() {
        val input: List<Int> = (0..80).map { 7 }.plus((0..20))
        val outliers = DefaultStrategy().detectDefaultInList(input, 0.8)
        val expected = 7
        kotlin.test.assertEquals(expected, outliers.getOrElse { fail("Missing Expected Default") })

    }
}
