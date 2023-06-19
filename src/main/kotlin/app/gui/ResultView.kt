package app.gui

import app.InteractiveInferenceController
import app.util.codearea
import app.util.fonticon
import app.util.highlightJSON
import app.util.richChanges
import com.fasterxml.jackson.databind.JsonNode
import javafx.application.Platform
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.ButtonBar
import javafx.scene.input.Clipboard
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Priority
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid
import tornadofx.*


class ResultView : View("Results") {
    private val controller: InteractiveInferenceController by inject()

    override val root = form {
        val schema: JsonNode = controller.resultSchema

        fieldset {
            field("JSON Schema:") {
                labelPosition = Orientation.VERTICAL

                //stackplane to have a copy button on overlap of the codearea
                stackpane {
                    alignment = Pos.CENTER_LEFT
                    vgrow = Priority.ALWAYS
                    hgrow = Priority.ALWAYS
                    codearea(text = schema.toPrettyString()) {
                        richChanges {
                            setStyleSpans(0, highlightJSON(this@codearea.text))
                        }
                        fitToParentHeight()
                        isEditable = false

                        styleClass.add("text-area")
                    }

                    anchorpane {
                        button {
                            icon = fonticon(FontAwesomeSolid.COPY)
                            action {
                                Clipboard.getSystemClipboard().putString(schema.toPrettyString())
                            }
                            tooltip("Copy the schema to clipboard")
                        }.apply {
                            AnchorPane.setTopAnchor(this, 1.0)
                            AnchorPane.setRightAnchor(this, 1.0)
                        }
                        // Otherwise we cannot scroll as it blocks the codearea
                        isPickOnBounds = false
                    }
                    fitToParentHeight()
                }
                fitToParentHeight()
            }
        }

        buttonbar {
            button("Close Program", ButtonBar.ButtonData.OK_DONE) {
                id = "close"
                action {
                    Platform.exit()
                }
            }
        }

    }

}
