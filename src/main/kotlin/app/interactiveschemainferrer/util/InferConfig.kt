package app.interactiveschemainferrer.util

import com.saasquatch.jsonschemainferrer.SpecVersion
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import tornadofx.*
import java.io.File

/**
 * Configuration class for the Inferrer.
 *
 * Here we store the config for the program, what files to use to infer the schema and the [SpecVersion]
 * the schema uses.
 */
class InferConfig(
    inputJsons: ObservableList<File> = observableListOf(),
    schemaVersion: SpecVersion = SpecVersion.DRAFT_2020_12,
    assumeArray: Boolean = true
) {
    val inputJsonsProperty = SimpleListProperty(this, "inputJsons", inputJsons)
    val schemaVersionProperty = SimpleObjectProperty(this, "schemaVersion", schemaVersion)
    val assumeArrayProperty = SimpleBooleanProperty(this, "assumeArray", assumeArray)
}

class InferConfigModel : ItemViewModel<InferConfig>() {
    var inputJson: SimpleListProperty<File> = bind(InferConfig::inputJsonsProperty)
    var schemaVersion: SimpleObjectProperty<SpecVersion> = bind(InferConfig::schemaVersionProperty)
    var assumeArray: SimpleBooleanProperty = bind(InferConfig::assumeArrayProperty)
}
