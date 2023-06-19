package app.strategy

import app.util.fonticon
import app.util.highlightJSON
import app.util.isValidJSON
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.io.JsonEOFException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.saasquatch.jsonschemainferrer.EnumExtractor
import com.saasquatch.jsonschemainferrer.EnumExtractorInput
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.ButtonBar
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Priority
import org.fxmisc.richtext.CodeArea
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid
import tornadofx.*
import java.util.*
import javax.json.JsonException

/**
 * # Strategy: EnumDetection
 * This strategy implements the [Enum](https://json-schema.org/understanding-json-schema/reference/generic.html?highlight=enum#enumerated-values)
 *
 * This is done by detecting from all the input samples if
 * there is only a small number of distinct values.
 * We divide this difference and compare it to a the [EnumDetection.THRESHOLD].
 *
 */
class EnumDetection : Strategy(), EnumExtractor {

    companion object {
        /** Threshold to specify when the difference between distinct and total is substantial */
        var THRESHOLD: Float = 0.1f
    }

    /**
     * @return The *group* of enums. Note that each group is expected to be not null and not
     * empty. All the elements in each group are expected to come directly from the given
     * samples if possible to ensure [JsonNode.equals] works correctly.
     */
    override fun extractEnums(input: EnumExtractorInput): MutableCollection<MutableCollection<out JsonNode>> {
        // TODO: If some field happens again, perhaps it is the same enum.
        //  How can we implement this? Definitions?
        val samples: MutableCollection<out JsonNode> = input.samples
        val distinct = samples.distinct()
        val distinctSize = distinct.size
        val totalSize = samples.size.toFloat()

        if (distinctSize == 1) {
            // If distinctSize is 1, then this is not an enum but a const
            // See ConstDetection
            return Collections.emptySet()
        }

        val fl = distinctSize.div(totalSize)
        if (fl > THRESHOLD) return Collections.emptySet()



        fun newCodeArea(text: String = ""): CodeArea {
            return CodeArea(text).apply {
                isEditable = true
                textProperty().onChange { n ->
                    setStyleSpans(0, highlightJSON(n ?: ""))
                }

                minHeight = 50.0
                this.style += "-fx-padding: 10px;"
                //Initial Styling
                setStyleSpans(0, highlightJSON(text ?: ""))
            }
        }

        val answerList = observableListOf(distinct.map {
            newCodeArea(it.toPrettyString())
        })

        askUserWith("Inferring - Possible Enum Found") {
            form {
                val validator = ValidationContext()

                fieldset {
                    field(
                        """
                    |The field with path: ${input.path} seems to have a relative small amount distinct values.
                    |Is this field an enum?
                    """.trimMargin(),
                    ) {
                        labelPosition = Orientation.VERTICAL
                    }
                    label("Current Values:")
                    listview(answerList) {
                        styleClass.clear()
                        onScroll = null
                        placeholder = label("No Values")
                        cellFormat {
                            graphic = hbox {
                                alignment = Pos.CENTER_LEFT
                                vgrow = Priority.ALWAYS
                                hgrow = Priority.ALWAYS
                                opcr(this, it).apply {
                                    fitToParentWidth()
                                    validator.addValidator(this, this.textProperty()) {
                                        if (!isValidJSON(it)) {
                                            error("Invalid JSON")
                                        } else {
                                            null
                                        }
                                    }
                                }

                                anchorpane {
                                    button {
                                        icon = fonticon(FontAwesomeSolid.TRASH)
                                        action {
                                            answerList.remove(it)
                                        }
                                        style += "-fx-border-radius: 0 3px 3px 0;"
                                    }.apply {
                                        AnchorPane.setTopAnchor(this, 1.0)
                                        AnchorPane.setBottomAnchor(this, 1.0)
                                        AnchorPane.setRightAnchor(this, 1.0)
                                    }
                                    // Otherwise we cannot scroll as it blocks the codearea
                                    isPickOnBounds = false
                                }
                                fitToParentHeight()
                                fitToParentWidth()
                                styleClass.clear()
                            }
                            styleClass.clear()
                        }
                    }
                    button("Add") {
                        action {
                            answerList.add(newCodeArea())
                        }
                    }
                }
                buttonbar {
                    button("Yes", ButtonBar.ButtonData.YES) {
                        enableWhen(validator.valid)
                        action {
                            done()
                        }
                    }
                    button("No", ButtonBar.ButtonData.NO) {
                        action {
                            answerList.clear()
                            done()
                        }
                    }
                }
            }
        }
        if (answerList.isEmpty()) return Collections.emptySet()

        val answers = answerList.map {
            ObjectMapper().readTree(it.text)
        }.distinct().toMutableList()

        return Collections.singleton(answers)
    }
}

class EnumDetectionModel : ItemViewModel<EnumDetection>() {}

