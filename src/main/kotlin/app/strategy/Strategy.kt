package app.strategy

import app.gui.InferringView
import tornadofx.*
import java.util.concurrent.CompletableFuture

interface Strategy {
    fun <T : StrategyDialogue> Strategy.askUserWith(create: (CompletableFuture<T>) -> T): T {
        val completableFuture = CompletableFuture<T>()
        runLater {
            val find = find(InferringView::class)
            find.replaceWith(create(completableFuture))
        }
        return completableFuture.get()
    }
}


abstract class StrategyDialogue(protected val cF: CompletableFuture<out StrategyDialogue>) : Fragment()
