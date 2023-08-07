package app.interactiveschemainferrer.strategy

import app.interactiveschemainferrer.Const
import app.interactiveschemainferrer.util.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.saasquatch.jsonschemainferrer.GenericSchemaFeature
import com.saasquatch.jsonschemainferrer.GenericSchemaFeatureInput
import com.saasquatch.jsonschemainferrer.SpecVersion
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import javafx.scene.Parent
import javafx.scene.control.ButtonBar
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*

/**
 * This strategy implements min/max for numbers object sizes, or arrays.
 *
 *
 */
class LengthStrategy : AbstractStrategy(), GenericSchemaFeature {

    private val storage: MutableList<Condition> = mutableListOf()

    companion object {
        const val SCHEMA_LOCATE_KEY = "_temp_length_reference_key"

        /**
         * Recursively remove a specific key from a json node and its children.
         */
        fun removeKeyFromJsonNode(jsonNode: JsonNode, keyToRemove: String) {
            if (jsonNode is ObjectNode || jsonNode is ArrayNode) {
                if (jsonNode.isObject) {
                    (jsonNode as ObjectNode).remove(keyToRemove)
                }
                for (element in jsonNode.elements()) {
                    removeKeyFromJsonNode(element, keyToRemove)
                }
            }
        }
    }


    /**
     * Get the add-on result to be merged in with the schema
     */
    override fun getFeatureResult(input: GenericSchemaFeatureInput): ObjectNode {
        val (_: ObjectNode, samples: MutableCollection<out JsonNode>, _: String?, specVersion: SpecVersion, path: String) = input


        val numberNodes = mutableListOf<NumericNode>()
        val arrayNodes = mutableListOf<ArrayNode>()
        val objectNodes = mutableListOf<ObjectNode>()
        val stringNodes = mutableListOf<TextNode>()

        for (sample in samples) {
            when (sample) {
                is NumericNode -> numberNodes.add(sample)
                is ArrayNode -> arrayNodes.add(sample)
                is ObjectNode -> objectNodes.add(sample)
                is TextNode -> stringNodes.add(sample)
                else -> {} // Unsupported
            }
        }
        if (numberNodes.isNotEmpty()) {
            inferNumber(numberNodes, path, specVersion)
        }
        if (arrayNodes.isNotEmpty()) {
            inferArray(arrayNodes, path)
        }
        if (objectNodes.isNotEmpty()) {
            inferObject(objectNodes, path)
        }
        if (stringNodes.isNotEmpty()) {
            inferString(stringNodes, path)
        }

        return objectNode { this.put(SCHEMA_LOCATE_KEY, path) }
    }

    private fun inferArray(samples: List<ArrayNode>, path: String) {
        require(samples.isNotEmpty()) { "Samples cannot be empty" }

        val constraint = IntConstraint(0)
        for (sample in samples) {
            constraint.update(sample.size())
        }
        storage.add(ItemsCondition(path, constraint))
    }

    private fun inferNumber(samples: List<NumericNode>, path: String, specVersion: SpecVersion) {
        require(samples.isNotEmpty()) { "Samples cannot be empty" }

        val constraint = Constraint(0.0)
        for (it in samples) {
            constraint.update(it.doubleValue())
        }
        storage.add(RangeCondition(path, constraint, specVersion))
    }

    private fun inferObject(samples: List<ObjectNode>, path: String) {
        require(samples.isNotEmpty()) { "Samples cannot be empty" }

        val constraint = IntConstraint(0)
        for (sample in samples) {
            constraint.update(sample.size())
        }
        storage.add(PropertiesCondition(path, constraint))
    }

    private fun inferString(samples: List<TextNode>, path: String) {
        require(samples.isNotEmpty()) { "Samples cannot be empty" }

        val constraint = IntConstraint(0)
        for (sample in samples) {
            constraint.update(sample.textValue().length)
        }
        storage.add(LengthCondition(path, constraint))
    }

    override fun postProcess(schema: JsonNode) {
        logger.info("Post-Processing...")

        val schemaReferences: Map<String, JsonNode> =
            schema.findParents(SCHEMA_LOCATE_KEY).associateBy { it.get(SCHEMA_LOCATE_KEY).textValue() }

        askUserWith(LengthForm(storage)).also { result ->
            for (condition in result) {
                // Use the key to get the reference in the schema.
                val subSchema = schemaReferences[condition.path] as ObjectNode
                // Apply the conditions
                condition.applyToSchema(subSchema)
            }
        }

        // Remove all locations key references.
        removeKeyFromJsonNode(schema, SCHEMA_LOCATE_KEY)
    }

    private class LengthForm(
        storage: List<Condition>
    ) :
        StrategyFragment<List<Condition>>("Inferring - Additional Size Information") {

        val conditions: ObservableList<Condition> = storage.asObservable()


        override val root = vbox {
            // Custom Menu bar for this strategy
            menubar {
                menu("Help") {
                    for ((type, link) in listOf(
                        "Array Length" to "https://json-schema.org/understanding-json-schema/reference/array.html#length",
                        "Object Size" to "https://json-schema.org/understanding-json-schema/reference/object.html#size",
                        "Number Range" to "https://json-schema.org/understanding-json-schema/reference/numeric.html#range",
                        "String Length" to "https://json-schema.org/understanding-json-schema/reference/string.html#length"
                    )) {
                        item(
                            "Documentation [$type]", graphic = FontIcon(FontAwesomeSolid.EXTERNAL_LINK_ALT)
                        ).action {
                            hostServices.showDocument(link)
                        }
                    }
                }
            }

            vbox(spacing = 20) {
                vgrow = Priority.ALWAYS
                paddingAll = 20.0

                // Add the multiline description label
                label {
                    graphic = text(
                        """
                        The following fields have the ability to specify a length/min/max.
                    """.trimIndent()
                    )
                    isWrapText = true
                }
                separator()

                // Body
                listview(conditions) {
                    this.selectionModel = NoSelectionModel()
                    this.isFocusTraversable = false

                    cellFormat { condition ->
                        graphic = cache(condition.path) {
                            vbox(spacing = 5) {
                                text(condition.path) { font = Font.font("Monospace") }
                                condition.render(this)
                            }
                        }
                    }
                }

                separator()
                //Button Bar
                region { vgrow = Priority.ALWAYS }
                buttonbar {
                    button(text = "Apply & Continue", ButtonBar.ButtonData.APPLY) {
                        action { done(conditions) }
                    }
                }
            }
        }

    }


    // --- CONDITIONS ---

    private sealed class Condition(val path: String) {
        abstract fun applyToSchema(schema: ObjectNode)
        abstract fun render(parent: Parent)
    }

    private class RangeCondition(
        path: String,
        constraint: Constraint<Double>,
        specVersion: SpecVersion
    ) : Condition(path) {
        val minValue = SimpleObjectProperty(constraint.min)
        val maxValue = SimpleObjectProperty(constraint.max)
        val exclusiveMin = SimpleBooleanProperty(false)
        val exclusiveMax = SimpleBooleanProperty(false)
        val minEnabled = SimpleBooleanProperty(false)
        val maxEnabled = SimpleBooleanProperty(false)
        val applier: DraftVersionApplier

        init {
            this.applier = when (specVersion) {
                SpecVersion.DRAFT_04 -> DraftVersionApplier.DRAFT_4
                else -> DraftVersionApplier.DEFAULT
            }
        }

        /**
         * Different versions have different implementations.
         */
        enum class DraftVersionApplier : (ObjectNode, RangeCondition) -> Unit {
            DRAFT_4 {
                override fun invoke(schema: ObjectNode, condition: RangeCondition) {
                    if (!condition.minEnabled.get()) {
                        schema.put(Const.Fields.MINIMUM, condition.minValue.get().convert(schema))
                        if (condition.exclusiveMin.get()) {
                            schema.put(Const.Fields.EXCLUSIVE_MINIMUM, true)
                        }
                    }
                    if (!condition.maxEnabled.get()) {
                        schema.put(Const.Fields.MAXIMUM, condition.maxValue.get().convert(schema))
                        if (condition.exclusiveMax.get()) {
                            schema.put(Const.Fields.EXCLUSIVE_MAXIMUM, true)
                        }
                    }
                }
            },
            DEFAULT {
                override fun invoke(schema: ObjectNode, condition: RangeCondition) {
                    if (condition.minEnabled.get()) {
                        schema.put(
                            if (condition.exclusiveMin.get()) {
                                Const.Fields.EXCLUSIVE_MINIMUM
                            } else {
                                Const.Fields.MINIMUM
                            }, condition.minValue.get().convert(schema)
                        )
                    }
                    if (condition.maxEnabled.get()) {
                        schema.put(
                            if (condition.exclusiveMax.get()) {
                                Const.Fields.EXCLUSIVE_MAXIMUM
                            } else {
                                Const.Fields.MAXIMUM
                            }, condition.maxValue.get().convert(schema)
                        )
                    }
                }
            };

            protected fun Double.convert(schema: ObjectNode): Number =
                if (schema[Const.Fields.TYPE].textValue() != Const.Types.INTEGER) {
                    this
                } else {
                    this.toInt()
                }
        }

        override fun applyToSchema(schema: ObjectNode) {
            applier.invoke(schema, this)
        }


        override fun render(parent: Parent) {
            parent.apply {
                gridpane {
                    hgap = 5.0
                    vgap = 5.0
                    row {
                        checkbox(property = this@RangeCondition.minEnabled)
                        label(text = Const.Fields.MINIMUM + ":")
                        spinner(
                            property = this@RangeCondition.minValue,
                            max = this@RangeCondition.minValue.get(),
                            min = -Double.MAX_VALUE,
                            editable = true
                        ) {
                            disableProperty().bind(this@RangeCondition.minEnabled.not())
                        }
                        checkbox(property = this@RangeCondition.exclusiveMin, text = "Exclusive")
                    }
                    row {
                        checkbox(property = this@RangeCondition.maxEnabled) {}
                        label(text = Const.Fields.MAXIMUM + ":")
                        spinner(
                            property = this@RangeCondition.maxValue,
                            min = this@RangeCondition.maxValue.get(),
                            max = Double.MAX_VALUE
                        ) {
                            disableProperty().bind(this@RangeCondition.maxEnabled.not())
                        }
                        checkbox(property = this@RangeCondition.exclusiveMax, text = "Exclusive")
                    }
                }
            }
        }
    }

    private class ItemsCondition(
        path: String,
        constraint: IntConstraint,
    ) : Condition(path) {
        val minValue = SimpleIntegerProperty(constraint.min)
        val maxValue = SimpleIntegerProperty(constraint.max)
        val minEnabled = SimpleBooleanProperty(false)
        val maxEnabled = SimpleBooleanProperty(false)

        override fun applyToSchema(schema: ObjectNode) {
            if (minEnabled.get()) {
                schema.put(Const.Fields.MIN_ITEMS, minValue.get())
            }
            if (maxEnabled.get()) {
                schema.put(Const.Fields.MAX_ITEMS, maxValue.get())
            }
        }

        override fun render(parent: Parent) {
            parent.apply {
                gridpane {
                    hgap = 5.0
                    vgap = 5.0
                    row {
                        checkbox(property = this@ItemsCondition.minEnabled) {}
                        label(text = Const.Fields.MIN_ITEMS + ":")
                        spinner(
                            property = this@ItemsCondition.minValue,
                            max = this@ItemsCondition.minValue.get(),
                            min = 0,
                            editable = true
                        ) {
                            disableProperty().bind(this@ItemsCondition.minEnabled.not())
                        }
                    }
                    row {
                        checkbox(property = this@ItemsCondition.maxEnabled) {}
                        label(text = Const.Fields.MAX_ITEMS + ":")
                        spinner(
                            property = this@ItemsCondition.maxValue,
                            min = this@ItemsCondition.maxValue.get(),
                            max = Int.MAX_VALUE,
                            editable = true
                        ) {
                            disableProperty().bind(this@ItemsCondition.maxEnabled.not())
                        }
                    }
                }
            }
        }
    }

    private class PropertiesCondition(
        path: String,
        constraint: IntConstraint,
    ) : Condition(path) {
        val minValue = SimpleIntegerProperty(constraint.min)
        val maxValue = SimpleIntegerProperty(constraint.max)
        val minEnabled = SimpleBooleanProperty(false)
        val maxEnabled = SimpleBooleanProperty(false)

        override fun applyToSchema(schema: ObjectNode) {
            if (minEnabled.get()) {
                schema.put(Const.Fields.MIN_PROPERTIES, minValue.get())
            }
            if (maxEnabled.get()) {
                schema.put(Const.Fields.MAX_PROPERTIES, maxValue.get())
            }
        }

        override fun render(parent: Parent) {
            parent.apply {
                gridpane {
                    hgap = 5.0
                    vgap = 5.0
                    row {
                        checkbox(property = this@PropertiesCondition.minEnabled) {}
                        label(text = Const.Fields.MIN_PROPERTIES + ":")
                        spinner(
                            property = this@PropertiesCondition.minValue,
                            max = this@PropertiesCondition.minValue.get(),
                            min = 0,
                            editable = true
                        ) {
                            disableProperty().bind(this@PropertiesCondition.minEnabled.not())
                        }
                    }
                    row {
                        checkbox(property = this@PropertiesCondition.maxEnabled) {}
                        label(text = Const.Fields.MAX_PROPERTIES + ":")
                        spinner(
                            property = this@PropertiesCondition.maxValue,
                            min = this@PropertiesCondition.maxValue.get(),
                            max = Int.MAX_VALUE,
                            editable = true
                        ) {
                            disableProperty().bind(this@PropertiesCondition.maxEnabled.not())
                        }
                    }
                }
            }
        }
    }

    private class LengthCondition(
        path: String,
        constraint: IntConstraint,
    ) : Condition(path) {
        val minValue = SimpleIntegerProperty(constraint.min)
        val maxValue = SimpleIntegerProperty(constraint.max)
        val minEnabled = SimpleBooleanProperty(false)
        val maxEnabled = SimpleBooleanProperty(false)

        override fun applyToSchema(schema: ObjectNode) {
            if (minEnabled.get()) {
                schema.put(Const.Fields.MIN_LENGTH, minValue.get())
            }
            if (maxEnabled.get()) {
                schema.put(Const.Fields.MAX_LENGTH, maxValue.get())
            }
        }

        override fun render(parent: Parent) {
            parent.apply {
                gridpane {
                    hgap = 5.0
                    vgap = 5.0
                    row {
                        checkbox(property = this@LengthCondition.minEnabled) {}
                        label(text = Const.Fields.MIN_LENGTH + ":")
                        spinner(
                            property = this@LengthCondition.minValue,
                            max = this@LengthCondition.minValue.get(),
                            min = 0,
                            editable = true
                        ) {
                            disableProperty().bind(this@LengthCondition.minEnabled.not())
                        }
                    }
                    row {
                        checkbox(property = this@LengthCondition.maxEnabled)
                        label(text = Const.Fields.MAX_LENGTH + ":")
                        spinner(
                            property = this@LengthCondition.maxValue,
                            min = this@LengthCondition.maxValue.get(),
                            max = Int.MAX_VALUE,
                            editable = true
                        ) {
                            disableProperty().bind(this@LengthCondition.maxEnabled.not())
                        }
                    }
                }
            }
        }
    }


}
