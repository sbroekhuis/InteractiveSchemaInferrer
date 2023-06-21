package app.interactiveschemainferrer

import app.interactiveschemainferrer.gui.ConfigurationView
import app.interactiveschemainferrer.gui.InferringView
import app.interactiveschemainferrer.gui.ResultView
import app.interactiveschemainferrer.strategy.ConstDetection
import app.interactiveschemainferrer.strategy.DefaultStrategy
import app.interactiveschemainferrer.strategy.EnumDetection
import app.interactiveschemainferrer.util.InferConfigModel
import app.interactiveschemainferrer.util.convertFilesToJson
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.saasquatch.jsonschemainferrer.JsonSchemaInferrer
import com.saasquatch.jsonschemainferrer.RequiredPolicies
import javafx.stage.Stage
import javafx.util.Duration
import tornadofx.*
import java.io.IOException
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger


class InteractiveInferenceApp : App(ConfigurationView::class) {
    init {
        importStylesheet("/styles.css")
    }

    override fun start(stage: Stage) {
        with(stage) {
            minWidth = 500.0
            minHeight = 600.0
            super.start(this)
        }
    }
}

class InteractiveInferenceController : Controller() {
    fun startInference() {
        val jsonFiles = convertFilesToJson(inferConfig.inputJson.value, inferConfig.assumeArray.value)
        runAsync {
            val schemaInferrer = JsonSchemaInferrer.newBuilder()
                .setSpecVersion(inferConfig.schemaVersion.value)
                .setRequiredPolicy(RequiredPolicies.commonFields())
                // STRATEGIES:
                .addEnumExtractors(EnumDetection())
                .addGenericSchemaFeatures(ConstDetection())
//                .addGenericSchemaFeatures(DefaultStrategy())
                .setDefaultPolicy(DefaultStrategy())
                //
                .build()

            resultSchema = schemaInferrer.inferForSamples(jsonFiles)
        }.success {
            runLater {
                find<InferringView>().replaceWith<ResultView>(
                    transition = ViewTransition.Slide(
                        duration = Duration(
                            100.0
                        )
                    )
                )
            }
        }

    }

    val inferConfig: InferConfigModel by inject()
    var resultSchema: JsonNode = JsonNodeFactory.instance.objectNode()
}

fun main(args: Array<String>) {
    readLogConfig();
    launch<InteractiveInferenceApp>(args)
}

fun readLogConfig() {
    val log: Logger = Logger.getLogger("app.interactiveschemainferrer.ConfigHandler")
    log.level = Level.ALL
    log.info("initializing - trying to load configuration file ...")

    try {
        val configFile = object{}.javaClass.getResourceAsStream("/logging.properties")
        LogManager.getLogManager().readConfiguration(configFile)
    } catch (ex: IOException) {
        log.warning("Could not open configuration file")
        log.warning("Logging not configured (console output only)")
    }
    log.info("Starting Program...")
}
