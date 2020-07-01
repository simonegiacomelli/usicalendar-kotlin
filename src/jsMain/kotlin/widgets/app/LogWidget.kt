package fragment

import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTemplateElement
import org.w3c.dom.HTMLUListElement
import nswf_utils.cloneElement

private val init = ResourceManager.reg { LogWidget() }

class LogWidget : ResourceWidget() {


    val ul: HTMLUListElement by docu
    val template: HTMLTemplateElement by docu
    val counter: HTMLElement by docu
    val header: HTMLElement by docu

    companion object {
        val accumulator = mutableListOf<String>()

        fun log(payloadString: String) {
            accumulator.add(payloadString)
        }
    }


    override fun beforeShow() {
        header.onclick = {
            ul.innerHTML = ""
            close()
        }
        for (log in accumulator) uilog(log)
        accumulator.clear()
    }

    private fun uilog(payloadString: String) {
        counter.innerHTML = (counter.innerHTML.toInt() + 1).toString()

        val node = template.cloneElement()
        node.innerHTML = "$payloadString"
        ul.append(node)
    }

}