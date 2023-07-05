package app.interactiveschemainferrer.util

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConstraintTest {

    @Test
    fun `test no update`(){
        val constraint = Constraint<Int>()
        assertThrows<UninitializedPropertyAccessException> {
            assertNull(constraint.max)
        }
        assertThrows<UninitializedPropertyAccessException> {
            assertNull(constraint.min)
        }
    }

    @Test
    fun `no initial test`() {
        var constraint = Constraint<Int>()
        constraint.update(3)
        constraint.update(6)
        constraint.update(-4)
        assertEquals(-4, constraint.min)
        assertEquals(6, constraint.max)
    }
}
