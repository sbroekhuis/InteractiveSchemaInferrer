package app.interactiveschemainferrer.strategy

import app.interactiveschemainferrer.util.IntConstrains
import app.interactiveschemainferrer.util.asJson
import com.fasterxml.jackson.databind.node.ObjectNode
import com.saasquatch.jsonschemainferrer.SpecVersion
import org.intellij.lang.annotations.Language
import kotlin.test.Test
import kotlin.test.assertEquals

class ContainsStrategyTest {


    companion object {
        //region companion
        @Language("JSON")
        private val schema =
            """{
  "type": "array",
  "items": {
    "anyOf": [
      {
        "enum": [
          "APPLE",
          "BANANA"
        ]
      },
      {
        "type": "integer"
      },
      {
        "type": "array",
        "items": {
          "type": "integer"
        }
      }
    ]
  }
}""".asJson() as ObjectNode
        //endregion
    }

    @Test
    fun `test unsupported schema version`() {
        TODO("Not implemented")
    }


    @Test
    fun `test simple array`() {
        val containsStrategy = ContainsStrategy()
        val samples = listOf("""["BANANA",2,[3,5]]""", """["APPLE",2, 4]""").map(String::asJson)
        val actual = containsStrategy.inferPotentialContains(
            schema,
            samples,
            "array",
            SpecVersion.DRAFT_06
        )
        val expected = setOf(
            IntConstrains().apply { update(1) } to schema["items"]["anyOf"][0],
            IntConstrains().apply { update(1); update(2) } to schema["items"]["anyOf"][1],
        )
        assertEquals(expected, actual)
    }
}
