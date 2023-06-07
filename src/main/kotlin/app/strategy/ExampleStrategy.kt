package app.strategy

import app.gui.InferringView
import com.fasterxml.jackson.databind.node.ObjectNode
import com.saasquatch.jsonschemainferrer.GenericSchemaFeature
import com.saasquatch.jsonschemainferrer.GenericSchemaFeatureInput
import javafx.scene.control.ButtonBar
import javafx.scene.control.TextField
import tornadofx.*
import java.util.concurrent.CompletableFuture

class ExampleStrategy : GenericSchemaFeature, Strategy {

    override fun getFeatureResult(context: GenericSchemaFeatureInput): ObjectNode? {
        val path = context.path
        val get = askUserWith { ExampleDialogFragment(path, it) }
        println(get.inputField.text)

        return null
    }


    class ExampleDialogFragment(val path: String, cF: CompletableFuture<ExampleDialogFragment>) : StrategyDialogue(cF) {
        val inputField = TextField()

        override val root = form {
            fieldset {
                field("JSON Path:") {
                    label(path)
                }
                field("Type Something?") {
                    add(inputField)
                }
            }

            buttonbar {
                button("Done", ButtonBar.ButtonData.OK_DONE) {
                    action {
                        replaceWith<InferringView>()
                        cF.complete(this@ExampleDialogFragment)
                    }
                }
            }
        }


    }
}
