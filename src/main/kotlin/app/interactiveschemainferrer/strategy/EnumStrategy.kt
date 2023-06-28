package app.interactiveschemainferrer.strategy

import app.interactiveschemainferrer.util.asJson
import app.interactiveschemainferrer.util.fonticon
import app.interactiveschemainferrer.util.jsonarea
import com.fasterxml.jackson.databind.JsonNode
import com.saasquatch.jsonschemainferrer.EnumExtractor
import com.saasquatch.jsonschemainferrer.EnumExtractorInput
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.scene.Parent
import javafx.scene.control.ButtonBar
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid
import tornadofx.*
import java.util.*
import java.util.logging.Logger

/**
 * # Strategy: EnumDetection
 * This strategy implements the [Enum](https://json-schema.org/understanding-json-schema/reference/generic.html?highlight=enum#enumerated-values)
 *
 * This is done by detecting from all the input samples if
 * there is only a small number of distinct values.
 * We divide this difference and compare it to a the [EnumStrategy.THRESHOLD].
 *
 */
class EnumStrategy : EnumExtractor {

    companion object {
        private val logger: Logger by lazy { Logger.getLogger(EnumStrategy::class.qualifiedName) }

        /** Threshold to specify when the difference between distinct and total is substantial */
        var THRESHOLD: Float = 0.1f
    }

    private class EnumCondition(json: String) {
        val jsonString = SimpleStringProperty(json)
    }

    private fun inferPotentialEnums(samples: MutableCollection<out JsonNode>): List<JsonNode>? {
        val distinct = samples.distinct()
        val distinctSize = distinct.size
        val totalSize = samples.size.toFloat()

        if (distinctSize == 1) {
            // If distinctSize is 1, then this is not an enum but a const
            // See ConstDetection
            logger.fine("Distinct Size 1, nothing to infer")
            return null
        }

        val fl = distinctSize.div(totalSize)
        if (fl > THRESHOLD) {
            logger.fine("Distinct size / total size ($fl) = lower than threshold$THRESHOLD no potential enum found")
            return null
        }
        return distinct
    }

    /**
     * @return The *group* of enums. Note that each group is expected to be not null and not
     * empty. All the elements in each group are expected to come directly from the given
     * samples if possible to ensure [JsonNode.equals] works correctly.
     */
    override fun extractEnums(input: EnumExtractorInput): MutableCollection<MutableCollection<out JsonNode>> {
        // TODO: If some field happens again, perhaps it is the same enum.
        //  How can we implement this? Definitions?
        val potentialEnums: List<JsonNode> = inferPotentialEnums(input.samples) ?: return Collections.emptySet()
        logger.fine("Potential enums found")
        logger.finer(potentialEnums.toString())

        val validator = ValidationContext()
        val enums = preProcessEnums(potentialEnums).asObservable()

        askUserWith("Inferring - Possible Enum Found", getForm(validator, enums, input.path))
        if (enums.isEmpty()) {
            logger.fine("User declined potential enum.")
            return Collections.emptySet()
        }

        val result = enums.map { it.jsonString.get().asJson() }.distinct().toMutableList()
        logger.fine("User accepted enum.")

        return Collections.singleton(result)
    }

    private fun getForm(
        validator: ValidationContext,
        enums: ObservableList<EnumCondition>,
        path: String
    ): StrategyFragment.() -> Parent {
        return {
            vbox(20) {
                text(
                    """
                        |The field with path: $path seems to have a relative small amount distinct values.
                        |Is this field an enum?
                        """.trimMargin(),
                )
                form {
                    fieldset(text = "Current enum values:") {
                        for (enum in enums) {
                            hbox(spacing = 20) {
                                button {
                                    icon = fonticon(FontAwesomeSolid.TRASH)
                                    action {
                                        enums.remove(enum)
                                    }
                                }
                                jsonarea(enum.jsonString, validator = validator)
                            }
                        }
                        button("Add") {
                            enums.add(EnumCondition(""))
                        }
                    }
                    buttonbar {
                        button("Yes", ButtonBar.ButtonData.YES) {
                            enableWhen(validator.valid.and(enums.sizeProperty.gt(0)))
                            action {
                                done()
                            }
                        }
                        button("No", ButtonBar.ButtonData.NO) {
                            action {
                                enums.clear()
                                done()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun preProcessEnums(potentialEnums: List<JsonNode>): MutableList<EnumCondition> {
        val result = mutableListOf<EnumCondition>()
        for (potentialEnum in potentialEnums) {
            result += EnumCondition(potentialEnum.toPrettyString())
        }
        return result;
    }
}
