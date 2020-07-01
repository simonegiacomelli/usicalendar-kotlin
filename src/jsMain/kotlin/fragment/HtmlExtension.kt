package fragment

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.w3c.dom.*
import org.w3c.dom.events.Event
import kotlin.browser.document

fun HTMLElement.onclickExt(value: (Event) -> Unit) {
    this.onclick = {
        it.preventDefault()
        it.stopPropagation()
        value(it)
        0
    }
}

fun HTMLElement.onclickAsync(value: suspend (Event) -> Unit) {
    this.onclick = {
        it.preventDefault()
        it.stopPropagation()
        GlobalScope.async {
            value(it)
        }
    }
}


val HTMLElement.firstElement: HTMLElement get() = this.children.first

val HTMLCollection.first: HTMLElement get() = this.asList().first() as HTMLElement

fun HTMLCollection.firstRecursive(tagName: String): HTMLElement? {
    return asList().firstRecursive(tagName)
}

fun Element.firstRecursive(tagName: String): HTMLElement? = listOf(this).firstRecursive(tagName)

fun Collection<Element>.firstRecursive(tagName: String): HTMLElement? {
    return firstRecursive({ it.nodeName.equals(tagName, true) }, { it.children.asList() }) as HTMLElement?
}

fun Collection<Node>.firstRecursive(tagName: String): HTMLElement? {
    return firstRecursive({ it.nodeName.equals(tagName, true) }, { it.childNodes.asList() }) as HTMLElement?
}

fun <T> Collection<T>.firstRecursive(cond: (T) -> Boolean, children: (T) -> Collection<T>): T? {

    forEach {
        if (cond(it))
            return it
    }

    forEach {
        children(it).firstRecursive(cond, children).apply {
            if (this != null) return this
        }
    }

    return null
}

fun <T> Collection<T>.toListRecursive(
    accept: (T) -> Boolean,
    inspect: (T) -> Boolean,
    children: (T) -> Collection<T>
): List<T> {
    val res = mutableListOf<T>()

    fun recurse(collection: Collection<T>) {
        collection.forEach { if (accept(it)) res.add(it) }
        collection.forEach { if (inspect(it)) recurse(children(it)) }
    }

    recurse(this)
    return res
}

fun HTMLTemplateElement.clone() = document.importNode(this.content, true).childNodes

var HTMLElement.visible: Boolean
    get() = style.display != "none"
    set(value) {
        style.display = if (value) "" else "none"
    }