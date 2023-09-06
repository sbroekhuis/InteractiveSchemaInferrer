package com.github.sbroekhuis.interactiveschemainferrer.util

import com.fasterxml.jackson.databind.node.TextNode
import com.jayway.jsonpath.JsonPath
import kotlin.test.Test
import kotlin.test.assertEquals

class UtilKtTest {


    @Test
    fun `test searching json path`() {
        val json = """
            {
                "root" : {
                  "string" : "c",
                  "num" : 3,
                  "array" : [
                      {"within" : false}
                  ]
                } 
            }
        """.asJson()

        assertEquals(TextNode("c"), json.at(JsonPath.compile("$[\"root\"][\"string\"]")))
        assertEquals("[false]".asJson(), json.at(JsonPath.compile("$[\"root\"][\"array\"][*][\"within\"]")))

    }
}
