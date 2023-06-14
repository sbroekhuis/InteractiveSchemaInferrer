package app.strategy

import app.gui.InferringView
import javafx.scene.Parent
import tornadofx.*
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

abstract class Strategy {

    /**
     * Ask the user with a form. When a response is finished, call [StrategyFragment.done] inside the [form] function.
     *
     * @see ConstDetection.getFeatureResult
     */
    fun askUserWith(title: String?, question: StrategyFragment.() -> Form) {
        val strategyFragment = object: StrategyFragment() {
            init{
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


}


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
