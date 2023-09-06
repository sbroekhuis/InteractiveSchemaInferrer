package app.interactiveschemainferrer.strategy

import app.interactiveschemainferrer.gui.InferringView
import com.fasterxml.jackson.databind.JsonNode
import javafx.event.EventTarget
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.logging.Logger

/**
 * # Strategy
 * A strategy is a technique or rule that detects a possible change in a JSON schema that requires a user input.
 *
 * We require a user input because we cannot always be certain the change in the schema is valid.
 * It could be a coincidence or the user thinks in the future it will change.
 *
 * Inferring Traversal Type
 *
 * - [com.saasquatch.jsonschemainferrer.EnumExtractor] - [Pre-order](https://en.wikipedia.org/wiki/Tree_traversal#Pre-order,_NLR)
 *
 * - [com.saasquatch.jsonschemainferrer.GenericSchemaFeature] - [Post-order](https://en.wikipedia.org/wiki/Tree_traversal#Post-order,_LRN)
 *
 *
 * Ask the user with a form. When a response is finished, call [StrategyFragment.done] inside the [form] function.
 *
 * @see ConstStrategy.getFeatureResult
 */
abstract class AbstractStrategy {
    val logger: Logger = Logger.getLogger(this::class.simpleName)


    fun <R, T : StrategyFragment<R>> askUserWith(question: T): R {
        runLater {
            find<InferringView>().replaceWith(question)
        }
        return question.waitForResponse()
    }


    /**
     * After finishing running the inferrer, call the postProcess for all strategies.
     */
    open fun postProcess(schema: JsonNode) {
        return
    }
}


/**
 * StrategyFragment is a [Fragment] that replaces the [InferringView] with a question for the user.
 * When done, we return ourselves
 */
abstract class StrategyFragment<FormData>(title: String) : Fragment(title) {
    private val cF = CompletableFuture<FormData>()
    protected val validator = ValidationContext()

    fun done(data: FormData) {
        replaceWith<InferringView>()
        cF.complete(data)
    }

    /**
     * Waits if necessary for this fragment to complete.
     *
     * @throws CancellationException if this future was cancelled
     * @throws ExecutionException if this future completed exceptionally
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     */
    fun waitForResponse(): FormData {
        return cF.get()
    }

    fun EventTarget.strategyroot(helpUrl: String? = null, op: VBox.() -> Unit): VBox {
        return opcr(this, VBox()).apply {
            if (!helpUrl.isNullOrEmpty()) {
                menubar {
                    menu("Help") {
                        item(
                            "Documentation", graphic = FontIcon(FontAwesomeSolid.EXTERNAL_LINK_ALT)
                        ).action {
                            hostServices.showDocument(helpUrl)
                        }
                    }
                }
            }
            vbox(spacing = 20) {
                vgrow = Priority.ALWAYS
                paddingAll = 20.0
            }.apply(op)
        }
    }
}
