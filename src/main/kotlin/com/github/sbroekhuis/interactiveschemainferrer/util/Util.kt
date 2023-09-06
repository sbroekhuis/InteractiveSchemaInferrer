package com.github.sbroekhuis.interactiveschemainferrer.util

import com.fasterxml.jackson.databind.JsonNode
import com.saasquatch.jsonschemainferrer.GenericSchemaFeatureInput
import javafx.beans.property.StringProperty
import javafx.event.EventTarget
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.model.RichTextChange
import org.kordamp.ikonli.Ikon
import org.kordamp.ikonli.javafx.FontIcon
import org.reactfx.EventStream
import tornadofx.*
import java.util.*
import com.networknt.schema.SpecVersion as SpecVersionNetwork
import com.saasquatch.jsonschemainferrer.SpecVersion as SpecVersionSaas


@Suppress("SpellCheckingInspection")
fun EventTarget.fonticon(iconCode: Ikon? = null, op: FontIcon.() -> Unit = {}) = FontIcon().attachTo(this, op) {
    if (iconCode != null) it.iconCode = iconCode
}

@Suppress("SpellCheckingInspection")
fun EventTarget.codearea(text: String, op: CodeArea.() -> Unit = {}) = opcr(this, CodeArea(text), op)


fun CodeArea.richChanges(op: EventStream<RichTextChange<MutableCollection<String>, String, MutableCollection<String>>>.() -> Unit = {}): EventStream<RichTextChange<MutableCollection<String>, String, MutableCollection<String>>> =
    this.richChanges().apply(op)


/**
 * Return a Map of each distinct value to the count of that value in the original list.
 */
internal fun <T : Any> Iterable<T?>.frequencies(): Map<T, Int> {
    return this.filterNotNull().groupingBy { it }.eachCount()
}


@Suppress("DuplicatedCode")
fun EventTarget.jsonarea(
    property: StringProperty,
    validator: ValidationContext? = null,
    op: CodeArea.() -> Unit = {}
) = opcr(this, CodeArea(property.get()), op).apply {
    property.stringBinding(this.textProperty()) { it }
    textProperty().onChange {
        setStyleSpans(0, highlightJSON(it ?: ""))
    }
    setStyleSpans(0, highlightJSON(text))
    style += "-fx-padding: 10px;"
    validator?.addValidator(this, this.textProperty()) {
        if (!isValidJSON(it)) {
            error("Invalid JSON")
        } else {
            null
        }
    }
}

@Suppress("DuplicatedCode")
fun EventTarget.jsonarea(
    text: String,
    validator: ValidationContext? = null,
    op: CodeArea.() -> Unit = {}
) = opcr(this, CodeArea(text), op).apply {
    textProperty().onChange {
        setStyleSpans(0, highlightJSON(it ?: ""))
    }
    setStyleSpans(0, highlightJSON(text))
    style += "-fx-padding: 10px;"
    validator?.addValidator(this, this.textProperty()) {
        if (!isValidJSON(it)) {
            error("Invalid JSON")
        } else {
            null
        }
    }
}


fun SpecVersionNetwork.VersionFlag.asSaasquatch() = when (this) {
    SpecVersionNetwork.VersionFlag.V201909 -> SpecVersionSaas.DRAFT_2019_09
    SpecVersionNetwork.VersionFlag.V4 -> SpecVersionSaas.DRAFT_04
    SpecVersionNetwork.VersionFlag.V6 -> SpecVersionSaas.DRAFT_06
    SpecVersionNetwork.VersionFlag.V7 -> SpecVersionSaas.DRAFT_07
    SpecVersionNetwork.VersionFlag.V202012 -> SpecVersionSaas.DRAFT_2020_12
}

fun SpecVersionSaas.asNetworknt() = when (this) {
    SpecVersionSaas.DRAFT_2019_09 -> SpecVersionNetwork.VersionFlag.V201909
    SpecVersionSaas.DRAFT_04 -> SpecVersionNetwork.VersionFlag.V4
    SpecVersionSaas.DRAFT_06 -> SpecVersionNetwork.VersionFlag.V6
    SpecVersionSaas.DRAFT_07 -> SpecVersionNetwork.VersionFlag.V7
    SpecVersionSaas.DRAFT_2020_12 -> SpecVersionNetwork.VersionFlag.V202012
}

operator fun GenericSchemaFeatureInput.component1() = this.schema
operator fun GenericSchemaFeatureInput.component2(): MutableCollection<out JsonNode> = this.samples
operator fun GenericSchemaFeatureInput.component3() = this.type
operator fun GenericSchemaFeatureInput.component4() = this.specVersion
operator fun GenericSchemaFeatureInput.component5() = this.path


fun <T> T?.optional() = Optional.ofNullable(this)
