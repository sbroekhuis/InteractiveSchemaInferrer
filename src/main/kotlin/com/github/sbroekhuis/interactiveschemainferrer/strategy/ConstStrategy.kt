package com.github.sbroekhuis.interactiveschemainferrer.strategy

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.sbroekhuis.interactiveschemainferrer.Const
import com.github.sbroekhuis.interactiveschemainferrer.util.asJson
import com.github.sbroekhuis.interactiveschemainferrer.util.jsonarea
import com.github.sbroekhuis.interactiveschemainferrer.util.objectNode
import com.saasquatch.jsonschemainferrer.GenericSchemaFeature
import com.saasquatch.jsonschemainferrer.GenericSchemaFeatureInput
import com.saasquatch.jsonschemainferrer.SpecVersion
import javafx.scene.control.ButtonBar
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import tornadofx.*

/**
 * # Strategy: ConstDetection
 * This strategy implements the [Const](https://json-schema.org/understanding-json-schema/reference/generic.html#constant-values)
 * keyword.
 *
 * This is the simplest strategy, we detect if from all the input
 * samples there is only one distinct sample. We then ask if the value is a const.
 *
 */
class ConstStrategy : GenericSchemaFeature, AbstractStrategy() {

    /**
     * Infer a constant from samples.
     * If there is only one distinct value, it is a const.
     */
    fun inferConst(samples: MutableCollection<out JsonNode>): JsonNode? {
        val distinct = samples.distinct()
        val distinctSize = distinct.size

        // Return if not applicable
        if (distinctSize != 1) {
            logger.fine("Not exactly one distinct value. Thus, not a constant.")
            return null
        }


        val value = distinct.first()
        logger.fine("Possible constant found")
        logger.finer(value.toPrettyString())
        return value
    }

    override fun getFeatureResult(input: GenericSchemaFeatureInput): ObjectNode? {
        if (input.specVersion < SpecVersion.DRAFT_06) {
            // Const exist only since draft 6
            logger.fine("Const not available in version: ${input.specVersion}")
            return null
        }
        if (input.samples.size == 1) {
            logger.fine("Const for sample size 1 disabled.")
            return null
        }
        if (input.type == Const.Types.NULL){
            logger.fine("Const ingore null types, as this is const by nature.")
            return null
        }

        val potentialConst = inferConst(input.samples) ?: return null


        val result = askUserWith(Form(input.path, potentialConst))

        if (result == null) {
            // It was not a constant.
            logger.fine("User declined potential constant.")
            return null
        }
        logger.fine("User accepted potential constant.")

        // Remove the existing type from the schema.
        input.schema.remove("type")

        // It is a const
        return objectNode {
            replace(Const.Fields.CONST, result)
        }
    }

    private class Form(val path: String, potentialConst: JsonNode) :
        StrategyFragment<JsonNode?>("Inferring - Possible Constant Found") {


        private val constProperty = potentialConst.toPrettyString().toProperty()

        // I do not know how to remove duplicate code here.
        // All strategies have the same structure.
        @Suppress("DuplicatedCode")
        override val root = strategyroot("https://json-schema.org/understanding-json-schema/reference/generic.html#constant-values") {

            // Add the multiline description label
            label {
                graphic = textflow {
                    text("The field with path: ")
                    text(path) { font = Font.font("Monospace") }
                    text(" seems to have a single distinct value.")
                    text("\n")
                    text("Is this field an const?")
                }
                isWrapText = true
            }

            separator()
            // Body
            jsonarea(property = constProperty, validator) {
                this.hgrow = Priority.ALWAYS
                minHeight = 50.0
            }

            separator()
            // Button Bar
            region { vgrow = Priority.ALWAYS }
            buttonbar {
                button("Yes", ButtonBar.ButtonData.YES) {
                    enableWhen(validator.valid)
                    action {
                        done(constProperty.get().asJson())
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
