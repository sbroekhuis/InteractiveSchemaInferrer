package app.interactiveschemainferrer.strategy

import app.interactiveschemainferrer.util.frequencies
import app.interactiveschemainferrer.util.newCodeArea
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.saasquatch.jsonschemainferrer.DefaultPolicy
import com.saasquatch.jsonschemainferrer.GenericSchemaFeatureInput
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ButtonBar
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import tornadofx.*
import java.util.logging.Level
import java.util.logging.Logger

/**
 * # Strategy: Default / Examples
 * This strategy implements the [Default](https://json-schema.org/understanding-json-schema/reference/generic.html#annotations)
 * keyword.
 *
 * This implements
 *
 */
class DefaultStrategy : DefaultPolicy {
    companion object {

        val logger: Logger by lazy { Logger.getLogger(DefaultStrategy::class.qualifiedName) }


        /**
         * Using 1.5 IQR, detect outliers based on the count of each distinct value in the collection.
         */
        internal fun <T : Any> Collection<T>.outliers(): Map<T, Int> {

            // TODO: Design choice write down 1.5IQR or 3SigmaRule
            val frequencies: Map<T, Int> = this.frequencies()
            val stats = DescriptiveStatistics()

            frequencies.forEach { stats.addValue(it.value.toDouble()) }

            val mean = stats.mean
            val standardDeviation = stats.standardDeviation


//            val q1 = stats.getPercentile(25.0)
//            val q3 = stats.getPercentile(75.0)
//            val iqr = q3 - q1

            val upperThreshold = mean + (3 * standardDeviation)
//            val upperThreshold = q3 + 1.5 * iqr
            if (logger.isLoggable(Level.FINER)) {
                val freqString = frequencies.toList().sortedByDescending { pair -> pair.second }.toString()
                logger.finer("Outlier info: standardDeviation:$standardDeviation mean:$mean upperThreshold:$upperThreshold size:${frequencies.size}")
                logger.finer("Frequencies: $freqString")
            }

            return frequencies.filter { it.value > upperThreshold }.toMap()
        }
    }


    /**
     * Get the default based on the count of distinct values in the samples using [outliers] function.
     */
    override fun getDefault(input: GenericSchemaFeatureInput): JsonNode? {
        logger.info("Entering: ${input.path}")
        val outliers = input.samples.outliers()
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
        val answer = SimpleBooleanProperty(false)
        val result = SimpleStringProperty(first.toPrettyString())

        askUserWith("Inferring - Possible Default Found") {
            form {
                val validator = ValidationContext()
                fieldset {
                    label(
                        """
                        |The field with path: ${input.path} seems to have common value.
                        |Should this be the default??
                        """.trimMargin(),
                    )

                    opcr(this, newCodeArea(first.toPrettyString())).apply {
                        result.bind(this.textProperty())
                    }
                }
                buttonbar {
                    button("Yes", ButtonBar.ButtonData.YES) {
                        enableWhen(validator.valid)
                        action {
                            answer.set(true)
                            done()
                        }
                    }
                    button("No", ButtonBar.ButtonData.NO) {
                        action {
                            answer.set(false)
                            done()
                        }
                    }
                }
            }
        }

        if (!answer.get()) {
            logger.info("Default for ${input.path} declined")
            // User does not want default
            return null
        }

        val readTree = ObjectMapper().readTree(result.get())
        logger.info("Default for ${input.path} accepted, using:")
        logger.info(readTree.toPrettyString())
        return readTree
    }


}
