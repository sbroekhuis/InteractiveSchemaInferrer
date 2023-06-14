package app

import app.gui.ConfigurationView
import app.gui.InferringView
import app.gui.ResultView
import app.strategy.ConstDetection
import app.util.InferConfigModel
import app.util.convertFilesToJson
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.saasquatch.jsonschemainferrer.JsonSchemaInferrer
import com.saasquatch.jsonschemainferrer.RequiredPolicies
import javafx.stage.Stage
import javafx.util.Duration
import tornadofx.*


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
                .addGenericSchemaFeatures(ConstDetection())
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
    launch<InteractiveInferenceApp>(args)
}
