package app.interactiveschemainferrer.strategy

import app.interactiveschemainferrer.gui.InferringView
import javafx.scene.Parent
import tornadofx.*
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

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
 * @see ConstDetection.getFeatureResult
 */
fun askUserWith(title: String?, question: StrategyFragment.() -> Form) {
    val strategyFragment = object : StrategyFragment() {
        init {
            if (title != null) {
                this.title = title
            }
        }

        override val root: Parent = this.question()
    }
    runLater {
        find<InferringView>().replaceWith(strategyFragment)
    }
    strategyFragment.waitForResponse()
}


/**
 * StrategyFragment is a [Fragment] that replaces the [InferringView] with a question for the user.
 * When done, we return ourselves
 */
abstract class StrategyFragment : Fragment() {
    private val cF = CompletableFuture<StrategyFragment>()

    fun done() {
        replaceWith<InferringView>()
        cF.complete(this@StrategyFragment)
    }

    /**
     * Waits if necessary for this fragment to complete.
     *
     * @throws CancellationException if this future was cancelled
     * @throws ExecutionException if this future completed exceptionally
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     */
    fun waitForResponse() {
        cF.get()
    }
}
