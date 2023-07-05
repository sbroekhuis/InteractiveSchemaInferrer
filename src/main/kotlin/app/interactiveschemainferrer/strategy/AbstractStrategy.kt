package app.interactiveschemainferrer.strategy

import app.interactiveschemainferrer.gui.InferringView
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
    val logger: Logger = Logger.getLogger(this::class.qualifiedName)


    fun <R, T : StrategyFragment<R>> askUserWith(question: T): R? {
        runLater {
            find<InferringView>().replaceWith(question)
        }
        return question.waitForResponse()
    }
}


/**
 * StrategyFragment is a [Fragment] that replaces the [InferringView] with a question for the user.
 * When done, we return ourselves
 */
abstract class StrategyFragment<FormData>(title: String) : Fragment(title) {
    private val cF = CompletableFuture<FormData?>()
    protected val validator = ValidationContext()

    fun done(data: FormData? = null) {
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
    fun waitForResponse(): FormData? {
        return cF.get()
    }
}