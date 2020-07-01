package fragment

import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement

private val init = ResourceManager.reg { ModalWidget() }

class ModalWidget : ResourceWidget() {

    val title: HTMLElement by docu
    val body: HTMLDivElement by docu
    val btnSave: HTMLButtonElement by docu
    val btnClose: HTMLButtonElement by docu

    var onSave = {}
    var onDismiss = {}

    val mod: dynamic by lazy {
        val selector = '#' + instanceName.instanceId("exampleModal")
        val mod = jq(selector)
        mod
    }

    override fun afterShow() {
        var saving = false

        btnSave.onclickExt {
            saving = true;
            close()
            onSave()
        }
        mod.on("hide.bs.modal") {
            if (!saving)
                onDismiss()
            super.close()
        }
        mod.modal("show")
    }

    override fun close() {
        mod.modal("hide")
    }

}