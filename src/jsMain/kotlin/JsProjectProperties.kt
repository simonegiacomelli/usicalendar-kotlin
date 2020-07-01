import org.w3c.dom.get
import kotlin.browser.window

object JsProjectProperties {
    val buildDate: String get() = "${window["buildDate"]}"
}