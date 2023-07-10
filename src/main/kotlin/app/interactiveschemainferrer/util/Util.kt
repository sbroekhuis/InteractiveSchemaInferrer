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
import com.saasquatch.jsonschemainferrer.*
import javafx.beans.property.StringProperty
import javafx.collections.ObservableList
import javafx.event.EventTarget
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.model.RichTextChange
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import org.intellij.lang.annotations.Language
import org.kordamp.ikonli.Ikon
import org.kordamp.ikonli.javafx.FontIcon
import org.reactfx.EventStream
import tornadofx.*
import java.io.File
import java.io.IOException
import java.util.*
import com.networknt.schema.SpecVersion as SpecVersionNetwork
import com.saasquatch.jsonschemainferrer.SpecVersion as SpecVersionSaas


@Suppress("SpellCheckingInspection")
fun EventTarget.fonticon(iconCode: Ikon? = null, op: FontIcon.() -> Unit = {}) = FontIcon().attachTo(this, op) {
    if (iconCode != null) it.iconCode = iconCode
}

@Suppress("SpellCheckingInspection")
fun EventTarget.codearea(text: String, op: CodeArea.() -> Unit = {}) = opcr(this, CodeArea(text), op)


fun CodeArea.richChanges(op: EventStream<RichTextChange<MutableCollection<String>, String, MutableCollection<String>>>.() -> Unit = {}): EventStream<RichTextChange<MutableCollection<String>, String, MutableCollection<String>>> =
    this.richChanges().apply(op)


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

/**
 * Return a Map of each distinct value to the count of that value in the original list.
 */
internal fun <T : Any> Iterable<T?>.frequencies(): Map<T, Int> {
    return this.filterNotNull().groupingBy { it }.eachCount()
}


@Suppress("DuplicatedCode")
fun EventTarget.jsonarea(
    property: StringProperty,
    validator: ValidationContext? = null,
    op: CodeArea.() -> Unit = {}
) = opcr(this, CodeArea(property.get()), op).apply {
    property.stringBinding(this.textProperty()) { it }
    textProperty().onChange {
        setStyleSpans(0, highlightJSON(it ?: ""))
    }
    setStyleSpans(0, highlightJSON(text))
    style += "-fx-padding: 10px;"
    validator?.addValidator(this, this.textProperty()) {
        if (!isValidJSON(it)) {
            error("Invalid JSON")
        } else {
            null
        }
    }
}

@Suppress("DuplicatedCode")
fun EventTarget.jsonarea(
    text: String,
    validator: ValidationContext? = null,
    op: CodeArea.() -> Unit = {}
) = opcr(this, CodeArea(text), op).apply {
    textProperty().onChange {
        setStyleSpans(0, highlightJSON(it ?: ""))
    }
    setStyleSpans(0, highlightJSON(text))
    style += "-fx-padding: 10px;"
    validator?.addValidator(this, this.textProperty()) {
        if (!isValidJSON(it)) {
            error("Invalid JSON")
        } else {
            null
        }
    }
}

fun JsonSchemaInferrerBuilder.addStrategy(s: ExamplesPolicy) = this.setExamplesPolicy(s)
fun JsonSchemaInferrerBuilder.addStrategy(s: DefaultPolicy) = this.setDefaultPolicy(s)
fun JsonSchemaInferrerBuilder.addStrategy(s: GenericSchemaFeature) = this.addGenericSchemaFeatures(s)
fun JsonSchemaInferrerBuilder.addStrategy(s: EnumExtractor) = this.addEnumExtractors(s)
fun JsonSchemaInferrerBuilder.addStrategy(s: FormatInferrer) = this.addFormatInferrers(s)

fun ObjectNode.putArray(s: String, op: ArrayNode.() -> Unit) = this.putArray(s).apply(op)
fun ObjectNode.putObject(s: String, op: ObjectNode.() -> Unit) = this.putObject(s).apply(op)


fun SpecVersionNetwork.VersionFlag.asSaasquatch() = when (this) {
    SpecVersionNetwork.VersionFlag.V201909 -> SpecVersionSaas.DRAFT_2019_09
    SpecVersionNetwork.VersionFlag.V4 -> SpecVersionSaas.DRAFT_04
    SpecVersionNetwork.VersionFlag.V6 -> SpecVersionSaas.DRAFT_06
    SpecVersionNetwork.VersionFlag.V7 -> SpecVersionSaas.DRAFT_07
    SpecVersionNetwork.VersionFlag.V202012 -> SpecVersionSaas.DRAFT_2020_12
}

fun SpecVersionSaas.asNetworknt() = when (this) {
    SpecVersionSaas.DRAFT_2019_09 -> SpecVersionNetwork.VersionFlag.V201909
    SpecVersionSaas.DRAFT_04 -> SpecVersionNetwork.VersionFlag.V4
    SpecVersionSaas.DRAFT_06 -> SpecVersionNetwork.VersionFlag.V6
    SpecVersionSaas.DRAFT_07 -> SpecVersionNetwork.VersionFlag.V7
    SpecVersionSaas.DRAFT_2020_12 -> SpecVersionNetwork.VersionFlag.V202012
}

operator fun GenericSchemaFeatureInput.component1() = this.schema
operator fun GenericSchemaFeatureInput.component2(): MutableCollection<out JsonNode> = this.samples
operator fun GenericSchemaFeatureInput.component3() = this.type
operator fun GenericSchemaFeatureInput.component4() = this.specVersion
operator fun GenericSchemaFeatureInput.component5() = this.path


fun @receiver:Language("JSON") String.asJson() = jacksonObjectMapper().readTree(this)

fun <T> T?.optional() = Optional.ofNullable(this)
