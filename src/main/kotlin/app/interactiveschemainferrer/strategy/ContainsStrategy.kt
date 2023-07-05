package app.interactiveschemainferrer.strategy

import app.interactiveschemainferrer.Const.Fields
import app.interactiveschemainferrer.Const.Types
import app.interactiveschemainferrer.util.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.saasquatch.jsonschemainferrer.GenericSchemaFeature
import com.saasquatch.jsonschemainferrer.GenericSchemaFeatureInput
import com.saasquatch.jsonschemainferrer.SpecVersion
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.ButtonBar
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid
import tornadofx.*

/**
 * # Strategy: Contains/Prefix
 * This strategy implements the [Contains/PrefixItems](https://json-schema.org/understanding-json-schema/reference/array.html)
 * keyword.
 *
 * While the items schema must be valid for every item in the array,
 * the contains schema only needs to validate against one or more items in the array.
 * The prefixItems is done when all the valid parts are of the same index.
 *
 * This is done by checking if the current schema is for arrays and contains an `anyOf` as type.
 * When this is the case we validate for all samples each `anyOf` rule and check if they all contain it.
 *
 * If this is the case we ask the user if we should add a 'contains' to the rule
 *
 */
class ContainsStrategy : GenericSchemaFeature, AbstractStrategy() {


    /**
     * Infer potential contains from a set of examples and a schema.
     */
    fun inferPotentialContains(
        schema: ObjectNode, samples: Collection<JsonNode>, type: String?, specVersion: SpecVersion
    ): Pair<Set<Pair<IntConstrains, JsonNode>>, List<JsonNode>> {
        if (specVersion < SpecVersion.DRAFT_06) {
            logger.fine("Not an array input, thus we cannot infer contains")
            return emptySet<Pair<IntConstrains, JsonNode>>() to emptyList()
        }

        if (type != Types.ARRAY) {
            logger.fine("Not an array input, thus we cannot infer contains")
            return emptySet<Pair<IntConstrains, JsonNode>>() to emptyList()
        }
        if (schema[Fields.ITEMS]?.get(Fields.ANY_OF)?.isArray != true) {
            logger.fine("Array does not have an any-of type, testing if is type: array")
            if (schema[Fields.ITEMS]?.get(Fields.TYPE)?.isArray != true) {
                logger.fine("Is not type: array, thus a contains/prefixItems does not make sense.")
                return emptySet<Pair<IntConstrains, JsonNode>>() to emptyList()
            }
        } else if (!samples.all { it.isArray }) {
            logger.warning("Found a non-array sample in generic feature, the sample seems to be invalid.")
            return emptySet<Pair<IntConstrains, JsonNode>>() to emptyList()
        }
        fixAnyOfSchema(schema)



        val anyOfs: List<JsonNode> = schema[Fields.ITEMS][Fields.ANY_OF].elements().asSequence().toList()


        // Set of Min/Max/Schema
        val containsResult = mutableSetOf<Pair<IntConstrains, JsonNode>>()
        val largestLengthOfSamples = samples.maxOf(JsonNode::size)
        val prefixItemsResult: MutableList<JsonNode> = (1..largestLengthOfSamples).map { objectNode() }.toMutableList()

        // For each schema in the anyOf
        for (anyOf in anyOfs) {
            // Make a custom schema
            val jsonSchema = makeSchema(anyOf, specVersion)
            val constrains = IntConstrains()

            // For each array sample
            val indexAlwaysList = (1..largestLengthOfSamples).map { true }.toMutableList()
            for (sample in samples) {
                if (!sample.isArray) {
                    logger.warning("Found a non-array node while checking a sample of type array!")
                    continue
                }
                // Update the constraint
                constrains += sample.filterIndexed { index, it ->
                    // Check each in the array
                    val isValid = jsonSchema.validate(it).isEmpty()
                    indexAlwaysList[index] = indexAlwaysList[index] && isValid
                    isValid
                }.size
            }
            val trues = indexAlwaysList.withIndex().filter { it.value }
            // We have a case where it is true for all cases of an index.
            for (indexedValue in trues) {
                val (index, _) = indexedValue
                prefixItemsResult[index] = anyOf
            }
            // Backup, we also ask when it could a contains. We remove these when user response for a specific case.
            if (constrains.min > 0) {
                // We do have it at least present always
                containsResult += constrains to anyOf
            }

        }
        return containsResult to prefixItemsResult
    }

    /**
     * A schema from the inference library with anyOf can contain a list where one item is actually of "type" :[...]
     * it would be better if they were seperate, this function does that.
     */
    fun fixAnyOfSchema(schema: JsonNode) {
        var items = schema[Fields.ITEMS]
        if (items.has(Fields.ANY_OF)){
            val anyOfs: ArrayNode = items[Fields.ANY_OF] as ArrayNode
            val copy = anyOfs.deepCopy()
            for ((i, rule) in copy.withIndex()) {
                val type = rule["type"] ?: continue
                if (type.isArray) {
                    // Case of where this happens
                    anyOfs.remove(i)
                    (type as ArrayNode).forEach {
                        anyOfs.add(objectNode {
                            this.replace(Fields.TYPE, it)
                        })
                    }
                }
            }
        }
        if (items.has(Fields.TYPE) && items[Fields.TYPE].isArray) {
            val typeArray = items[Fields.TYPE] as ArrayNode
            (items as ObjectNode).replace(Fields.ANY_OF, arrayNode {
                typeArray.forEach{
                    this.add(objectNode {
                        this.replace(Fields.TYPE, it.deepCopy())
                    })
                }
            })
            items.remove(Fields.TYPE)
        }

    }


    /**
     * Get the add-on result to be merged in with the schema
     */
    override fun getFeatureResult(input: GenericSchemaFeatureInput): ObjectNode? {
        val (potentialContains, potentialPrefixItems) = inferPotentialContains(
            input.schema,
            input.samples,
            input.type,
            input.specVersion
        )
        if (potentialContains.isEmpty() && potentialPrefixItems.isEmpty()) {
            return null
        }
        logger.fine("Potential contains/prefixItems found")
        logger.finer("Contains: $potentialContains")
        logger.finer("PrefixItems: $potentialPrefixItems")

        val prefixItems = preProcessPrefixItems(potentialPrefixItems)


        val prefixResultPair = askUserWith(PrefixForm(prefixItems, input.path))
        if (prefixResultPair == null) {
            logger.warning("Result to PrefixForm was null! Skipping...")
            return null
        }
        val (prefixResult, skipContains) = prefixResultPair

        val result = if (prefixResult.isEmpty()) {
            logger.fine("User declined potential prefixItems.")
            null
        } else {
            logger.fine("User accepted potential prefixItems.")
            prefixResult.toSchemaCondition()
        }

        // If we skip contains, return here
        if (skipContains) {
            logger.fine("User skipped contains condition.")
            return result
        }

        val contains = preProcessContains(potentialContains.filterNot {
            // Remove all the cases that the prefixItems already processed.
            result?.get(Fields.PREFIX_ITEMS)?.contains(it.second) ?: false
        }.toSet())

        if (contains.isEmpty()){
            logger.fine("No other contains conditions possible.")
            return result;
        }

        val containsResult = askUserWith(ContainsForm(contains, input.path, input.specVersion))

        return when {
            containsResult.isNullOrEmpty() -> {
                logger.fine("User declined potential contains condition.")
                result
            }

            containsResult.size == 1 -> {
                logger.finest("Result with size 1. No allOf")
                contains.first().toObject().apply {
                    result?.fields()?.forEach {
                        this.replace(it.key, it.value)
                    }
                }
            }

            else -> {
                logger.finest("Result with size: ${containsResult.size}. No allOf")
                objectNode {
                    replace(Fields.ALL_OF, arrayNode(contains.map(ContainsCondition::toObject)))
                    result?.fields()?.forEach {
                        this.replace(it.key, it.value)
                    }
                }
            }
        }
    }

    /**
     * Converts a set of potential constraints to the TornadoFX models
     */
    private fun preProcessContains(potentialConstraints: Set<Pair<IntConstrains, JsonNode>>): MutableList<ContainsCondition> {
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

    /**
     * Converts a set of potential constraints to the TornadoFX models
     */
    private fun preProcessPrefixItems(potentialPrefixItems: List<JsonNode>): MutableList<PrefixItemsCondition> {
        return potentialPrefixItems
            .map { PrefixItemsCondition(it.toPrettyString()) }.toMutableList()
    }


    private fun makeSchema(schema: JsonNode, specVersion: SpecVersion): JsonSchema {
        val instance = JsonSchemaFactory.getInstance(specVersion.asNetworknt())
        return instance.getSchema(schema)
    }


    private class ContainsCondition(
        json: String,
        minValue: Int,
        maxValue: Int,
        minDisabled: Boolean,
        maxDisabled: Boolean
    ) {
        val jsonString = SimpleStringProperty(json)
        val minValue = SimpleIntegerProperty(minValue)
        val maxValue = SimpleIntegerProperty(maxValue)
        val minDisabled = SimpleBooleanProperty(minDisabled)
        val maxDisabled = SimpleBooleanProperty(maxDisabled)


        fun toObject(): ObjectNode = objectNode {
            this.replace(Fields.CONTAINS, jsonString.get().asJson())
            if (minDisabled.get()) {
                this.put(Fields.MIN_CONTAINS, minValue.get())
            }
            if (maxDisabled.get()) {
                this.put(Fields.MAX_CONTAINS, maxValue.get())
            }
        }
    }

    private class PrefixItemsCondition(
        json: String
    ) {
        val jsonString = SimpleStringProperty(json)
        val enabled = SimpleBooleanProperty(true)
    }

    private fun List<PrefixItemsCondition>.toSchemaCondition(): ObjectNode = objectNode {
        this.replace(Fields.PREFIX_ITEMS, arrayNode {
            // Make a list where we remove the empty prefixIndex schemas.
            val values = this@toSchemaCondition.dropLastWhile {
                it.jsonString.get() == objectNode().toPrettyString()
            }
            for (prefixItems in values) {
                // If disabled, do not add it.
                if (!prefixItems.enabled.get()) {
                    this.add(objectNode())
                    continue
                }
                this.add(prefixItems.jsonString.get().asJson())
            }
        })
    }


    /**
     * A form for specifying the prefixItems. Returns a [Pair] where first
     * is the list of [PrefixItemsCondition] schema rules
     * and the second is a [Boolean] that specifies if we should continue to [ContainsForm].
     */
    private class PrefixForm(
        potentialPrefixItems: List<PrefixItemsCondition>,
        val path: String
    ) :
        StrategyFragment<Pair<List<PrefixItemsCondition>, Boolean>>("Inferring - Possible PrefixItems Found") {
        val prefixItemsValues = potentialPrefixItems.asObservable()
        val skipContains = SimpleBooleanProperty(false)

        override val root = vbox(spacing = 20.0) {
            paddingAll = 20.0

            // Add the multiline description label
            label {
                graphic = textflow {
                    text("The array with the path: ")
                    text(path) { font = Font.font("Monospace") }
                    text(" seems to always contain the following conditions at the specific index.")
                    text("\n")
                    text("Should this field have these 'prefixItems' conditions?")
                    text("\n")
                    text("If you rather want a 'contains' condition, disable all the cases you do not want or press NO. ");
                    text("After this question, if applicable, we will ask you to specify 'contains' excluding all the rules from here.")
                }
                isWrapText = true
            }

            separator()

            // Body
            listview(prefixItemsValues) {
                cellFormat { prefixItems ->
                    graphic =
                        hbox(spacing = 20) {
                            checkbox(property = prefixItems.enabled) {
                                this.alignment = Pos.CENTER_LEFT
                                selectedProperty().onChange {
                                    // todo: fix css not working
                                    this@hbox.style {
                                        if (it) {
                                            opacity = 1.0
                                            strikethrough = false
                                            fontStyle = FontPosture.REGULAR
                                        } else {
                                            opacity = .5
                                            fontStyle = FontPosture.ITALIC
                                            strikethrough = true
                                        }
                                    }
                                }
                            }
                            jsonarea(property = prefixItems.jsonString) {
                                // Not editable as it is based on the existing schema
                                isEditable = false
                                hgrow = Priority.ALWAYS
                                minHeight = 50.0
                            }
                        }

                }
            }

            separator()
            // Extra option to skip contains.
            hbox {
                checkbox(text = "Skip Contains?", property = skipContains)
            }
            // Button Bar
            buttonbar {
                button("Yes", ButtonBar.ButtonData.YES) {
                    enableWhen(validator.valid)
                    action {
                        done(prefixItemsValues to skipContains.get())
                    }
                }
                button("No", ButtonBar.ButtonData.NO) {
                    action {
                        done(emptyList<PrefixItemsCondition>() to skipContains.get())
                    }
                }
            }
        }
    }

    private class ContainsForm(
        potentialConstraints: List<ContainsCondition>,
        val path: String,
        specVersion: SpecVersion
    ) :
        StrategyFragment<List<ContainsCondition>>("Inferring - Possible Contains Found") {

        val notAvailableInVersion = specVersion < SpecVersion.DRAFT_2019_09
        val containsValues = potentialConstraints.asObservable()

        override val root = vbox(spacing = 20.0) {
            paddingAll = 20.0

            // Add the multiline description label
            label {
                graphic = textflow {
                    text("The array with the path: ")
                    text(path) { font = Font.font("Monospace") }
                    text(" seems to always contain the following conditions.")
                    text("\n")
                    text("Should this field have these 'contains' conditions?")
                }
                isWrapText = true
            }

            separator()

            // Body
            listview(containsValues) {
                cellFormat { condition ->
                    graphic = cache {
                        hbox(spacing = 20) {
                            button {
                                this.alignment = Pos.CENTER_LEFT
                                this.graphic = fonticon(FontAwesomeSolid.TRASH)
                                action {
                                    containsValues.remove(condition)
                                }
                            }
                            jsonarea(property = condition.jsonString) {
                                // Not editable as it is based on the existing schema
                                isEditable = false
                                hgrow = Priority.ALWAYS
                            }
                            vbox(spacing = 20) {
                                hbox(spacing = 5) {
                                    label(text = "Min:")
                                    if (notAvailableInVersion) tooltip("Not available in current schema version.")
                                    checkbox(property = condition.minDisabled) {
                                        this.isDisable = notAvailableInVersion
                                    }
                                    spinner(property = condition.minValue, max = condition.minValue.get()) {
                                        disableProperty().bind(condition.minDisabled.not())
                                    }
                                }
                                hbox(spacing = 5) {
                                    label(text = "Max:")
                                    if (notAvailableInVersion) tooltip("Not available in current schema version.")
                                    checkbox(property = condition.maxDisabled) {
                                        this.isDisable = notAvailableInVersion
                                    }
                                    spinner(property = condition.maxValue, min = condition.maxValue.get()) {
                                        disableProperty().bind(condition.maxDisabled.not())
                                    }
                                }
                            }
                        }
                    }
                }
            }

            separator()
            // Button Bar
            buttonbar {
                button("Yes", ButtonBar.ButtonData.YES) {
                    enableWhen(validator.valid)
                    action {
                        done(containsValues)
                    }
                }
                button("No", ButtonBar.ButtonData.NO) {
                    action {
                        done()
                    }
                }
            }

        }
    }

}
