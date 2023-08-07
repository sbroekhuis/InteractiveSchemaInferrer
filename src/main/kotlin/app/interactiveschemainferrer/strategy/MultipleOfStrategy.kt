package app.interactiveschemainferrer.strategy

import com.fasterxml.jackson.databind.JsonNode
import com.saasquatch.jsonschemainferrer.GenericSchemaFeatureInput
import com.saasquatch.jsonschemainferrer.MultipleOfPolicies
import com.saasquatch.jsonschemainferrer.MultipleOfPolicy
import javafx.scene.control.ButtonBar
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import tornadofx.*

/**
 * # Strategy: MultipleOfStrategy
 * This strategy implements the [MultipleOf](https://json-schema.org/understanding-json-schema/reference/numeric.html#multiples)
 * keyword.
 *
 * We use the [MultipleOfPolicies.gcd] to get the greatest common divider, and if it is not 1, we ask the user to confirm.
 *
 */
class MultipleOfStrategy : AbstractStrategy(), MultipleOfPolicy {

    override fun getMultipleOf(input: GenericSchemaFeatureInput): JsonNode? {
        val result: JsonNode = MultipleOfPolicies.gcd().getMultipleOf(input) ?: return null
        val multipleOf: Number = result.numberValue()
        if (multipleOf == 1) {
            logger.info("The GCD is 1, therefor we should not ask anything, ignore it")
            return null
        }
        val response = askUserWith(Form(input.path, multipleOf))
        return if (response) {
            logger.info("Potential multipleOf accepted")
            result
        } else {
            logger.info("Potential multipleOf declined")
            null
        }
    }

    private class Form(val path: String, val multipleOf: Number) :
        StrategyFragment<Boolean>("Inferring - Possible MultipleOf Found") {

        override val root = strategyroot("https://json-schema.org/understanding-json-schema/reference/numeric.html#multiples") {

            // Add the multiline description label
            label {
                graphic = textflow {
                    text("The field with path: ")
                    text(path) { font = Font.font("Monospace") }
                    text(" seems to be a number with a greatest common divider of ")
                    text(multipleOf.toString()) { font = Font.font("Monospace") }
                    text(".\nShould we add this constraint?")
                }
                isWrapText = true
            }
            separator()
            // Button Bar
            region { vgrow = Priority.ALWAYS }
            buttonbar {
                button("Yes", ButtonBar.ButtonData.YES) {
                    enableWhen(validator.valid)
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
