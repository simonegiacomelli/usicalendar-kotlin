package fragment

import globalFun
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLLIElement
import kotlin.browser.document

private val init = ResourceManager.reg { HeaderWidget() }

class HeaderWidget : ResourceWidget() {
    val title_add: HTMLElement by docu
    var save_counter = 0
    fun saving_increment() {
        save_counter++
        update_title()

    }

    private fun update_title() {
        title_add.innerText = if (save_counter > 0) "saving..." else ""
    }

    fun saving_decrement() {
        save_counter--
        update_title()
    }

    inner class MenuEntry(val title: String, visible: Boolean = true, val function: () -> Unit) {
        var visible: Boolean
            get() = li.visible
            set(visible) {
                li.visible = visible
            }

        val li = document.createElement("li") as HTMLLIElement
        val a = document.createElement("a") as HTMLAnchorElement

        init {
            a.setAttribute("data-toggle", "collapse")
            a.setAttribute("data-target", ".navbar-collapse.show") //close after click
            a.href = "#"
            a.className = "nav-link"
            a.innerHTML = title
            a.onclick = { it.preventDefault(); function() }
            li.appendChild(a)
            menu.appendChild(li)
            this.visible = visible
        }
    }

    val allEntries = mutableMapOf<String, MenuEntry>()

    val menu: HTMLElement by docu
    var chiudi by allEntries
    var conf by allEntries
    var log by allEntries
    var howto by allEntries
    var about by allEntries
    var applog by allEntries


    init {
        afterRender {
            this.chiudi = MenuEntry("Chiudi", visible = app.args.isAdmin) { globalFun.userClose() }
            this.conf = MenuEntry("Conf", visible = app.args.isAdmin) { globalFun.settings() }
            this.log = MenuEntry("Log", visible = app.args.isAdmin) { globalFun.logWidget() }
            this.howto = MenuEntry("How to...", visible = false) { globalFun.howto() }
            this.about = MenuEntry("About") { globalFun.about() }
            this.applog = MenuEntry("AppLog", visible = app.args.isAdmin) { globalFun.table() }
        }
    }


}
