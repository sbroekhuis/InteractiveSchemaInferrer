package app.strategy

import app.Const
import app.util.codearea
import app.util.highlight
import app.util.newObject
import app.util.richChanges
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.saasquatch.jsonschemainferrer.GenericSchemaFeature
import com.saasquatch.jsonschemainferrer.GenericSchemaFeatureInput
import com.saasquatch.jsonschemainferrer.SpecVersion
import javafx.geometry.Orientation
import javafx.scene.control.ButtonBar
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
class ConstDetection : Strategy(), GenericSchemaFeature {
    override fun getFeatureResult(input: GenericSchemaFeatureInput): ObjectNode? {
        if (input.specVersion < SpecVersion.DRAFT_06) {
            // Const exist only since draft 6
            return null
        }


        val samples: MutableCollection<out JsonNode> = input.samples
        val distinct = samples.distinct()
        val distinctSize = distinct.size

        // Return if not applicable
        if (distinctSize != 1) return null

        val value = distinct.first()
        var result = false
        println(input.schema)


        this.askUserWith("Inferring - Possible Constant Found") {
            form {
                fieldset {
                    field(
                        """
                    |The field with path: ${input.path} seems to have a single distinct value.
                    |Is this field an const?
                    """.trimMargin()
                    ) {
                        labelPosition = Orientation.VERTICAL
                        codearea(text = value.toPrettyString()) {
                            richChanges {
                                setStyleSpans(0, highlight(this@codearea.text))
                            }
                            fitToParentHeight()
                            isEditable = false

                            styleClass.add("text-area")
                        }
                        fitToParentHeight()
                    }
                }
                buttonbar {
                    button("Yes", ButtonBar.ButtonData.YES) {
                        action {
                            result = true
                            done()
                        }
                    }
                    button("No", ButtonBar.ButtonData.NO) {
                        action { this@askUserWith.done() }
                    }
                }
            }
        }
        if (!result) {
            // It was not a constant.
            return null
        }

        // It is a const
        val newObject: ObjectNode = newObject()
        newObject.set<ObjectNode>(Const.CONST, value)
        // Remove the existing type from the schema.
        input.schema.remove("type")
        return newObject
    }
}
