package app.interactiveschemainferrer.strategy

import app.interactiveschemainferrer.util.asJson
import app.interactiveschemainferrer.util.fonticon
import app.interactiveschemainferrer.util.jsonarea
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.saasquatch.jsonschemainferrer.EnumExtractor
import com.saasquatch.jsonschemainferrer.EnumExtractorInput
import javafx.geometry.Pos
import javafx.scene.control.ButtonBar
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid
import tornadofx.*

/**
 * # Strategy: EnumDetection
 * This strategy implements the [Enum](https://json-schema.org/understanding-json-schema/reference/generic.html?highlight=enum#enumerated-values)
 *
 * This is done by detecting from all the input samples if
 * there is only a small number of distinct values.
 * We divide this difference and compare it to a the [EnumStrategy.THRESHOLD].
 *
 */
class EnumStrategy : EnumExtractor, AbstractStrategy() {

    companion object {
        /** Threshold to specify when the difference between distinct and total is substantial */
        var THRESHOLD: Float = 0.2f
    }

    private fun inferPotentialEnums(samples: List<JsonNode>): List<JsonNode>? {
        val totalSize = samples.size.toFloat()
        val distinct = samples.distinct()
        val distinctSize = distinct.size

        if (distinctSize <= 1) {
            // If distinctSize is 1, then this is not an enum but a const
            // See ConstDetection
            logger.fine("Distinct Size <=1, nothing to infer.")
            return null
        }

        val fl = distinctSize.div(totalSize)
        logger.finer("Distinct size ($distinctSize) / total size ($totalSize) (=$fl)")
        if (fl > THRESHOLD) {
            logger.fine("No potential enum found!")
            return null
        }
        return distinct
    }

    private fun extractEnumPerType(samples : List<JsonNode>, path: String): List<JsonNode>? {
        // TODO: If some field happens again, perhaps it is the same enum.
        //  How can we implement this? Definitions?
        val potentialEnums: List<JsonNode> = inferPotentialEnums(samples) ?: return null
        logger.fine("Potential enums found")
        logger.finer(potentialEnums.toString())

        val result = askUserWith(Form(potentialEnums, path))
        if (result.isNullOrEmpty()) {
            logger.fine("User declined potential enum.")
            return null
        }
        logger.fine("User accepted enum.")

        return result.toMutableList()
    }

    /**
     * @return The *group* of enums. Note that each group is expected to be not null and not
     * empty. All the elements in each group are expected to come directly from the given
     * samples if possible to ensure [JsonNode.equals] works correctly.
     */
    override fun extractEnums(input: EnumExtractorInput): List<List<JsonNode>> {
        logger.info(input.path)
        val typeListMap: Map<JsonNodeType, List<JsonNode>> = input.samples.filterNotNull().groupBy {
            it.nodeType
        }
        val result = mutableListOf<List<JsonNode>?>()
        for ((type, value) in typeListMap) {
            if (type == JsonNodeType.BOOLEAN) {
                // Ignore booleans, since they are enums of True/False
                continue
            }
            result.add(extractEnumPerType(value, input.path))
        }
        return result.filterNotNull();
    }

    private class Form(potentialEnums: List<JsonNode>, val path: String) :
        StrategyFragment<List<JsonNode>?>("Inferring - Possible Enum Found") {

        val enumValues = potentialEnums.map { it.toPrettyString().toProperty() }.asObservable()

        override val root = strategyroot("https://json-schema.org/understanding-json-schema/reference/generic.html#enumerated-values") {
            paddingAll = 20.0

            // Add the multiline description label
            label {
                graphic = textflow {
                    text("The field with path: ")
                    text(path) { font = Font.font("Monospace") }
                    text(" seems to have a relative small amount distinct values.")
                    text("\n")
                    text("Is this field an enum?")
                }
                isWrapText = true
            }

            separator()
            // Body
            listview(enumValues) {
                cellFormat { value ->
                    this.graphic =
                        hbox(spacing = 20) {
                            button {
                                this.alignment = Pos.CENTER
                                this.graphic = fonticon(FontAwesomeSolid.TRASH)
                                action {
                                    enumValues.remove(value)
                                }
                            }
                            jsonarea(property = value, validator = validator) {
                                hgrow = Priority.ALWAYS
                                minHeight = 50.0
                            }
                        }

                }
            }

            separator()
            // Button Bar
            region { vgrow = Priority.ALWAYS }
            buttonbar {
                button("Yes", ButtonBar.ButtonData.YES) {
                    enableWhen(this@Form.validator.valid)
                    action {
                        done(enumValues.map {
                            it.get().asJson()
                        })
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
