package fragment

import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement

//private val init = ResourceManager.reg { LoginWidget() }
private val init = ResourceManager.reg { AlertWidget() }

class AlertWidget : ResourceWidget() {
    private val L by logger()


    val title: HTMLElement by docu
    val body: HTMLDivElement by docu

    val mod: dynamic by lazy {
        val selector = '#' + instanceName.instanceId("exampleModal")
        val mod = jq(selector)
        mod
    }

    override fun afterShow() {
        mod.on("hide.bs.modal") {
            super.close()
        }
        mod.modal("show")
    }


    override fun close() {
        mod.modal("hide")
    }
}