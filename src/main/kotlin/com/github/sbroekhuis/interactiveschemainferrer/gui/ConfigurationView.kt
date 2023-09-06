package com.github.sbroekhuis.interactiveschemainferrer.gui

import com.github.sbroekhuis.interactiveschemainferrer.InteractiveInferenceController
import com.saasquatch.jsonschemainferrer.SpecVersion
import javafx.scene.control.ButtonBar
import javafx.stage.FileChooser
import javafx.util.Duration
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*

class ConfigurationView : View("Configuration") {

    private val controller: InteractiveInferenceController by inject()
    private val inferConfig = controller.inferConfig

    override val root = form {
        fieldset {
            field("Schema Version") {
                choicebox(
                    values = SpecVersion.values().toList(),
                    property = inferConfig.schemaVersion,
                ).validator {
                    if (it == null) error("Specify a Version") else null
                }
            }
            field("Assume JSON Array") {
                label.graphic = FontIcon(FontAwesomeRegular.QUESTION_CIRCLE)

                checkbox().bind(inferConfig.assumeArray)
                tooltip {
                    text =
                        "If selected, the provides JSON files are marked as JSON arrays and each value is one example."
                }
            }
            field("Samples") {
                button("Choose Files") {
                    action {
                        val files = chooseFile(
                            title = "Choose Json Example Files",
                            filters = arrayOf(FileChooser.ExtensionFilter("Json Files", "*.json")),
                            mode = FileChooserMode.Multi,
                        )
                        inferConfig.inputJson.set(files.asObservable())
                    }
                }
                inferConfig.addValidator(node = this, property = inferConfig.inputJson) {
                    when {
                        it.isNullOrEmpty() -> {
                            error("We have no files to infer from!")
                        }

                        else -> {
                            null
                        }
                    }
                }
            }
            listview(inferConfig.inputJson)
        }
        buttonbar {
            button("Infer Schema", ButtonBar.ButtonData.APPLY) {
                id = "inferStart"
                enableWhen(inferConfig.valid)
                action {
                    inferConfig.commit {
                        replaceWith<InferringView>(transition = ViewTransition.Slide(duration = Duration(100.0)))
                        controller.startInference()
                    }
                }
            }
        }
    }
}
