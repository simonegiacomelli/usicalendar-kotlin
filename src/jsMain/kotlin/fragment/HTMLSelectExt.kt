package fragment

import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import kotlin.browser.document


fun HTMLSelectElement.optionsRemoveAll() {
    while (options.length > 0) options.remove(0)
}

fun HTMLSelectElement.optionsAdd(func: (opt: HTMLOptionElement) -> Unit) {
    val opt = document.createElement("option") as HTMLOptionElement
    func(opt)
    options.add(opt)
}

fun HTMLSelectElement.setItems(values: List<Pair<Any, String>>) {
    optionsRemoveAll()
    values.forEach {
        optionsAdd { opt ->
            opt.value = it.first.toString()
            opt.innerHTML = it.second
        }
    }
}

fun newOption( func: (HTMLOptionElement) -> Unit = {}): HTMLOptionElement {
    val opt = document.createElement("option") as HTMLOptionElement
    func(opt)
    return opt
}