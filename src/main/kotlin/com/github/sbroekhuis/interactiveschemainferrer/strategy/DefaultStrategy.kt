package com.github.sbroekhuis.interactiveschemainferrer.strategy

import com.fasterxml.jackson.databind.JsonNode
import com.github.sbroekhuis.interactiveschemainferrer.util.asJson
import com.github.sbroekhuis.interactiveschemainferrer.util.frequencies
import com.github.sbroekhuis.interactiveschemainferrer.util.jsonarea
import com.saasquatch.jsonschemainferrer.DefaultPolicy
import com.saasquatch.jsonschemainferrer.GenericSchemaFeatureInput
import javafx.scene.control.ButtonBar
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import tornadofx.*
import java.util.*

/**
 * # Strategy: Default / Examples
 * This strategy implements the [Default](https://json-schema.org/understanding-json-schema/reference/generic.html#annotations)
 * keyword.
 *
 * This implements
 *
 */
class DefaultStrategy : DefaultPolicy, AbstractStrategy() {

    /**
     * Detect outliers based on the count of each distinct value in the collection.
     */
    fun <T : Any> detectDefaultInList(values: Collection<T>, threshold: Double = 0.80): Optional<T> {
        val frequencies = values.frequencies()
        val max: Map.Entry<T, Int> = frequencies.maxBy { e -> e.value }
        val coverage = max.value.toDouble() / values.size.toDouble()

        return if (coverage > threshold && coverage != 1.0) Optional.of(max.key) else Optional.empty<T>()
    }


    /**
     * Get the default based on the count of distinct values in the samples using [detectDefaultInList] function.
     */
    override fun getDefault(input: GenericSchemaFeatureInput): JsonNode? {
        val optional = detectDefaultInList(input.samples)
        if (optional.isEmpty) {
            logger.fine("No default empty, nothing to infer")
            return null
        }
        val default = optional.get()

        // Examples does not yet exist, use default instead

        logger.fine("Potential default found.")
        logger.finer(default.toPrettyString())

        val result = askUserWith(Form(default, input.path))

        if (result == null) {
            logger.info("User declined potential default.")
            // User does not want default
            return null
        }

        logger.fine("User accepted default.")
        logger.finer(result.toPrettyString())
        return result
    }


    class Form(potentialDefault: JsonNode, val path: String) :
        StrategyFragment<JsonNode?>("Inferring - Possible Default Found") {

        private val defaultProperty = potentialDefault.toPrettyString().toProperty()

        // I do not know how to remove duplicate code here.
        // All strategies have the same structure.
        @Suppress("DuplicatedCode")
        override val root = strategyroot("https://json-schema.org/understanding-json-schema/reference/generic.html#comments") {
            paddingAll = 20.0

            // Add the multiline description label
            label {
                graphic = textflow {
                    text("The field with the path: ")
                    text(path) { font = Font.font("Monospace") }
                    text(" seems to have a common value.")
                    text("\n")
                    text("Should this be the default?")
                }
                isWrapText = true
            }

            separator()
            // Body
            jsonarea(property = defaultProperty, validator) {
                hgrow = Priority.ALWAYS
                minHeight = 50.0
            }
            separator()
            // Button Bar
            region { vgrow = Priority.ALWAYS }
            buttonbar {
                button("Yes", ButtonBar.ButtonData.YES) {
                    enableWhen(validator.valid)
                    action {
                        done(defaultProperty.get().asJson())
                    }
                }
                button("No", ButtonBar.ButtonData.NO) {
                    action {
                        done(null)
                    }
                }
            }
        }
    }

}
