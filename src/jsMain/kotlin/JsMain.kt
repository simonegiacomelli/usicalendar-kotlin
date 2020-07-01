import fragment.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import nswf.Appenders
import nswf.Logger
import starter2.Api
import widgets.app.AboutWidget
import widgets.app.HowtoWidget
import widgets.app.SettingsWidget
import widgets.app.SetupOwnCalendarWidget
import widgets.components.PagedTableWidget
import widgets.components.TableWidget
import kotlin.browser.document
import kotlin.browser.window
import kotlin.js.Promise
import kotlin.random.Random
import kotlin.random.nextUInt

private val init = Appenders.add { LogWidget.log(it) }
fun main() {
    JsMain.init("", "by main")
}

object JsMain {
    var done = false

    fun init(baseUrl: String, descr: String) {
        Api.instance = "i${Random.nextUInt(10000u, 99999u)}"
        val L = Logger("main ")
        L.info("Build date: ${JsProjectProperties.buildDate} $baseUrl $descr")
        if (done) return
        done = true


        ResourceManager.baseurl = ""

        GlobalScope.launch {
            app.args //this line force to instantiante arguments
            window.history.replaceState(Any(), document.title, ".")
            if (app.args.token != "") {
                SettingsWidget.cbTokenStor = true
                SettingsWidget.tbTokenStor = app.args.token
                L.info("writing token by url argument: ${app.args.token} localstorage: ${SettingsWidget.tbTokenStor}")
                Api.apiLog("application-startup", "with token param")
            } else
                Api.apiLog("application-startup", "without token param")
            Promise.all(arrayOf(
                ResourceManager.loadResource { SetupOwnCalendarWidget() }
                , ResourceManager.loadResource { HeaderWidget() }
                , ResourceManager.loadResource { PagedTableWidget() }
            )).await()
            ResourceManager.loadResources()
            val rootDiv = document.getElementById("rootDiv")!!
            //rootDiv.appendChild(rootWidget.elementInstance)
            rootDiv.appendChild(rootWidget.elementInstance)
            rootDiv.appendChild(rootModalWidget.elementInstance)
            val header = document.getElementById("header")!!
            header.innerHTML = ""
            header.appendChild(app.headerWidget.elementInstance)
            globalFun.handleBack()

            HotkeyWindow.add("ESC") { globalFun.userClose() }
            HotkeyWindow.add("F1") { globalFun.settings() }
            HotkeyWindow.add("F2") { globalFun.showLog() }
            HotkeyWindow.add("F3") { globalFun.howto() }
            HotkeyWindow.add("F4") { globalFun.about() }
            HotkeyWindow.add("CTRL-SHIFT-F8") { globalFun.logWidget() }

            //app.serverListWidget.show()
            //app.switchListWidget.show()

            app.create(SetupOwnCalendarWidget()).show()

            Unit

        }
    }
}

val rootWidget by lazy { WidgetManager() }
val rootModalWidget by lazy { WidgetManagerSingle() }

object app {
    val headerWidget by lazy { HeaderWidget() }
    val settingsWidget by lazy { create(SettingsWidget()) }
    val tableWidget by lazy { create(TableWidget()) }
    val logWidget by lazy { create(LogWidget()) }
    val calendarCodesWidget by lazy { create(HowtoWidget()) }
    val aboutWidget by lazy { create(AboutWidget()) }
    val open_location = window.location.hash.removePrefix("#")
    val args by lazy { AppArgs(open_location) }

    fun <T : ResourceWidget> create(inst: T): T {
        rootWidget.manage(inst)
        return inst
    }

    fun alert(caption: String, msg: String): AlertWidget {
        val w = AlertWidget().also {
            it.title.innerText = caption
            it.body.innerText = msg
        }
        rootModalWidget.manage(w).show()
        return w
    }

    fun confirm(caption: String, msg: String): ModalWidget {
        val w = ModalWidget().also {
            it.title.innerText = caption
            it.body.innerText = msg
        }
        rootModalWidget.manage(w)
        return w
    }

}

object globalFun {

    val userClose = {
        if ((rootModalWidget.widgets.size + rootWidget.widgets.size) > 1)
            window.history.back()
    }
    val settings = { app.settingsWidget.show() }
    val showLog = { app.logWidget.show() }
    val logWidget = {
        LogWidget.log("form instance: ${currentHolder().widgets.lastOrNull()?.instanceName}")
        app.logWidget.show()
    }

    val howto = { app.calendarCodesWidget.show() }
    val about = { app.aboutWidget.show() }
    val table = { app.tableWidget.showAsync() }

    fun handleBack() {
        window.onpopstate = { currentHolder().closeCurrent() }
        val push = { window.history.pushState("", "", "") }
        rootWidget.notifyNewPushState = push
        rootModalWidget.notifyNewPushState = push
    }

    private fun currentHolder() = if (rootModalWidget.widgets.size > 0) rootModalWidget else rootWidget


}
