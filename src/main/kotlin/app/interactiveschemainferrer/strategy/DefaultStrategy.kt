package app.interactiveschemainferrer.strategy

import app.interactiveschemainferrer.util.asJson
import app.interactiveschemainferrer.util.frequencies
import app.interactiveschemainferrer.util.jsonarea
import com.fasterxml.jackson.databind.JsonNode
import com.saasquatch.jsonschemainferrer.DefaultPolicy
import com.saasquatch.jsonschemainferrer.GenericSchemaFeatureInput
import javafx.scene.control.ButtonBar
import javafx.scene.layout.Priority
import javafx.scene.text.Font
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import tornadofx.*
import java.util.logging.Level

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
    fun <T : Any> outliers(values: Collection<T>): Map<T, Int> {

        // TODO: Design choice write down 1.5IQR or 3SigmaRule
        // Reason for 3-sigma-rule is that it is less sensitive, resulting in a better user experience.

        val frequencies: Map<T, Int> = values.frequencies()
        val stats = DescriptiveStatistics()

        frequencies.forEach { stats.addValue(it.value.toDouble()) }

        val mean = stats.mean
        val standardDeviation = stats.standardDeviation

        val upperThreshold = mean + (3 * standardDeviation)
        // Do not calculate val if not needing to log.
        if (logger.isLoggable(Level.FINER)) {
            val freqString = frequencies.toList().sortedByDescending { pair -> pair.second }.toString()
            logger.finer("Outlier info: standardDeviation:$standardDeviation mean:$mean upperThreshold:$upperThreshold size:${frequencies.size}")
            logger.finer("Frequencies: $freqString")
        }

        return frequencies.filter { it.value > upperThreshold }.toMap()
    }


    /**
     * Get the default based on the count of distinct values in the samples using [outliers] function.
     */
    override fun getDefault(input: GenericSchemaFeatureInput): JsonNode? {
        val outliers = outliers(input.samples)
        if (outliers.isEmpty()) {
            logger.fine("Samples empty, nothing to infer")
            return null
        }


        val defaults = outliers.asObservable()

        if (defaults.isEmpty()) {
            logger.fine("Samples contains no outliers, continuing")
            return null
        }

        // Examples does not yet exist, use default instead

        val (first, _) = outliers.maxBy { it.value }.toPair()
        logger.fine("Potential default found.")
        logger.finer(first.toPrettyString())

        val result = askUserWith(Form(first, input.path))

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
