package starter2

import api.*
import fragment.ResourceWidget
import nswf.NswfClient
import nswf.appendParams
import org.w3c.dom.HTMLElement
import widgets.app.SettingsWidget


object Api {


    var instance = ""

    fun apiDbLogAddOnClickInstrument(widget: ResourceWidget, element: HTMLElement, info2: String = "") {
        val info = "${widget::class.simpleName!!}.${element.getAttribute("w-id")}.click"
        element.addEventListener("click", {
            apiLog(kind_ux, info, info2)
        }, true)
    }

    fun apiLog(kind: String, info: String, info2: String = "") {
        apiDbLog.new {
            it.kind = kind
            it.info = info
            it.instance = instance
            it.info2 = info2
        }.call { }
    }


    val cli = NswfClient {
        //        val host = if (SettingsWidget.cbUrlStor) SettingsWidget.tbUrlStor else ""
        val u2 = StringBuilder("/${Configuration.apiPrefixUrl}/$it?")
//
        val value = if (SettingsWidget.cbTokenStor) SettingsWidget.tbTokenStor else ""
        u2.appendParams("token", value)
//
        u2.toString()
    }

    val apiLogin by lazy { cli.api(::Login) }
    val apiQueryCalendars by lazy { cli.api(::QueryCalendars) }
    val apiCreateToken by lazy { cli.api(::CreateToken) }
    val apiCourseStatus by lazy { cli.api(::CourseStatus) }
    val apiDbLog by lazy { cli.api(::DbLog) }
    val apiAppLogInspect by lazy { cli.api(::AppLogInspect) }
    val apiListaFatture by lazy { cli.api(::ListaFatture) }
    val apiPdfFattura by lazy { cli.api(::PdfFattura) }
    val apiRackEdit by lazy { cli.api(::RackEdit) }
    val apiRackAdd by lazy { cli.api(::RackAdd) }
    val apiRackList by lazy { cli.api(::RackList) }
    val apiSqlQuery by lazy { cli.api(::SqlQuery) }
    val apiDocsUtente by lazy { cli.api(::DocsUtente) }
    val apiDocsUtenteDownload by lazy { cli.api(::DocsUtenteDownload) }

}


