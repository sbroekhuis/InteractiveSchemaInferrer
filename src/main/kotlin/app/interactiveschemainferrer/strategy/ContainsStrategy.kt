package app.interactiveschemainferrer.strategy

import app.interactiveschemainferrer.Const.Fields
import app.interactiveschemainferrer.Const.Types
import app.interactiveschemainferrer.util.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.saasquatch.jsonschemainferrer.GenericSchemaFeature
import com.saasquatch.jsonschemainferrer.GenericSchemaFeatureInput
import com.saasquatch.jsonschemainferrer.SpecVersion
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.scene.Parent
import javafx.scene.control.ButtonBar
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid
import tornadofx.*
import java.util.logging.Logger

/**
 * # Strategy: Contains
 * This strategy implements the [Contains](https://json-schema.org/understanding-json-schema/reference/array.html#contains)
 * keyword.
 *
 * This implements the contains keyword.
 * While the items schema must be valid for every item in the array,
 * the contains schema only needs to validate against one or more items in the array.
 *
 * This is done by checking if the current schema is for arrays and contains an `anyOf` as type.
 * When this is the case we validate for all samples each `anyOf` rule and check if they all contain it.
 *
 * If this is the case we ask the user if we should add a 'contains' to the rule
 *
 */
class ContainsStrategy : GenericSchemaFeature {

    companion object {
        private val logger: Logger by lazy { Logger.getLogger(ContainsStrategy::class.qualifiedName) }
    }
    class ContainsCondition(json: String, minValue: Int, maxValue: Int, minDisabled: Boolean, maxDisabled: Boolean) {
        val jsonString = SimpleStringProperty(json)
        val minValue = SimpleIntegerProperty(minValue)
        val maxValue = SimpleIntegerProperty(maxValue)
        val minDisabled = SimpleBooleanProperty(minDisabled)
        val maxDisabled = SimpleBooleanProperty(maxDisabled)
    }

    private fun getJsonSchema(schema: JsonNode, specVersion: SpecVersion): JsonSchema {
        val instance = JsonSchemaFactory.getInstance(specVersion.asNetworknt())
        return instance.getSchema(schema)
    }



    /**
     * Get the add-on result to be merged in with the schema
     */
    override fun getFeatureResult(input: GenericSchemaFeatureInput): ObjectNode? {
        val potentialContains = inferPotentialContains(input.schema, input.samples, input.type, input.specVersion)
        if (potentialContains.isEmpty()) {
            return null
        }
        logger.fine("Potential contains found")
        logger.finer(potentialContains.toString())


        val contains = convertToContains(potentialContains).asObservable()
        askUserWith("Inferring - Possible Contains Found", getForm(contains, input.specVersion, input.path))

        return when (contains.size) {
            0 -> {
                logger.fine("User declined potential contains condition.")
                null
            }

            1 -> {
                val condition = contains.first()
                makeContainsRule(condition)
            }

            else -> {
                objectNode {
                    replace("allOf", arrayNode(contains.map(::makeContainsRule)))
                }
            }
        }
    }

    /**
     * Converts a set of potential constraints to the TornadoFX models
     */
    private fun convertToContains(potentialConstraints: Set<Pair<IntConstrains, JsonNode>>): MutableList<ContainsCondition> {
        val result = mutableListOf<ContainsCondition>()
        for ((constraint, second) in potentialConstraints) {
            val (min, max) = constraint
            result += ContainsCondition(
                json = second.toString(),
                minValue = min,
                maxValue = max,
                minDisabled = false,
                maxDisabled = false
            )
        }
        return result;
    }

    private fun getForm(
        conditions: ObservableList<ContainsCondition>, specVersion: SpecVersion, path: String
    ): StrategyFragment.() -> Parent {
        return {
            val notAvailableInVersion = specVersion < SpecVersion.DRAFT_2019_09
            vbox(20) {
                text(
                    "The array with path: $path seems to always contain the following conditions.\n" +
                            "Should it have these contains conditions?",
                )
                form {
                    fieldset(text = "Conditions:") {
                        for (condition in conditions) {
                            hbox(spacing = 20) {
                                button {
                                    icon = fonticon(FontAwesomeSolid.TRASH)
                                    action {
                                        conditions.remove(condition)
                                    }
                                }
                                jsonarea(condition.jsonString)
                                vbox {
                                    field("Min:") {
                                        checkbox(property = condition.minDisabled) {
                                            this.isDisable = notAvailableInVersion
                                            if (notAvailableInVersion) tooltip("Not available in current schema version.")
                                        }
                                        spinner(property = condition.minValue, max = condition.minValue.get()) {
                                            disableProperty().bind(condition.maxDisabled)
                                        }
                                    }
                                    field("Max:") {
                                        checkbox(property = condition.maxDisabled) {
                                            this.isDisable = notAvailableInVersion
                                            if (notAvailableInVersion) tooltip("Not available in current schema version.")
                                        }
                                        spinner(property = condition.maxValue, min = condition.maxValue.get()) {
                                            disableProperty().bind(condition.maxDisabled)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    buttonbar {
                        button("Yes", ButtonBar.ButtonData.YES) {
                            enableWhen(conditions.sizeProperty.gt(0))
                            action {
                                done()
                            }
                        }
                        button("No", ButtonBar.ButtonData.NO) {
                            action {
                                conditions.clear()
                                done()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun makeContainsRule(condition: ContainsCondition): ObjectNode {
        return objectNode {
            this.replace("contains", condition.jsonString.get().asJson())
            if (condition.maxDisabled.get()) {
                this.put("maxContains", condition.maxValue.get())
            }
            if (condition.minDisabled.get()) {
                this.put("minContains", condition.minValue.get())
            }
        }
    }

    /**
     * Infer potential contains from a set of examples and a schema.
     */
    fun inferPotentialContains(
        schema: ObjectNode, samples: Collection<JsonNode>, type: String?, specVersion: SpecVersion
    ): Set<Pair<IntConstrains, JsonNode>> {
        if (specVersion < SpecVersion.DRAFT_06) {
            logger.fine("Not an array input, thus we cannot infer contains")
            return emptySet()
        }
        if (type != Types.ARRAY) {
            logger.fine("Not an array input, thus we cannot infer contains")
            return emptySet()
        }
        if (schema["items"]?.get(Fields.ANY_OF)?.isArray != true) {
            logger.fine("Array does not have an any-of type, thus a contains would not make sense.")
            return emptySet()
        } else if (!samples.all { it.isArray }) {
            logger.warning("Found a non-array sample in generic feature, the sample seems to be invalid.")
            return emptySet()
        }
        val anyOfs: List<JsonNode> = schema["items"][Fields.ANY_OF].elements().asSequence().toList()


        // Set of Min/Max/Schema
        val result = mutableSetOf<Pair<IntConstrains, JsonNode>>()

        for (anyOf in anyOfs) {
            val jsonSchema = getJsonSchema(anyOf, specVersion)
            val constrains = IntConstrains()

            for (sample in samples) {
                // Update the constraint
                constrains += sample.filter {
                    jsonSchema.validate(it).isEmpty()
                }.size
            }
            if (constrains.min > 0) {
                // At least one is always present
                result += constrains to anyOf
            }
        }
        return result
    }
}
