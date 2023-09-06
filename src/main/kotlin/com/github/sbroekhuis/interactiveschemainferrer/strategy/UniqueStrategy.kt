package com.github.sbroekhuis.interactiveschemainferrer.strategy

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.sbroekhuis.interactiveschemainferrer.Const
import com.github.sbroekhuis.interactiveschemainferrer.util.*
import com.saasquatch.jsonschemainferrer.GenericSchemaFeature
import com.saasquatch.jsonschemainferrer.GenericSchemaFeatureInput
import com.saasquatch.jsonschemainferrer.SpecVersion
import javafx.scene.control.ButtonBar
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import tornadofx.*

/**
 * # Strategy: Unique
 * This strategy implements the [Unique]()
 * keyword.
 *
 * We do this by checking all the values in an array and check they always, never contain the same.
 */
class UniqueStrategy : GenericSchemaFeature, AbstractStrategy() {

    override fun getFeatureResult(input: GenericSchemaFeatureInput): ObjectNode? {
        val (_: ObjectNode,
            samples: MutableCollection<out JsonNode>,
            type: String?,
            _: SpecVersion,
            path: String) = input

        if (type != Const.Types.ARRAY) {
            logger.fine("Not an array, skipping")
            return null
        }

        val isUnique = samples.all { sample ->
            if (!sample.isArray) {
                false
            } else {
                sample.size() == (sample as ArrayNode).distinct().size
            }
        }
        if (!isUnique) {
            logger.fine("Array is not always unique, skipping")
            return null
        }
        val userWantsUnique = askUserWith(UniqueForm(path))

        if (!userWantsUnique) {
            logger.info("User skipped uniqueItems condition.")
            return null
        }

        logger.info("User accepted uniqueItems condition.")
        return objectNode {
            this.put(Const.Fields.UNIQUE_ITEMS, true)
        }
    }

    class UniqueForm(val path: String) : StrategyFragment<Boolean>("Inferring - Possible Unique Found") {
        override val root = strategyroot("https://json-schema.org/understanding-json-schema/reference/array.html#uniqueness") {
            paddingAll = 20.0

            // Add the multiline description label
            label {
                graphic = textflow {
                    text("The array with the path: ")
                    text(path) { font = Font.font("Monospace") }
                    text(" never contains the same value twice.")
                    text("\n")
                    text("Should this field be marked as `uniqueItems`?")
                }
                isWrapText = true
            }
            separator()

            // Button Bar
            region { vgrow = Priority.ALWAYS }
            buttonbar {
                button("Yes", ButtonBar.ButtonData.YES) {
                    action {
                        done(true)
                    }
                }
                button("No", ButtonBar.ButtonData.NO) {
                    action {
                        done(false)
                    }
                }
            }
        }
    }
}
