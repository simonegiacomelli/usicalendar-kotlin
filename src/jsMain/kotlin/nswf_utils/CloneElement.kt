package nswf_utils

import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTemplateElement
import kotlin.browser.document

fun <T:HTMLElement> T.cloneElement(): T {
    val newTemplate = document.createElement("template") as HTMLTemplateElement
    newTemplate.innerHTML = this.innerHTML.trim()
    return newTemplate.content.firstChild!! as T
}