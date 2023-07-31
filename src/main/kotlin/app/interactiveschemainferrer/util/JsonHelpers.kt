package app.interactiveschemainferrer.util

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.saasquatch.jsonschemainferrer.SpecVersion
import javafx.collections.ObservableList
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import org.intellij.lang.annotations.Language
import tornadofx.*
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Simple Helper Function for converting a list of files to list of json nodes.
 */
internal fun convertFilesToJson(files: List<File>, isArrayExamples: Boolean): ObservableList<JsonNode> {
    // TODO: Move this to a helper class?
    val objectMapper = ObjectMapper()
    val mapped = files.mapNotNull {
        try {
            objectMapper.readTree(it)
        } catch (e: MismatchedInputException) {
            // TODO: Better logging system
            print(e.message)
            null
        } catch (e: IOException) {
            print(e.message)
            null
        }
    }
    if (isArrayExamples) {
        val arrays = mutableListOf<JsonNode>()
        for (jsonNode in mapped) {
            if (jsonNode is ArrayNode) {
                arrays.addAll(jsonNode.elements().asSequence())
            }
        }
        return arrays.asObservable()
    }
    return mapped.toMutableList().asObservable()
}

/**
 * Highlighter for JSON strings.
 */
fun highlightJSON(json: String): StyleSpans<Collection<String>>? {
    val parser: JsonParser = JsonFactory().createParser(json)
    val spansBuilder = StyleSpansBuilder<Collection<String>>()

    var lastPos = 0
    try {
        while (true) {
            //Break check in case the isClosed does not work...
            val jsonToken: JsonToken = parser.nextToken() ?: break
            var length: Int = parser.textLength

            // Because getTextLength() does contain the surrounding ""
            if (jsonToken == JsonToken.VALUE_STRING || jsonToken == JsonToken.FIELD_NAME) {
                length += 2
            }
            // Get classname for styling
            val className = jsonTokenToClassName(jsonToken)

            if (className.isNotEmpty()) {
                val start = parser.tokenLocation.charOffset.toInt()
                // Fill the gaps, since Style Spans need to be contiguous.
                if (start > lastPos) {
                    val noStyleLength: Int = start - lastPos
                    spansBuilder.add(Collections.emptyList(), noStyleLength)
                }
                lastPos = start + length
                spansBuilder.add(Collections.singleton(className), length)
            }
        }

    } catch (e: IOException) {
        // Ignoring JSON parsing exception in the context of
        // syntax highlighting
    }
    if (lastPos == 0) {
        spansBuilder.add(Collections.emptyList(), json.length)
    }
    return spansBuilder.create()
}

fun isValidJSON(json: String?): Boolean {
    try {
        val parser = JsonFactory().createParser(json)
        while (parser.nextToken() != null) {
            continue
        }
    } catch (jpe: JsonParseException) {
        return false
    } catch (ioe: IOException) {
        return false
    }
    return true
}

fun objectNode(op: (ObjectNode.() -> Unit) = {}): ObjectNode {
    return JsonNodeFactory.instance.objectNode().apply(op)
}

fun arrayNode(children: Collection<JsonNode> = emptyList(), op: (ArrayNode.() -> Unit) = {}): ArrayNode {
    return JsonNodeFactory.instance.arrayNode().addAll(children).apply(op)
}

private fun jsonTokenToClassName(jsonToken: JsonToken): String = when (jsonToken) {
    JsonToken.FIELD_NAME -> "json-property"
    JsonToken.VALUE_STRING -> "json-string"
    JsonToken.VALUE_NUMBER_FLOAT, JsonToken.VALUE_NUMBER_INT -> "json-number"
    JsonToken.VALUE_TRUE, JsonToken.VALUE_FALSE -> "json-boolean"
    else -> ""
}

fun @receiver:Language("JSON") String.asJson(): JsonNode = jacksonObjectMapper().readTree(this)

fun makeSchema(schema: JsonNode, specVersion: SpecVersion): JsonSchema {
    val instance = JsonSchemaFactory.getInstance(specVersion.asNetworknt())
    return instance.getSchema(schema)
}

// JsonPath Functions

private val PATH_CONFIG = Configuration.builder().jsonProvider(JacksonJsonNodeJsonProvider()).build()

fun JsonNode.at(jsonPath: JsonPath): JsonNode {
    return JsonPath.using(PATH_CONFIG).parse(this).read(jsonPath)
}

fun ObjectNode.put(field: String, number: Number) {
    when(number) {
        is Int -> this.put(field, number)
        is Double -> this.put(field, number)
        is Float -> this.put(field, number)
        is Long -> this.put(field, number)
        else -> kotlin.error("Missing Implementation Number jackson put")
    }
}
