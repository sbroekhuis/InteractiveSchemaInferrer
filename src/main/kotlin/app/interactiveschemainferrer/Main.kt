package app.interactiveschemainferrer

import app.interactiveschemainferrer.gui.ConfigurationView
import app.interactiveschemainferrer.gui.InferringView
import app.interactiveschemainferrer.gui.ResultView
import app.interactiveschemainferrer.strategy.ContainsStrategy
import app.interactiveschemainferrer.util.InferConfigModel
import app.interactiveschemainferrer.util.addStrategy
import app.interactiveschemainferrer.util.convertFilesToJson
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.saasquatch.jsonschemainferrer.JsonSchemaInferrer
import com.saasquatch.jsonschemainferrer.RequiredPolicies
import javafx.application.Platform
import javafx.stage.Stage
import javafx.util.Duration
import tornadofx.*
import java.io.IOException
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger
import kotlin.system.exitProcess


class InteractiveInferenceApp : App(ConfigurationView::class) {
    init {
        importStylesheet("/styles.css")
    }

    override fun start(stage: Stage) {
        with(stage) {
            minWidth = 500.0
            minHeight = 600.0
            Platform.setImplicitExit(true)
            stage.setOnCloseRequest {
                Platform.exit()
                exitProcess(0)
            }
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
//                .addStrategy(EnumDetection())
//                .addStrategy(ConstDetection())
//                .addStrategy(DefaultStrategy())
                .addStrategy(ContainsStrategy())
                //
                .build()

            log.info("Starting Inferring Schema")
            resultSchema = schemaInferrer.inferForSamples(jsonFiles)
            log.info("Finishing Inferring Schema")
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
enum class Fruit{
    APPLE, BANANA
}

fun main(args: Array<String>) {

//
//    val schemaInferrer = JsonSchemaInferrer.newBuilder()
//        .setSpecVersion(SpecVersion.DRAFT_2020_12)
//        .setRequiredPolicy(RequiredPolicies.commonFields())
//        // STRATEGIES:
//        .addStrategy(ContainsStrategy())
//        .addEnumExtractors(EnumExtractors.validEnum(Fruit::class.java))
//        //
//        .build()
//
//
//    schemaInferrer.inferForSamples(
//        listOf(
//            objectNode {
//                this.putArray("foo") {
//                    this.add("BANANA")
//                    this.add(2)
//                    this.add(arrayNode {
//                        this.add(3)
//                        this.add(5)
//                    })
//                }
//            },
//            objectNode {
//                this.putArray("foo") {
//                    this.add("APPLE")
//                    this.add(2)
//                    this.add(arrayNode {
//                        this.add(3)
//                        this.add(5)
//                    })
//                }
//            },
//        )
//    )
//    return;
    readLogConfig();
    launch<InteractiveInferenceApp>(args)
}


fun readLogConfig() {
    val log: Logger = Logger.getLogger("app.interactiveschemainferrer.ConfigHandler")
    log.level = Level.ALL
    log.info("initializing - trying to load configuration file ...")

    try {
        val configFile = object {}.javaClass.getResourceAsStream("/logging.properties")
        LogManager.getLogManager().readConfiguration(configFile)
    } catch (ex: IOException) {
        log.warning("Could not open configuration file")
        log.warning("Logging not configured (console output only)")
    }
    log.info("Starting Program...")
}
