package generated

import org.w3c.dom.get
import kotlin.browser.window

//this file will be overwritten

private val start = generated_entry_point()

@JsName("generated_entry_point")
fun generated_entry_point() {
    window.asDynamic()["buildDate"] = "{BUILDDATE}"
    println("Build date: ${window["buildDate"]}")
}
