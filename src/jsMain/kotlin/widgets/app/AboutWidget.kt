package widgets.app


import api.info_about
import api.kind_ux
import fragment.ResourceManager
import fragment.ResourceWidget
import fragment.afterShow
import org.w3c.dom.HTMLElement
import starter2.Api

private val init = ResourceManager.reg { AboutWidget() }

class AboutWidget() : ResourceWidget() {

    val a_mailto: HTMLElement by docu

    override fun afterRender() {
        Api.apiDbLogAddOnClickInstrument(this, a_mailto)
    }
    init {

        afterShow {
            Api.apiDbLog.new {
                it.kind = kind_ux
                it.info = info_about
                it.info2 = ""
                it.instance = ""
            }.call { }
        }
    }
}
