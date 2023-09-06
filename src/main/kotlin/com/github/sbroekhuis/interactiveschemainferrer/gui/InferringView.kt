package com.github.sbroekhuis.interactiveschemainferrer.gui

import com.github.sbroekhuis.interactiveschemainferrer.util.fonticon
import javafx.animation.Timeline
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.util.Duration
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid
import tornadofx.*

class InferringView : View("Inferring...") {

    override var root = vbox {
        val icon = fonticon(FontAwesomeSolid.SPINNER) {
            alignment = Pos.CENTER
            vboxConstraints {
                margin = Insets(20.0)
            }
            iconSize = 100
        }
        timeline {
            keyframe(duration = Duration.millis(200.0)) {
                setOnFinished {
                    icon.rotateProperty().value += 45
                }
            }
            cycleCount = Timeline.INDEFINITE
        }.play()
    }
}
