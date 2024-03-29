package com.github.sbroekhuis.interactiveschemainferrer.gui

import com.github.sbroekhuis.interactiveschemainferrer.InteractiveInferenceController
import com.github.sbroekhuis.interactiveschemainferrer.util.fonticon
import com.saasquatch.jsonschemainferrer.SpecVersion
import javafx.geometry.Pos
import javafx.scene.control.ButtonBar
import javafx.stage.FileChooser
import javafx.util.Duration
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*
import java.util.stream.Collectors

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
                            title = "Choose JSON Example Files",
                            filters = arrayOf(FileChooser.ExtensionFilter("JSON Files", "*.json")),
                            mode = FileChooserMode.Multi,
                        )
                        inferConfig.inputJson.addAll(
                            files.stream().filter { t -> inferConfig.inputJson.none { u -> u == t } }.collect(
                                Collectors.toList()
                            )
                        )
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
            listview(inferConfig.inputJson) {
                cellFormat { file ->
                    this.graphic =
                        hbox(spacing = 20) {
                            button {
                                this.alignment = Pos.CENTER
                                this.graphic = fonticon(FontAwesomeSolid.TRASH)
                                action {
                                    inferConfig.inputJson.remove(file)
                                }
                            }
                            text(file.path) {
                                alignment = Pos.CENTER_LEFT
                            }
                        }

                }
            }
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
